# Native Windows installer builder; portable ZIP can also be assembled on a Mac.
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
if ([Environment]::OSVersion.Platform -ne [PlatformID]::Win32NT) { throw 'Run native jpackage on Windows.' }
if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) { throw 'Install JDK 21 with jpackage.' }
$version = ([xml](Get-Content pom.xml)).project.version
& .\mvnw.cmd clean package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/package/lib
if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }
$stage = 'target/release-windows-native'
if (Test-Path $stage) { Remove-Item -Recurse -Force $stage }
New-Item -ItemType Directory -Force $stage | Out-Null
Copy-Item "target/inventory-system-$version.jar" target/package/
jpackage --type app-image --name InventorySystem --app-version $version --icon assets/InventorySystem.ico --input target/package --main-jar "inventory-system-$version.jar" --main-class main.Main --module-path target/package/lib --add-modules java.se,javafx.controls --java-options '-Dfile.encoding=UTF-8' --java-options '--add-modules=javafx.controls' --dest $stage
if ($LASTEXITCODE -ne 0) { throw 'App image creation failed.' }
jpackage --type msi --name InventorySystem --app-version $version --app-image "$stage/InventorySystem" --dest $stage
if ($LASTEXITCODE -ne 0) { throw 'MSI creation failed. Check WiX tooling.' }
if (Test-Path dist/windows) { Remove-Item -Recurse -Force dist/windows }
New-Item -ItemType Directory -Force dist | Out-Null
Move-Item $stage dist/windows
Write-Host 'Latest Windows app and MSI: dist/windows'
