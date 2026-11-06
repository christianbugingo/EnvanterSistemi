#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[[ "$(uname -s)" == Darwin ]] || { echo "Build the Mac app on macOS." >&2; exit 2; }
if [[ -z "${JAVA_HOME:-}" && -d ../.tools/jdk/Contents/Home ]]; then
  export JAVA_HOME="$(cd ../.tools/jdk/Contents/Home && pwd)"
fi
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
[[ "$(java -version 2>&1 | rg -m 1 'version "')" == *"21."* ]] || { echo "JDK 21 is required." >&2; exit 2; }
command -v jpackage >/dev/null
command -v python3 >/dev/null
version=$(python3 scripts/release_tools.py version)
./mvnw clean package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/package/lib
stage=$(python3 scripts/release_tools.py prepare release-macos)
cp "target/inventory-system-${version}.jar" target/package/
jpackage --type app-image --name InventorySystem --app-version "$version" \
  --vendor "$(python3 scripts/metadata.py vendor)" --description "$(python3 scripts/metadata.py description)" --copyright "$(python3 scripts/metadata.py copyright)" --mac-package-identifier "$(python3 scripts/metadata.py bundle.identifier)" \
  --icon assets/InventorySystem.icns --input target/package --main-jar "inventory-system-${version}.jar" --main-class main.Main \
  --module-path target/package/lib --add-modules java.se,javafx.controls \
  --java-options '-Dfile.encoding=UTF-8' --java-options '--add-modules=javafx.controls' --dest "$stage"
python3 scripts/metadata.py mac "$stage/InventorySystem.app"
codesign --force --deep --sign - "$stage/InventorySystem.app"
jpackage --type dmg --name InventorySystem --app-version "$version" \
  --app-image "$stage/InventorySystem.app" --dest "$stage"
mv "$stage/InventorySystem-${version}.dmg" "$stage/InventorySystem.dmg"
python3 scripts/release_tools.py publish macos "$stage"
