from pathlib import Path
import plistlib
import shutil
ROOT=Path(__file__).resolve().parents[2]
def metadata():
    return dict(line.split('=',1) for line in (ROOT/'app-metadata.properties').read_text().splitlines() if line and not line.startswith('#'))
def bundle_notices(destination):
    destination=Path(destination);destination.mkdir(parents=True,exist_ok=True)
    for name in ('LICENSE.txt','TERMS.md','PRIVACY.md','THIRD-PARTY-NOTICES.txt','app-metadata.properties'):
        shutil.copyfile(ROOT/name,destination/name)
    shutil.copytree(ROOT/'legal-licenses',destination/'legal-licenses',dirs_exist_ok=True)
def mac(app):
    m=metadata();path=Path(app)/'Contents/Info.plist'
    with path.open('rb') as f:data=plistlib.load(f)
    data.update(CFBundleName=m['product.name'],CFBundleDisplayName=m['product.name'],CFBundleIdentifier=m['bundle.identifier'],CFBundleVersion=m['version'],CFBundleShortVersionString=m['version'],NSHumanReadableCopyright=m['copyright'],CFBundleGetInfoString=m['product.name']+' '+m['version']+' · '+m['vendor'],Developer=m['vendor'])
    with path.open('wb') as f:plistlib.dump(data,f)
    bundle_notices(Path(app)/'Contents/Resources/Legal')
if __name__=='__main__':
    import sys
    if sys.argv[1]=='mac':mac(sys.argv[2])
    else:print(metadata()[sys.argv[1]])
