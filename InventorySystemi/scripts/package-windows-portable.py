"""Create a Windows x64 offline app ZIP from macOS/Linux/Windows; no Windows execution claimed."""
from pathlib import Path
import argparse
import shutil
import subprocess
import zipfile
import json
import io
import struct
import os
from release_tools import PROJECT, version, clean_stage, publish
from windows_icon import embed_icon

ROOT = PROJECT.parent
parser = argparse.ArgumentParser()
parser.add_argument('--runtime-zip', type=Path, default=ROOT / '.tools/windows-jdk21.zip')
args = parser.parse_args()
runtime_zip = args.runtime_zip.resolve()
if not runtime_zip.is_file():
    runtime_zip.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(['curl', '-fL', 'https://corretto.aws/downloads/latest/amazon-corretto-21-x64-windows-jdk.zip', '-o', str(runtime_zip)], check=True)
wrapper = str(ROOT / ('mvnw.cmd' if os.name == 'nt' else 'mvnw'))
subprocess.run([wrapper, '-B', 'package'], cwd=ROOT, check=True)
libs = PROJECT / 'target/windows-libs'
if libs.exists(): shutil.rmtree(libs)
subprocess.run([wrapper, '-B', 'dependency:copy-dependencies', '-Djavafx.platform=win', '-DincludeScope=runtime', '-DoutputDirectory=target/windows-libs'], cwd=ROOT, check=True)
for component in ('base', 'graphics', 'controls'):
    if not list(libs.glob(f'javafx-{component}-*-win.jar')): raise SystemExit('Missing Windows JavaFX native dependency')
if any('mac-' in p.name or '-linux' in p.name for p in libs.iterdir()): raise SystemExit('Wrong-platform JavaFX dependency')
stage = clean_stage('release-windows')
app = stage / 'InventorySystem'
(app / 'app').mkdir(parents=True)
shutil.copytree(libs, app / 'lib')
jar = f'inventory-system-{version()}.jar'
shutil.copyfile(PROJECT / 'target' / jar, app / 'app' / jar)
runtime = app / 'runtime'
runtime.mkdir()
with zipfile.ZipFile(runtime_zip) as archive:
    if archive.testzip(): raise SystemExit('Corrupt runtime archive')
    for item in archive.infolist():
        parts = Path(item.filename).parts
        if len(parts) < 2: continue
        relative = Path(*parts[1:])
        if relative.is_absolute() or '..' in relative.parts: raise SystemExit('Unsafe runtime archive path')
        # Keep the distributable runtime, configuration and all notices, omitting development jmods/src.
        if relative.parts[0] not in ('bin','lib','conf','legal','release','LICENSE','NOTICE','ADDITIONAL_LICENSE_INFO','ASSEMBLY_EXCEPTION'): continue
        if relative.name == 'src.zip': continue
        target = runtime / relative
        if item.is_dir(): target.mkdir(parents=True, exist_ok=True)
        else:
            target.parent.mkdir(parents=True, exist_ok=True)
            with archive.open(item) as source, target.open('wb') as destination: shutil.copyfileobj(source, destination)
with zipfile.ZipFile(runtime_zip) as archive:
    jmod = next(name for name in archive.namelist() if name.endswith('/jmods/jdk.jpackage.jmod'))
    with zipfile.ZipFile(io.BytesIO(archive.read(jmod))) as module:
        (app / 'InventorySystem.exe').write_bytes(module.read('classes/jdk/jpackage/internal/resources/jpackageapplauncherw.exe'))
embed_icon(app / 'InventorySystem.exe', PROJECT / 'assets/InventorySystem.ico')
shutil.copyfile(PROJECT / 'assets/InventorySystem.ico', app / 'InventorySystem.ico')
h2 = next((app / 'lib').glob('h2-*.jar')).name
config = ('[Application]\napp.classpath=$APPDIR/' + jar + '\napp.mainclass=main.Main\n'
          'app.classpath=$APPDIR/../lib/' + h2 + '\n\n[JavaOptions]\n'
          'java-options=-Djpackage.app-version=' + version() + '\n'
          'java-options=-Dfile.encoding=UTF-8\njava-options=--module-path\n'
          'java-options=$APPDIR/../lib\njava-options=--add-modules=javafx.controls\n')
(app / 'app/InventorySystem.cfg').write_text(config, encoding='utf-8')
(app / 'OPENJDK-LAUNCHER-NOTICE.txt').write_text('The Windows launcher is from the bundled Amazon Corretto JDK 21 jdk.jpackage module; its icon resources have been customized.\nLicensing notices are in runtime/legal/jdk.jpackage/.\nUpstream source: https://github.com/openjdk/jdk21u/tree/master/src/jdk.jpackage\n', encoding='utf-8')
launcher = (app / 'InventorySystem.exe').read_bytes()
pe_offset = struct.unpack_from('<I', launcher, 0x3c)[0]
if launcher[pe_offset:pe_offset+4] != b'PE\0\0' or struct.unpack_from('<H', launcher, pe_offset+4)[0] != 0x8664:
    raise SystemExit('Expected a Windows x64 native launcher')
release = (runtime / 'release').read_text()
if 'OS_NAME="Windows"' not in release or not any(arch in release for arch in ('OS_ARCH="amd64"','OS_ARCH="x86_64"')): raise SystemExit('Expected Windows x64 runtime')
for binary in ('bin/java.exe', 'bin/javaw.exe', 'bin/server/jvm.dll'):
    if (runtime / binary).read_bytes()[:2] != b'MZ': raise SystemExit(f'Invalid Windows PE file: {binary}')
if not (runtime / 'lib/modules').is_file(): raise SystemExit('Runtime module image missing')
command = f'--module-path "%~dp0lib" --add-modules javafx.controls -Dfile.encoding=UTF-8 -cp "%~dp0app\\{jar};%~dp0lib\\*" main.Main'
(app / 'Start InventorySystem.cmd').write_bytes(('@echo off\r\ncd /d "%~dp0"\r\nstart "" "%~dp0runtime\\bin\\javaw.exe" ' + command + '\r\n').encode())
(app / 'Run with console.cmd').write_bytes(('@echo off\r\ncd /d "%~dp0"\r\n"%~dp0runtime\\bin\\java.exe" ' + command + '\r\nif errorlevel 1 pause\r\n').encode())
(app / 'README.txt').write_text('Inventory System '+version()+' — Windows x64 portable\n\nExtract the ENTIRE ZIP before running. Double-click InventorySystem.exe. If needed, use Start InventorySystem.cmd as a fallback.\nJava and JavaFX are bundled; no internet or separate Java installation is needed.\nUse Run with console.cmd to inspect startup errors.\nData: %USERPROFILE%\\.inventorysystem\\inventory.mv.db\nAdmin/staff share data when using the same Windows user profile.\nCreate the first administrator at first launch; no default password is seeded.\nOpen a shift before selling. Card payment selection records payment only.\nThis package was assembled on macOS and must be tested on Windows.\n', encoding='utf-8')
manifest={'version':version(),'platform':'windows-x64','runtime':'Amazon Corretto 21','launcher':'InventorySystem.exe','windows_execution_tested':False,'custom_icon':True}
(app / 'build-info.json').write_text(json.dumps(manifest,indent=2)+'\n')
shutil.make_archive(str(stage / 'InventorySystem-Windows-x64'), 'zip', root_dir=stage, base_dir='InventorySystem')
publish('windows',stage)
