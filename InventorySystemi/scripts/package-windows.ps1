$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
if ([Environment]::OSVersion.Platform -ne [PlatformID]::Win32NT) {
    throw 'Build the Windows package on Windows to include the correct native runtime.'
}

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    throw 'JDK 21 with jpackage must be installed and available on PATH.'
}
$javaVersion = (& java -version 2>&1 | Select-Object -First 1)
if ($javaVersion -notmatch '21\.') {
    throw "JDK 21 is required; active runtime is: $javaVersion"
}

& .\mvnw.cmd clean package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/package/lib
if ($LASTEXITCODE -ne 0) { throw "Maven packaging failed with exit code $LASTEXITCODE" }
$packageDestination = if ($env:INVENTORY_DIST_DIR) { $env:INVENTORY_DIST_DIR } else { 'dist/windows-1.1.0' }
if (Test-Path (Join-Path $packageDestination 'InventorySystem')) { throw 'Output already exists. Set INVENTORY_DIST_DIR to a new folder.' }
New-Item -ItemType Directory -Force $packageDestination | Out-Null
Copy-Item target/inventory-system-1.1.0.jar target/package/
jpackage --type app-image --name InventorySystem --app-version 1.1.0 `
  --input target/package --main-jar inventory-system-1.1.0.jar --main-class main.Main `
  --module-path target/package/lib --add-modules java.se,javafx.controls `
  --java-options '-Dfile.encoding=UTF-8' --java-options '--add-modules=javafx.controls' --dest $packageDestination
if ($LASTEXITCODE -ne 0) { throw "App image creation failed with exit code $LASTEXITCODE" }
jpackage --type msi --name InventorySystem --app-version 1.1.0 `
  --app-image (Join-Path $packageDestination 'InventorySystem') --dest $packageDestination

if ($LASTEXITCODE -ne 0) { throw "MSI creation failed with exit code $LASTEXITCODE" }
