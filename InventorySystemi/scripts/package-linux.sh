#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[[ $# -eq 0 ]] || { echo 'Usage: package-linux.sh (native Linux x86_64 host required)' >&2; exit 2; }
source scripts/package-common.sh
setup_target linux-x86_64
build_input
jpackage --type app-image --name InventorySystem --app-version "$version" \
  --vendor "$(python3 scripts/metadata.py vendor)" --description "$(python3 scripts/metadata.py description)" --copyright "$(python3 scripts/metadata.py copyright)" \
  --input "$build/input" --main-jar "inventory-system-$version.jar" --main-class main.Main \
  --module-path "$build/input/lib" --add-modules java.se,javafx.controls \
  --java-options '-Dfile.encoding=UTF-8' --java-options '--add-modules=javafx.controls' --dest "$build/image"
app="$build/image/InventorySystem"
python3 scripts/package_target.py notices "$target" "$app/Legal"
cat > "$app/launch.sh" <<'LAUNCH'
#!/bin/sh
set -eu
app_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec "$app_dir/bin/InventorySystem" "$@"
LAUNCH
chmod +x "$app/launch.sh"
# The installer resolves an absolute path at the final extraction location. No desktop changes at build time.
cat > "$app/install-desktop.sh" <<'DESKTOP'
#!/bin/sh
set -eu
app_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
install_dir="${XDG_DATA_HOME:-$HOME/.local/share}/applications"
mkdir -p "$install_dir"
# Escape Desktop Entry Exec special characters in paths, including literal % field codes.
escaped=$(printf '%s' "$app_dir/launch.sh" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g' -e 's/`/\\`/g' -e 's/\$/\\$/g' -e 's/%/%%/g')
printf '[Desktop Entry]\nType=Application\nName=Inventory System\nComment=Offline inventory, sales and shop management\nExec="%s"\nTerminal=false\nCategories=Office;Finance;\nStartupNotify=true\n' "$escaped" > "$install_dir/com.coodiclabs.inventorysystem.desktop"
printf '%s\n' "Installed menu entry in $install_dir. Keep the application folder in this location."
DESKTOP
chmod +x "$app/install-desktop.sh"
cat > "$app/InventorySystem.desktop" <<'DESKTOP'
[Desktop Entry]
Type=Application
Name=Inventory System
Comment=Offline inventory, sales and shop management
Exec=InventorySystem
Terminal=false
Categories=Office;Finance;
StartupNotify=true
DESKTOP
cat > "$app/README.txt" <<'README'
Extract the whole archive, keep all folders together, and run ./launch.sh.
Java 21 and JavaFX are bundled. No internet or separate Java installation is needed.
Requires a graphical Linux x86_64 session, GTK 3, X11/XWayland and font/OpenGL libraries.
Optional: run ./install-desktop.sh after moving the folder to its permanent location.
Data is separate in ~/.inventorysystem. Replacing the app does not delete shop data.
Remove the installed menu entry manually when uninstalling; preserve shop data and backups.
This target needs launch, setup, login, weighted sale, receipt and backup acceptance tests on Linux.
README
python3 scripts/package_target.py verify "$target" "$app"
# Verify the actual archive by extracting it into this target's build directory.
tar -czf "$stage/$stem.tar.gz" -C "$build/image" InventorySystem
mkdir "$build/archive-check"
tar -xzf "$stage/$stem.tar.gz" -C "$build/archive-check"
python3 scripts/package_target.py verify "$target" "$build/archive-check/InventorySystem"
python3 scripts/package_target.py publish "$target" "$stage"
