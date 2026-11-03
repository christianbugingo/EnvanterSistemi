"""Convert the supplied logo to native icon formats without changing its artwork (macOS)."""
from pathlib import Path
import subprocess
import struct
import shutil

project=Path(__file__).resolve().parent.parent
source=project/'src/branding/inventory-logo.png'
assets=project/'assets';assets.mkdir(exist_ok=True)
work=project/'target/icon-work'
if work.exists():shutil.rmtree(work)
iconset=work/'InventorySystem.iconset';iconset.mkdir(parents=True)
for size in (16,32,128,256,512):
    for multiplier,suffix in ((1,''),(2,'@2x')):
        target=iconset/f'icon_{size}x{size}{suffix}.png'
        subprocess.run(['sips','-z',str(size*multiplier),str(size*multiplier),str(source),'--out',str(target)],check=True,stdout=subprocess.DEVNULL)
subprocess.run(['iconutil','-c','icns',str(iconset),'-o',str(assets/'InventorySystem.icns')],check=True)
images=[]
for size in (16,32,48,64,128,256):
    target=work/f'windows-{size}.png'
    subprocess.run(['sips','-z',str(size),str(size),str(source),'--out',str(target)],check=True,stdout=subprocess.DEVNULL)
    images.append((size,target.read_bytes()))
offset=6+16*len(images)
header=bytearray(struct.pack('<HHH',0,1,len(images)))
for size,png in images:
    header.extend(struct.pack('<BBBBHHII',size if size<256 else 0,size if size<256 else 0,0,0,1,32,len(png),offset));offset+=len(png)
(assets/'InventorySystem.ico').write_bytes(bytes(header)+b''.join(png for _,png in images))
print('Created native Mac and Windows icons from the original logo.')
