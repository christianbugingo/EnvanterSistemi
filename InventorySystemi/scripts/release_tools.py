"""Publish only complete generated releases and keep one build per platform."""
from pathlib import Path
import shutil
import sys
# Architecture-specific Mac/Linux releases use package_target.py.

PROJECT = Path(__file__).resolve().parent.parent
DIST = PROJECT / 'dist'

def version():
    from metadata import metadata
    return metadata()['version']

def clean_stage(name):
    if name != 'release-windows':
        raise ValueError('Unknown generated staging folder')
    stage = PROJECT / 'target' / name
    if stage.is_symlink():
        raise ValueError('Refusing symbolic-link staging folder')
    if stage.exists():
        shutil.rmtree(stage)
    stage.mkdir(parents=True)
    return stage

def publish(platform, staged):
    if platform != 'windows': raise ValueError('Unknown platform')
    staged = Path(staged).resolve()
    if not staged.is_relative_to((PROJECT / 'target').resolve()):
        raise ValueError('Release must come from the generated target folder')
    if not staged.is_dir() or not any(staged.iterdir()): raise ValueError('Empty release')
    DIST.mkdir(exist_ok=True)
    destination = DIST / platform
    if destination.is_symlink(): raise ValueError('Refusing symbolic-link release folder')
    if destination.exists(): shutil.rmtree(destination)
    shutil.move(str(staged), destination)
    print(destination)

if __name__ == '__main__':
    if sys.argv[1] == 'version': print(version())
    elif sys.argv[1] == 'prepare': print(clean_stage(sys.argv[2]))
    elif sys.argv[1] == 'publish': publish(sys.argv[2], sys.argv[3])
    else: raise SystemExit('Unknown release operation')
