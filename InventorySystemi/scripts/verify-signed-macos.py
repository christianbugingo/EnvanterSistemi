"""Verify a mounted Developer ID DMG and launch an untouched copy with disposable data.
An external Java verification agent exercises packaged services without modifying the app signature.
"""
from pathlib import Path
import json
import hashlib
import os
import shutil
import subprocess
import sys
import tempfile
import time

project=Path(__file__).resolve().parents[1]
dmg=Path(sys.argv[1]).resolve()
evidence=Path(sys.argv[2]).resolve()
if not evidence.is_relative_to(project/'target'): raise ValueError('Evidence must be project-local staging')
jdk=Path(os.environ['JAVA_HOME'])
work=Path(tempfile.mkdtemp(prefix='signed-offline-',dir=project/'target'))
mount=work/'mount';mount.mkdir()
attached=False

def run(command):
    subprocess.run([str(x) for x in command],check=True)

def launch(app, home, agent=None):
    home.mkdir(parents=True)
    (home/'shop').mkdir()
    env=os.environ.copy()
    options=f'-Duser.home={home} -Djava.io.tmpdir={work}'
    if agent: options+=f' -javaagent:{agent}'
    env.update(HOME=str(home),JAVA_TOOL_OPTIONS=options,DB_URL='jdbc:h2:file:'+str(home/'shop/inventory')+';DATABASE_TO_LOWER=TRUE',DB_USER='sa',DB_PASSWORD='',INVENTORY_SIGNED_TEST_HOME=str(home))
    command=['/usr/bin/sandbox-exec','-p','(version 1)(allow default)(deny network*)',str(app/'Contents/MacOS/InventorySystem')]
    logfile=evidence/('offline-workflow.log' if agent else 'offline-startup.log')
    with logfile.open('w') as log:
        process=subprocess.Popen(command,env=env,cwd=home,stdout=log,stderr=log)
        try:
            if agent:
                deadline=time.monotonic()+45
                while not (home/'passed.txt').exists():
                    if process.poll() is not None or time.monotonic()>deadline: raise RuntimeError('Signed workflow failed: '+logfile.read_text())
                    time.sleep(.25)
            try:
                process.wait(timeout=6)
                raise RuntimeError('Signed app exited during startup: '+logfile.read_text())
            except subprocess.TimeoutExpired: pass
        finally:
            if process.poll() is None:
                process.terminate();process.wait(timeout=10)
    text=logfile.read_text()
    if 'Exception' in text or 'ERROR' in text: raise RuntimeError('Signed app logged an error: '+text)

try:
    run(['hdiutil','attach','-readonly','-nobrowse','-noautoopen','-mountpoint',mount,dmg]);attached=True
    sources=list(mount.glob('*.app'))
    if len(sources)!=1: raise ValueError('Expected exactly one app in DMG')
    copied=work/sources[0].name
    run(['ditto',sources[0],copied])
    run(['codesign','--verify','--deep','--strict','--verbose=2',copied])
    run(['spctl','--assess','--type','execute','-v',copied])
    launch(copied,work/'startup-home')
    # Compile the non-shipped agent against this exact packaged JAR/dependencies.
    libs=copied/'Contents/app/lib'
    jar=next((copied/'Contents/app').glob('inventory-system-*.jar'))
    run([jdk/'bin/javac','--module-path',libs,'--add-modules','javafx.controls','-cp',str(jar)+os.pathsep+str(libs/'*'),'-d',work,project/'packaging/macos/SignedOfflineSmoke.java'])
    manifest=work/'agent-manifest.mf';manifest.write_text('Premain-Class: SignedOfflineSmoke\n\n')
    agent=work/'offline-test-agent.jar'
    run([jdk/'bin/jar','--create','--file',agent,'--manifest',manifest,'-C',work,'SignedOfflineSmoke.class'])
    launch(copied,work/'workflow-home',agent)
    run(['codesign','--verify','--deep','--strict','--verbose=2',copied])
    result={'dmg_sha256':hashlib.sha256(dmg.read_bytes()).hexdigest(),'platform':'macos-arm64','dmg_mounted_readonly':True,'gatekeeper_copied_app':'accepted','signed_bundle_modified':False,'native_startup':True,'network_access_denied':True,'physical_wifi_changed':False,'real_shop_data_used':False,'workflow':(work/'workflow-home/passed.txt').read_text(),'test_data_location':str(work)}
    (evidence/'signed-offline.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result,indent=2))
finally:
    if attached: run(['hdiutil','detach',mount])
    # Disposable evidence/data intentionally retained in project target/ for inspection.
