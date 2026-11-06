"""Publish only complete generated releases and keep one build per platform."""
from pathlib import Path
import shutil
import sys
import xml.etree.ElementTree as ET

PROJECT = Path(__file__).resolve().parent.parent
DIST = PROJECT / 'dist'

def version():
    from metadata import metadata
    return metadata()['version']

def clean_stage(name):
    if name not in ('release-macos', 'release-windows'):
        raise ValueError('Unknown generated staging folder')
    stage = PROJECT / 'target' / name
    if stage.is_symlink():
        raise ValueError('Refusing symbolic-link staging folder')
    if stage.exists():
        shutil.rmtree(stage)
    stage.mkdir(parents=True)
    return stage

def prune_legacy():
    for path in list(DIST.iterdir()):
        if path.name in ('macos', 'windows'):
            continue
        known = (path.name == 'InventorySystem.app' or path.name.startswith(('macos-', 'windows-'))
                 or (path.name.startswith('InventorySystem') and path.suffix in ('.dmg', '.zip', '.msi')))
        if known:
            if path.is_symlink() or path.is_file(): path.unlink()
            elif path.is_dir(): shutil.rmtree(path)
    tools = PROJECT.parent / '.tools'
    if tools.is_dir():
        for path in tools.iterdir():
            if path.name.startswith('inventory-') and path.is_dir() and not path.is_symlink() and any(path.rglob('InventorySystem.app')):
                shutil.rmtree(path)

def publish(platform, staged):
    if platform not in ('macos', 'windows'): raise ValueError('Unknown platform')
    staged = Path(staged).resolve()
    if not staged.is_relative_to((PROJECT / 'target').resolve()):
        raise ValueError('Release must come from the generated target folder')
    if not staged.is_dir() or not any(staged.iterdir()): raise ValueError('Empty release')
    DIST.mkdir(exist_ok=True)
    destination = DIST / platform
    if destination.is_symlink(): raise ValueError('Refusing symbolic-link release folder')
    if destination.exists(): shutil.rmtree(destination)
    shutil.move(str(staged), destination)
    prune_legacy()
    print(destination)

if __name__ == '__main__':
    if sys.argv[1] == 'version': print(version())
    elif sys.argv[1] == 'prepare': print(clean_stage(sys.argv[2]))
    elif sys.argv[1] == 'publish': publish(sys.argv[2], sys.argv[3])
    else: raise SystemExit('Unknown release operation')
