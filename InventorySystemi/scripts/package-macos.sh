#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

mvn clean package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/package/lib
cp target/inventory-system-1.0.0.jar target/package/
mkdir -p dist
jpackage --type app-image --name InventorySystem --app-version 1.0.0 \
  --input target/package --main-jar inventory-system-1.0.0.jar --main-class main.Main \
  --module-path target/package/lib --add-modules javafx.controls \
  --java-options '--module-path $APPDIR/lib --add-modules javafx.controls' --dest dist
jpackage --type dmg --name InventorySystem --app-version 1.0.0 \
  --app-image dist/InventorySystem.app --dest dist
