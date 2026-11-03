#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "Build this package on macOS so jpackage selects native macOS JavaFX and runtime files." >&2
  exit 2
fi
if [[ -z "${JAVA_HOME:-}" && -d ../.tools/jdk/Contents/Home ]]; then
  export JAVA_HOME="$(cd ../.tools/jdk/Contents/Home && pwd)"
fi
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
architecture=$(uname -m)
case "$architecture" in
  arm64|x86_64) ;;
  *) echo "Unsupported macOS architecture: $architecture" >&2; exit 2 ;;
esac
java_version=$(java -version 2>&1 | head -n 1)
if [[ "$java_version" != *"21."* ]]; then
  echo "JDK 21 is required; active runtime is: $java_version" >&2
  exit 2
fi
command -v jpackage >/dev/null || { echo "jpackage is unavailable; install a full JDK 21." >&2; exit 2; }

./mvnw clean package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/package/lib
package_destination="${INVENTORY_DIST_DIR:-dist/macos-${architecture}-1.1.0}"
if [[ -e "$package_destination/InventorySystem.app" ]]; then
  echo "An app already exists at $package_destination. Set INVENTORY_DIST_DIR to a new output folder." >&2
  exit 2
fi
mkdir -p target/package "$package_destination"
cp target/inventory-system-1.1.0.jar target/package/
jpackage --type app-image --name InventorySystem --app-version 1.1.0 \
  --input target/package --main-jar inventory-system-1.1.0.jar --main-class main.Main \
  --module-path target/package/lib --add-modules java.se,javafx.controls \
  --java-options '-Dfile.encoding=UTF-8' --java-options '--add-modules=javafx.controls' --dest "$package_destination"
jpackage --type dmg --name InventorySystem --app-version 1.1.0 \
  --app-image "$package_destination/InventorySystem.app" --dest "$package_destination"

printf "Built app and installer in: %s\n" "$package_destination"
