"""Target checks, isolated staging and target-only publication (build tooling only)."""
from pathlib import Path
import argparse
import json
import os
import platform
import shutil
import struct
import subprocess
import zipfile
from metadata import metadata, bundle_notices

PROJECT = Path(__file__).resolve().parents[1]
TARGETS = {
    'macos-arm64': ('Darwin', 'arm64', 'mac-aarch64'),
    'macos-x86_64': ('Darwin', 'x86_64', 'mac'),
    'linux-x86_64': ('Linux', 'x86_64', 'linux'),
}

def validate_host(target, system, machine, jdk_arch):
    expected_os, expected_arch, _ = TARGETS[target]
    normalize = lambda a: {'aarch64': 'arm64', 'amd64': 'x86_64'}.get(a, a)
    if system != expected_os or normalize(machine) != expected_arch or normalize(jdk_arch) != expected_arch:
        raise ValueError(f'{target} requires a native {expected_os} {expected_arch} host and matching JDK 21; got host {system} {machine}, JDK {jdk_arch}. Cross-labeling/Rosetta builds are refused.')

def validate_native_host(target, jdk_arch):
    system = platform.system()
    if system == 'Darwin':
        translated = subprocess.run(['/usr/sbin/sysctl', '-in', 'sysctl.proc_translated'], capture_output=True, text=True)
        if translated.stdout.strip() == '1':
            raise ValueError('Rosetta is not an Intel Mac: native Intel hardware is required for macos-x86_64')
    validate_host(target, system, subprocess.check_output(['uname', '-m'], text=True).strip(), jdk_arch)

def checked_path(path):
    path = Path(path).absolute()
    if path.is_symlink() or not path.resolve().is_relative_to(PROJECT / 'target') or path.resolve() == PROJECT / 'target':
        raise ValueError('Cleanup/staging is restricted to a target-specific project target/ subfolder')
    return path

def record_deleted(paths):
    destination = PROJECT.parent / 'docs/evidence/packaging-deleted-files.txt'
    destination.parent.mkdir(parents=True, exist_ok=True)
    previous = destination.read_text().splitlines() if destination.exists() else []
    destination.write_text('\n'.join(sorted(set(previous + [str(p.relative_to(PROJECT.parent)) for p in paths]))) + '\n')

def remove_generated(path):
    if not path.exists(): return
    if path.is_symlink(): raise ValueError('Refusing symlink cleanup')
    files = [path] if path.is_file() else [p for p in path.rglob('*') if p.is_file() or p.is_symlink()]
    if any(p.name.endswith(('.mv.db', '.trace.db')) or {'receipts', 'migration-backups', 'backups'} & set(p.parts) for p in files):
        raise ValueError('Refusing to remove database, receipts or backups')
    record_deleted(files)
    if path.is_dir(): shutil.rmtree(path)
    else: path.unlink()

def prepare(path):
    path = checked_path(path)
    remove_generated(path)
    path.mkdir(parents=True)

def binary_kind(data):
    if data[:4] in (b'\xcf\xfa\xed\xfe', b'\xfe\xed\xfa\xcf'):
        endian = '<' if data[:4] == b'\xcf\xfa\xed\xfe' else '>'
        cpu = struct.unpack_from(endian + 'I', data, 4)[0]
        return ('Darwin', {0x1000007: 'x86_64', 0x100000c: 'arm64'}.get(cpu, 'unknown'))
    if data[:4] == b'\x7fELF':
        endian = '<' if data[5] == 1 else '>'
        return ('Linux', 'x86_64' if data[4] == 2 and struct.unpack_from(endian + 'H', data, 18)[0] == 62 else 'other')
    if data[:2] == b'MZ': return ('Windows', 'other')
    if data[:4] in (b'\xca\xfe\xba\xbe', b'\xbe\xba\xfe\xca', b'\xca\xfe\xba\xbf', b'\xbf\xba\xfe\xca'): return ('universal', 'unverified')
    return None

def verify_natives(root, target):
    expected_os, expected_arch, classifier = TARGETS[target]
    found = []
    jars = []
    for path in Path(root).rglob('*'):
        if not path.is_file(): continue
        if path.name.endswith(('.mv.db', '.trace.db')) or path.name == '.env': raise ValueError('Shop data found in package')
        if path.suffix == '.jar':
            jars.append(path.name)
            with zipfile.ZipFile(path) as archive:
                for name in archive.namelist():
                    # Class files also start CAFEBABE; inspect only native library members.
                    if name.lower().endswith(('.dll', '.so', '.dylib', '.jnilib')):
                        kind = binary_kind(archive.read(name))
                        if kind != (expected_os, expected_arch): raise ValueError(f'Wrong native {path.name}!{name}: {kind}')
                        found.append(path.name + '!' + name)
        else:
            with path.open('rb') as source: kind = binary_kind(source.read(64))
            if path.suffix.lower() in ('.dll', '.so', '.dylib', '.jnilib') and kind is None:
                raise ValueError(f'Unrecognized native library {path}')
            if kind:
                if kind != (expected_os, expected_arch): raise ValueError(f'Wrong executable/library {path}: {kind}')
                found.append(str(path.relative_to(root)))
    for component in ('base', 'graphics', 'controls'):
        wanted = f'javafx-{component}-'
        matches = [name for name in jars if name.startswith(wanted)]
        if not any(name.endswith('-' + classifier + '.jar') for name in matches): raise ValueError(f'Missing {component} {classifier} natives')
        allowed = {f'javafx-{component}-{metadata_version()}.jar', f'javafx-{component}-{metadata_version()}-{classifier}.jar'}
        if any(name not in allowed for name in matches): raise ValueError(f'Unexpected JavaFX classifier: {matches}')
    if not found: raise ValueError('No native executables/libraries inspected')
    return found

def metadata_version():
    import xml.etree.ElementTree as ET
    tree = ET.parse(PROJECT / 'pom.xml')
    return tree.findtext('./m:properties/m:javafx.version', namespaces={'m': 'http://maven.apache.org/POM/4.0.0'})

def verify_app(app, target):
    app = Path(app)
    executable = app / ('Contents/MacOS/InventorySystem' if target.startswith('macos') else 'bin/InventorySystem')
    output = subprocess.check_output(['file', str(executable)], text=True).strip()
    print(output)
    if target.startswith('macos'):
        arch = subprocess.check_output(['lipo', '-archs', str(executable)], text=True).strip()
        detail = subprocess.check_output(['lipo', '-info', str(executable)], text=True).strip()
        print(detail)
        if arch != TARGETS[target][1]: raise ValueError('Launcher architecture mismatch')
    else:
        detail = output
        if 'ELF 64-bit' not in output or 'x86-64' not in output: raise ValueError('Expected x86-64 ELF launcher')
    natives = verify_natives(app, target)
    evidence = {'target': target, 'file': output, 'lipo': detail if target.startswith('macos') else None,
                'native_files_checked': len(natives), 'wrong_natives': False,
                'runtime_execution_tested': False}
    (app / ('Contents/Resources' if target.startswith('macos') else 'lib/app') / 'architecture-check.json').write_text(json.dumps(evidence, indent=2) + '\n')
    return evidence

def publish(target, stage):
    stage = checked_path(stage)
    m = metadata(); stem = f"InventorySystem-{m['version']}-{target}"
    names = [stem + '.app', stem + '.dmg'] if target.startswith('macos') else [stem + '.tar.gz']
    if set(p.name for p in stage.iterdir()) != set(names): raise ValueError('Incomplete/unexpected target release')
    destination = PROJECT / 'dist' / ('macos' if target.startswith('macos') else 'linux')
    if destination.is_symlink() or not destination.resolve().is_relative_to(PROJECT / 'dist'): raise ValueError('Refusing symlink/outside-project destination')
    destination.mkdir(parents=True, exist_ok=True)
    # Only names belonging to this target can be replaced. Windows and other architectures are untouched.
    old = [p for p in destination.iterdir() if p.name.startswith('InventorySystem-') and any(p.name.endswith('-' + target + ext) for ext in ('.app', '.dmg', '.tar.gz'))]
    # Original unlabelled Mac release was arm64. Verify before treating it as legacy arm64.
    legacy = destination / 'InventorySystem.app'
    if target == 'macos-arm64' and legacy.exists():
        arch = subprocess.check_output(['lipo', '-archs', str(legacy / 'Contents/MacOS/InventorySystem')], text=True).strip()
        if arch != 'arm64': raise ValueError('Unlabelled legacy Mac build is not arm64; refusing removal')
        old += [p for p in (legacy, destination / 'InventorySystem.dmg') if p.exists()]
    # Stage is complete and verified by its builder before replacing any previous artifacts.
    for path in old: remove_generated(path)
    for name in names: shutil.move(str(stage / name), destination / name)
    print(destination)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('action', choices=('host', 'prepare', 'natives', 'verify', 'publish', 'notices'))
    parser.add_argument('target', choices=TARGETS)
    parser.add_argument('path', nargs='?')
    args = parser.parse_args()
    if args.action == 'host':
        validate_native_host(args.target, args.path)
    elif args.action == 'prepare': prepare(args.path)
    elif args.action == 'natives': print(f'Checked {len(verify_natives(Path(args.path), args.target))} native files')
    elif args.action == 'verify': print(json.dumps(verify_app(args.path, args.target), indent=2))
    elif args.action == 'publish': publish(args.target, Path(args.path))
    elif args.action == 'notices': bundle_notices(args.path)
