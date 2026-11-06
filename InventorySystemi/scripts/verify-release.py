"""Read-only structural verification of final release files. Windows execution is not claimed."""
from pathlib import Path
import zipfile
import io
import json
import plistlib
import struct
from metadata import metadata,ROOT
from windows_icon import read_resources
project=ROOT/'InventorySystemi';dist=project/'dist';meta=metadata()
app=dist/'macos/InventorySystem.app'
with (app/'Contents/Info.plist').open('rb') as f:info=plistlib.load(f)
for key,value in {'CFBundleName':meta['product.name'],'CFBundleIdentifier':meta['bundle.identifier'],'CFBundleShortVersionString':meta['version'],'NSHumanReadableCopyright':meta['copyright'],'Developer':meta['vendor']}.items():assert info[key]==value,(key,info.get(key))
assert (dist/'macos/InventorySystem.dmg').stat().st_size>1_000_000
macjar=next((app/'Contents/app').glob('inventory-system-*.jar'))
archive=dist/'windows/InventorySystem-Windows-x64.zip'
with zipfile.ZipFile(archive) as win:
    assert win.testzip() is None
    assert not any(n.endswith(('.mv.db','.trace.db','/.env')) for n in win.namelist())
    prefix='InventorySystem/'
    exe=win.read(prefix+'InventorySystem.exe');offset=struct.unpack_from('<I',exe,0x3c)[0];assert struct.unpack_from('<H',exe,offset+4)[0]==0x8664
    version=read_resources(exe)[16][1][1033][0]
    for expected in (meta['vendor'],meta['product.name'],meta['version'],meta['copyright']):assert (expected+'\0').encode('utf-16le') in version
    # Validate the native VS_FIXEDFILEINFO as well as displayed string values.
    key_end=6+len('VS_VERSION_INFO'.encode('utf-16le'))+2;value_start=(key_end+3)//4*4
    fixed=struct.unpack_from('<13I',version,value_start);assert fixed[0]==0xFEEF04BD;assert fixed[2]==(1<<16|3) and fixed[3]==0
    config=win.read(prefix+'app/InventorySystem.cfg').decode()
    for line in config.splitlines():
        if line.startswith('app.classpath='):
            name=line.split('=',1)[1].replace('$APPDIR/',prefix+'app/')
            # Normalise /../ without touching disk.
            import posixpath
            assert posixpath.normpath(name) in win.namelist(),name
    for lib in ('jbcrypt-','slf4j-api-','logback-classic-','logback-core-'):
        assert any('/lib/'+lib in n for n in win.namelist())
    assert any('javafx-graphics-' in n and '-win.jar' in n for n in win.namelist())
    assert not any('-mac-aarch64.jar' in n for n in win.namelist())
    for binary in ('runtime/bin/java.exe','runtime/bin/javaw.exe','runtime/bin/server/jvm.dll'):assert win.read(prefix+binary)[:2]==b'MZ'
    for name in ('LICENSE.txt','TERMS.md','PRIVACY.md','THIRD-PARTY-NOTICES.txt'):assert win.read(prefix+'Legal/'+name)==(ROOT/name).read_bytes()
    jarname=next(n for n in win.namelist() if '/app/inventory-system-' in n and n.endswith('.jar'))
    with zipfile.ZipFile(macjar) as a,zipfile.ZipFile(io.BytesIO(win.read(jarname))) as b:
        classes=[n for n in a.namelist() if n.endswith('.class')];assert all(a.read(n)==b.read(n) for n in classes)
        for resource in ('currencies/catalogue.tsv','app-metadata.properties','migrations/V130_global__currency_tax_legal.sql','legal/TERMS.md','legal/PRIVACY.md','legal/THIRD-PARTY-NOTICES.txt','branding/inventory-logo.png'):assert a.read(resource)==b.read(resource),resource
result={'version':meta['version'],'vendor':meta['vendor'],'bundle_identifier':meta['bundle.identifier'],'matching_application_classes':len(classes),'windows_zip_crc':'passed','windows_native_version_resource':'passed','mac_plist_metadata':'passed','no_shop_data_in_windows_zip':True,'legal_resources':'passed','windows_execution_tested':False}
output=ROOT/'docs/evidence/package-structure.json';output.parent.mkdir(exist_ok=True);output.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
