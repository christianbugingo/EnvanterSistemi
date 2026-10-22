#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "Build this package on macOS so jpackage selects native macOS JavaFX and runtime files." >&2
  exit 2
fi
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
mkdir -p target/package dist
cp target/inventory-system-1.0.0.jar target/package/
jpackage --type app-image --name InventorySystem --app-version 1.0.0 \
  --input target/package --main-jar inventory-system-1.0.0.jar --main-class main.Main \
  --module-path target/package/lib --add-modules javafx.controls \
  --java-options '-Dfile.encoding=UTF-8' --dest dist
jpackage --type dmg --name InventorySystem --app-version 1.0.0 \
  --app-image dist/InventorySystem.app --dest dist
