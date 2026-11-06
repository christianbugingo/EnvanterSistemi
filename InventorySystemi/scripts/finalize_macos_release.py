"""Install verified LOCAL arm64 artifacts, archiving the old release without deleting it.
No network operations. Run only after signed/notarized/Gatekeeper/offline verification.
"""
from pathlib import Path
from datetime import datetime, timezone
import hashlib
import json
import os
import shutil
import sys
import uuid
from metadata import metadata

PROJECT=Path(__file__).resolve().parents[1]

def sha(path):
    digest=hashlib.sha256()
    with Path(path).open('rb') as source:
        for block in iter(lambda:source.read(1024*1024),b''): digest.update(block)
    return digest.hexdigest()

def moved_files(source, destination):
    paths=sorted(source.rglob('*')) if source.is_dir() else [source]
    return [{'from':str(p),'to':str(destination/(p.relative_to(source))) if source.is_dir() else str(destination),
             'sha256':None if p.is_symlink() else sha(p),'symlink':os.readlink(p) if p.is_symlink() else None}
            for p in paths if p.is_file() or p.is_symlink()]

def install(stage,evidence):
    stage=Path(stage).resolve();evidence=Path(evidence).resolve()
    if not stage.is_relative_to(PROJECT/'target') or not evidence.is_relative_to(PROJECT/'target'):
        raise ValueError('Candidate/evidence must be inside project target/')
    submitted=json.loads((evidence/'submission.json').read_text())
    offline=json.loads((evidence/'signed-offline.json').read_text())
    if submitted.get('status')!='Accepted' or not submitted.get('id'):
        raise ValueError('Apple acceptance is required; existing release unchanged')
    if offline.get('native_startup') is not True or offline.get('network_access_denied') is not True or offline.get('signed_bundle_modified') is not False or offline.get('gatekeeper_copied_app')!='accepted':
        raise ValueError('Signed, unmodified offline workflow verification is required')
    stem=f"InventorySystem-{metadata()['version']}-macos-arm64"
    names=[stem+'.app',stem+'.dmg',stem+'.dmg.sha256']
    app,dmg=stage/names[0],stage/names[1]
    if not app.is_dir() or not dmg.is_file() or sha(dmg)!=offline.get('dmg_sha256'):
        raise ValueError('Verified candidate missing/changed; existing release unchanged')
    (stage/names[2]).write_text(sha(dmg)+'  '+dmg.name+'\n')
    destination=PROJECT/'dist/macos'
    previous=PROJECT.parent/'docs/evidence/previous-release'
    if not destination.resolve().is_relative_to(PROJECT/'dist') or not previous.resolve().is_relative_to(PROJECT.parent/'docs/evidence'):
        raise ValueError('Refusing outside-project destination')
    destination.mkdir(parents=True,exist_ok=True)
    archive=previous/(datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')+'-'+uuid.uuid4().hex[:8])
    old=[destination/name for name in names if (destination/name).exists()]
    for path in old:
        if path.is_symlink(): raise ValueError('Refusing symlink release replacement')
        if path.is_dir() and any(p.name.endswith(('.mv.db','.trace.db')) for p in path.rglob('*')):
            raise ValueError('Unexpected database in release; no changes made')
    moves=[];archived=[];installed=[]
    try:
        if old: archive.mkdir(parents=True)
        for path in old:
            to=archive/path.name;moves+=moved_files(path,to)
            shutil.move(str(path),str(to));archived.append((path,to))
        for name in names:
            to=destination/name
            moves+=moved_files(stage/name,to)
            shutil.move(str(stage/name),str(to));installed.append((stage/name,to))
    except Exception:
        for source,current in reversed(installed): shutil.move(str(current),str(source))
        for original,saved in reversed(archived): shutil.move(str(saved),str(original))
        raise
    result={'local_only':True,'submission_id':submitted['id'],'previous_release':str(archive) if old else None,
            'deleted_files':[],'moved_files':moves,'artifacts':[str(destination/name) for name in names],
            'dmg_sha256':sha(destination/names[1])}
    (evidence/'local-release-install.json').write_text(json.dumps(result,indent=2)+'\n')
    # Source evidence is already archived by the signing driver; add the post-install movement record.
    report=PROJECT.parent/'docs/evidence/release-1.3.0';report.mkdir(parents=True,exist_ok=True)
    (report/'local-release-install.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({k:v for k,v in result.items() if k!='moved_files'},indent=2))
    return result

if __name__=='__main__': install(sys.argv[1],sys.argv[2])
