"""Embed ICO resources into the bundled unsigned x64 launcher; preserve its code/imports."""
from pathlib import Path
import struct


def _layout(data):
    pe=struct.unpack_from('<I',data,0x3c)[0]
    if data[:2]!=b'MZ' or data[pe:pe+4]!=b'PE\0\0':raise ValueError('Not a PE image')
    if struct.unpack_from('<H',data,pe+24)[0]!=0x20b:raise ValueError('Expected PE32+')
    count=struct.unpack_from('<H',data,pe+6)[0]
    optional=pe+24;table=optional+struct.unpack_from('<H',data,pe+20)[0]
    sections=[]
    for index in range(count):
        offset=table+index*40
        size,va,raw_size,raw=struct.unpack_from('<IIII',data,offset+8)
        sections.append((va,size,raw,raw_size))
    return pe,optional,table,sections


def read_resources(data):
    _,optional,_,sections=_layout(data)
    def physical(rva):
        for va,size,raw,raw_size in sections:
            if va<=rva<va+max(size,raw_size):return raw+rva-va
        raise ValueError('Resource address outside PE sections')
    rva=struct.unpack_from('<I',data,optional+112+2*8)[0]
    if not rva:return {}
    base=physical(rva)
    def directory(relative,depth=0):
        if depth>8:raise ValueError('Resource nesting too deep')
        offset=base+relative;named,ids=struct.unpack_from('<HH',data,offset+12)
        result={}
        for index in range(named+ids):
            name,value=struct.unpack_from('<II',data,offset+16+index*8)
            if name&0x80000000:
                position=base+(name&0x7fffffff);length=struct.unpack_from('<H',data,position)[0]
                key=bytes(data[position+2:position+2+length*2]).decode('utf-16le')
            else:key=name
            if value&0x80000000:result[key]=directory(value&0x7fffffff,depth+1)
            else:
                address,size,codepage,_=struct.unpack_from('<IIII',data,base+value)
                position=physical(address);result[key]=(bytes(data[position:position+size]),codepage)
        return result
    return directory(0)


def _encode(tree,base_rva):
    output=bytearray();directories={}
    def allocate(size,alignment=4):
        output.extend(b'\0'*((-len(output))%alignment));offset=len(output);output.extend(b'\0'*size);return offset
    def keys(node):return sorted(node,key=lambda key:(0,str(key)) if isinstance(key,str) else (1,key))
    def reserve(node):
        directories[id(node)]=allocate(16+8*len(node))
        for key in keys(node):
            if isinstance(node[key],dict):reserve(node[key])
    reserve(tree)
    def write(node):
        offset=directories[id(node)];ordered=keys(node)
        struct.pack_into('<HH',output,offset+12,sum(isinstance(k,str) for k in ordered),sum(isinstance(k,int) for k in ordered))
        for index,key in enumerate(ordered):
            if isinstance(key,str):
                encoded=key.encode('utf-16le');position=allocate(2+len(encoded),2)
                struct.pack_into('<H',output,position,len(encoded)//2);output[position+2:position+2+len(encoded)]=encoded
                name=position|0x80000000
            else:name=key
            value=node[key]
            if isinstance(value,dict):entry=directories[id(value)]|0x80000000;write(value)
            else:
                blob,codepage=value;entry=allocate(16);position=allocate(len(blob));output[position:position+len(blob)]=blob
                struct.pack_into('<IIII',output,entry,base_rva+position,len(blob),codepage,0)
            struct.pack_into('<II',output,offset+16+index*8,name,entry)
    write(tree)
    return bytes(output)


def embed_icon(executable,icon,metadata=None):
    executable=Path(executable);original=executable.read_bytes();data=bytearray(original)
    pe,optional,table,sections=_layout(data);tree=read_resources(data)
    previous={k:v for k,v in tree.items() if k not in (3,14,16)}
    ico=Path(icon).read_bytes();reserved,kind,count=struct.unpack_from('<HHH',ico)
    if reserved!=0 or kind!=1 or not 1<=count<=32:raise ValueError('Invalid ICO file')
    icons={};group=bytearray(struct.pack('<HHH',0,1,count))
    for index in range(count):
        offset=6+16*index;size,position=struct.unpack_from('<II',ico,offset+8)
        blob=ico[position:position+size]
        if len(blob)!=size:raise ValueError('Truncated ICO image')
        icons[index+1]={1033:(blob,0)}
        group.extend(ico[offset:offset+12]);group.extend(struct.pack('<H',index+1))
    tree[3]=icons;tree[14]={101:{1033:(bytes(group),0)}}
    if metadata is not None:tree[16]={1:{1033:(version_info(metadata),0)}}
    section_align,file_align=struct.unpack_from('<II',data,optional+32)
    align=lambda value,step:(value+step-1)//step*step
    va=align(max(va+max(size,raw_size) for va,size,raw,raw_size in sections),section_align)
    raw=align(len(data),file_align);blob=_encode(tree,va);raw_size=align(len(blob),file_align)
    new_header=table+40*len(sections)
    if new_header+40>min(raw for _,_,raw,_ in sections if raw):raise ValueError('No room for an icon resource section')
    data.extend(b'\0'*(raw+raw_size-len(data)));data[raw:raw+len(blob)]=blob
    struct.pack_into('<8sIIIIIIHHI',data,new_header,b'.brand\0\0',len(blob),va,raw_size,raw,0,0,0,0,0x40000040)
    struct.pack_into('<H',data,pe+6,len(sections)+1)
    struct.pack_into('<I',data,optional+56,align(va+len(blob),section_align))
    struct.pack_into('<I',data,optional+8,struct.unpack_from('<I',data,optional+8)[0]+raw_size)
    struct.pack_into('<I',data,optional+64,0) # Replacing resources makes any previous checksum stale.
    struct.pack_into('<II',data,optional+112+2*8,va,len(blob))
    struct.pack_into('<II',data,optional+112+4*8,0,0) # Never retain an invalid certificate after a resource change.
    rebuilt=read_resources(data)
    if {k:v for k,v in rebuilt.items() if k not in (3,14,16)}!=previous:raise ValueError('Non-icon resources changed')
    if rebuilt[3]!=icons:raise ValueError('Embedded icon verification failed')
    for _,_,position,size in sections:
        if data[position:position+size]!=original[position:position+size]:raise ValueError('Original executable sections changed')
    executable.write_bytes(data)


def version_info(metadata):
    def block(key,value=b'',children=(),kind=0,length=None):
        data=bytearray(b'\0'*6+key.encode('utf-16le')+b'\0\0')
        data.extend(b'\0'*((-len(data))%4));data.extend(value)
        for child in children:data.extend(b'\0'*((-len(data))%4));data.extend(child)
        size=len(data);struct.pack_into('<HHH',data,0,size,len(value) if length is None else length,kind)
        return bytes(data)
    v=[int(n) for n in metadata['version'].split('.')]+[0];ms=(v[0]<<16)|v[1];ls=(v[2]<<16)|v[3]
    fixed=struct.pack('<13I',0xFEEF04BD,0x10000,ms,ls,ms,ls,0x3f,0,0x40004,1,0,0,0)
    fields={'CompanyName':metadata['vendor'],'ProductName':metadata['product.name'],'FileDescription':metadata['description'],'FileVersion':metadata['version'],'ProductVersion':metadata['version'],'LegalCopyright':metadata['copyright'],'OriginalFilename':'InventorySystem.exe','InternalName':metadata['app.name']}
    strings=[block(k,(v+'\0').encode('utf-16le'),kind=1,length=len((v+'\0').encode('utf-16le'))//2) for k,v in fields.items()]
    info=block('StringFileInfo',children=[block('040904B0',children=strings,kind=1,length=0)],kind=1,length=0)
    var=block('VarFileInfo',children=[block('Translation',struct.pack('<HH',1033,1200))],kind=1,length=0)
    return block('VS_VERSION_INFO',fixed,[info,var])
