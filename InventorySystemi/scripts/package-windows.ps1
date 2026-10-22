$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    throw 'JDK 21 with jpackage must be installed and available on PATH.'
}
$javaVersion = (& java -version 2>&1 | Select-Object -First 1)
if ($javaVersion -notmatch '21\.') {
    throw "JDK 21 is required; active runtime is: $javaVersion"
}

& .\mvnw.cmd clean package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/package/lib
if ($LASTEXITCODE -ne 0) { throw "Maven packaging failed with exit code $LASTEXITCODE" }
New-Item -ItemType Directory -Force dist | Out-Null
Copy-Item target/inventory-system-1.0.0.jar target/package/
jpackage --type app-image --name InventorySystem --app-version 1.0.0 `
  --input target/package --main-jar inventory-system-1.0.0.jar --main-class main.Main `
  --module-path target/package/lib --add-modules javafx.controls `
  --java-options '-Dfile.encoding=UTF-8' --dest dist
jpackage --type msi --name InventorySystem --app-version 1.0.0 `
  --app-image dist/InventorySystem --dest dist
