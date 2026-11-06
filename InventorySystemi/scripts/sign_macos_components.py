"""Sign nested native code, including natives in dependency JARs, from the inside out.
Signing credentials stay in Keychain. This helper reads only a public identity name from the environment.
"""
from pathlib import Path
import argparse
import os
import plistlib
import struct
import subprocess
import tempfile
import zipfile
from package_target import binary_kind

EXPECTED_ENTITLEMENTS = {
    'com.apple.security.cs.allow-jit': True,
    'com.apple.security.cs.allow-unsigned-executable-memory': True,
    'com.apple.security.cs.disable-library-validation': True,
}

def run(command):
    subprocess.run(command, check=True)

def executable(data):
    return struct.unpack_from('<I', data, 12)[0] == 2

def sign(path, identity, entitlements, is_executable=False):
    command = ['codesign', '--force', '--sign', identity, '--options', 'runtime', '--timestamp']
    if is_executable: command += ['--entitlements', str(entitlements)]
    run(command + [str(path)])
    run(['codesign', '--verify', '--strict', '--verbose=2', str(path)])

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('app', type=Path)
    parser.add_argument('entitlements', type=Path)
    args = parser.parse_args()
    app = args.app.resolve()
    project = Path(__file__).resolve().parents[1]
    if not app.is_relative_to(project / 'target') or app.suffix != '.app':
        raise ValueError('Signing is restricted to a staging .app inside project target/')
    identity = os.environ['MAC_SIGN_IDENTITY']
    if not identity.startswith('Developer ID Application: '): raise ValueError('Developer ID Application identity required')
    with args.entitlements.open('rb') as source:
        if plistlib.load(source) != EXPECTED_ENTITLEMENTS: raise ValueError('Unexpected entitlements; only the three JVM runtime keys are allowed')
    count = 0
    # Archive members are not covered individually by signing the outer application.
    for jar in sorted(app.rglob('*.jar')):
        replacements = {}
        with zipfile.ZipFile(jar) as archive:
            members = archive.infolist()
            native = [item for item in members if item.filename.lower().endswith(('.dylib', '.jnilib', '.so', '.dll'))]
            if not native: continue
            if any(item.filename.upper().endswith(('.SF', '.RSA', '.DSA', '.EC')) for item in members):
                raise ValueError('Refusing to invalidate a signed JAR: ' + jar.name)
            with tempfile.TemporaryDirectory(prefix='native-sign-', dir=project / 'target') as temporary:
                for index, item in enumerate(native):
                    data = archive.read(item)
                    if binary_kind(data) != ('Darwin', 'arm64'): raise ValueError('Foreign/unknown native: ' + item.filename)
                    path = Path(temporary) / f'{index}-{Path(item.filename).name}'
                    path.write_bytes(data)
                    sign(path, identity, args.entitlements, executable(data))
                    replacements[item.filename] = path.read_bytes()
                    count += 1
            replacement = jar.with_suffix('.signed-tmp')
            with zipfile.ZipFile(replacement, 'w') as destination:
                for item in members:
                    destination.writestr(item, replacements[item.filename] if item.filename in replacements else archive.read(item))
                destination.comment = archive.comment
        replacement.replace(jar)
    # Sign all standalone Mach-O items. Reject unsupported native architecture before signing.
    for path in sorted(app.rglob('*'), key=lambda p: len(p.parts), reverse=True):
        if path.is_symlink() or not path.is_file() or path.suffix == '.jar': continue
        with path.open('rb') as source: header = source.read(64)
        kind = binary_kind(header)
        if kind is None: continue
        if kind != ('Darwin', 'arm64'): raise ValueError('Foreign native code: ' + str(path))
        sign(path, identity, args.entitlements, executable(header))
        count += 1
    # Seal nested bundles after their members. The runtime bundle's entry point is a dylib.
    nested = [p.parent.parent for p in app.rglob('Contents/Info.plist') if p.parent.parent != app]
    for bundle in sorted(set(nested), key=lambda p: len(p.parts), reverse=True):
        sign(bundle, identity, args.entitlements)
    sign(app, identity, args.entitlements, True)
    run(['codesign', '--verify', '--deep', '--strict', '--verbose=2', str(app)])
    print(f'Signed and verified {count} native items plus nested bundles and outer app.')

if __name__ == '__main__': main()
