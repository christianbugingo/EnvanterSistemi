$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

mvn clean package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/package/lib
New-Item -ItemType Directory -Force dist | Out-Null
Copy-Item target/inventory-system-1.0.0.jar target/package/
jpackage --type app-image --name InventorySystem --app-version 1.0.0 `
  --input target/package --main-jar inventory-system-1.0.0.jar --main-class main.Main `
  --module-path target/package/lib --add-modules javafx.controls `
  --java-options '--module-path $APPDIR/lib --add-modules javafx.controls' --dest dist
jpackage --type msi --name InventorySystem --app-version 1.0.0 `
  --app-image dist/InventorySystem --dest dist
