#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
requested_arch="${1:-$(uname -m)}"
[[ $# -le 1 && ( "$requested_arch" == arm64 || "$requested_arch" == x86_64 ) ]] || { echo 'Usage: package-macos.sh [arm64|x86_64]' >&2; exit 2; }
source scripts/package-common.sh
setup_target "macos-$requested_arch"
build_input
jpackage --type app-image --name InventorySystem --app-version "$version" \
  --vendor "$(python3 scripts/metadata.py vendor)" --description "$(python3 scripts/metadata.py description)" --copyright "$(python3 scripts/metadata.py copyright)" --mac-package-identifier "$(python3 scripts/metadata.py bundle.identifier)" \
  --icon assets/InventorySystem.icns --input "$build/input" --main-jar "inventory-system-$version.jar" --main-class main.Main \
  --module-path "$build/input/lib" --add-modules java.se,javafx.controls \
  --java-options '-Dfile.encoding=UTF-8' --java-options '--add-modules=javafx.controls' --dest "$stage"
python3 scripts/metadata.py mac "$stage/InventorySystem.app"
python3 scripts/package_target.py verify "$target" "$stage/InventorySystem.app"
codesign --force --deep --sign - "$stage/InventorySystem.app"
codesign --verify --deep --strict "$stage/InventorySystem.app"
jpackage --type dmg --name InventorySystem --app-version "$version" \
  --app-image "$stage/InventorySystem.app" --dest "$stage"
hdiutil verify "$stage/InventorySystem-$version.dmg"
mv "$stage/InventorySystem-$version.dmg" "$stage/$stem.dmg"
mv "$stage/InventorySystem.app" "$stage/$stem.app"
python3 scripts/package_target.py publish "$target" "$stage"
