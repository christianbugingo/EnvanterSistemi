# Repeatable native packaging — Inventory System 1.3.0

Build tooling requires Git checkout, a complete **JDK 21** (java, javac, jlink, jpackage), Python 3, Bash, Maven wrapper dependencies, `file` and `tar`. macOS additionally needs Apple command-line tools (`lipo`, `codesign`, `hdiutil`). Linux needs a graphical x86_64 Ubuntu/Debian-based environment and the JDK's jpackage/jlink OS prerequisites. Building/downloading dependencies may require internet; the installed app never needs it.

The Mac script accepts `arm64` or `x86_64`; omitting the argument uses `uname -m`. Linux accepts only a native Linux x86_64 host. Both reject a mismatched JDK or OS before Maven/cleanup; Rosetta is not accepted as an Intel Mac. Maven profiles are `mac-aarch64`, `mac-x86_64` and `linux`; their JavaFX classifiers are `mac-aarch64`, `mac`, `linux`. Scripts also pin that classifier as a Maven property for transitive OpenJFX descriptors. Wrong-platform libraries cause failure before publication.

## Intel Mac: build and check

Run in the repository root on a native Intel Mac with JDK 21 installed:

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
uname -m
"$JAVA_HOME/bin/java" -version
mkdir -p .tools/tmp .tools/logs
./mvnw -Pmac-x86_64 -Dinventory.build.directory="$PWD/InventorySystemi/target/build-macos-x86_64" clean test
bash InventorySystemi/scripts/package-macos.sh x86_64
file InventorySystemi/dist/macos/InventorySystem-1.3.0-macos-x86_64.app/Contents/MacOS/InventorySystem
lipo -info InventorySystemi/dist/macos/InventorySystem-1.3.0-macos-x86_64.app/Contents/MacOS/InventorySystem
python3 InventorySystemi/scripts/verify-macos-package.py --arch x86_64
```

`uname -m` and `lipo` must show x86_64; the JDK must be 21 and x86_64. The verifier uses disposable project-local data and denies process network access; it is not a replacement for the manual acceptance checklist below. Its result is `InventorySystemi/target/packaged-macos-verification.json`; retain it with the target's build log.

For Apple Silicon, substitute `mac-aarch64`, `build-macos-arm64`, `arm64` and `macos-arm64`. Never build both targets concurrently in the same checkout. Use separate checkouts for concurrent jobs.

## Linux x86_64: build, extract and check

With a complete JDK 21 selected on a graphical Linux x86_64 machine, run from the repository root:

```sh
export JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
uname -m
"$JAVA_HOME/bin/java" -version
mkdir -p .tools/tmp .tools/logs
./mvnw -Plinux -Dinventory.build.directory="$PWD/InventorySystemi/target/build-linux-x86_64" clean test
bash InventorySystemi/scripts/package-linux.sh
mkdir -p InventorySystemi/target/linux-manual-check
tar -xzf InventorySystemi/dist/linux/InventorySystem-1.3.0-linux-x86_64.tar.gz -C InventorySystemi/target/linux-manual-check
file InventorySystemi/target/linux-manual-check/InventorySystem/bin/InventorySystem
```

The launcher must report **ELF 64-bit … x86-64**. To test without opening an existing shop, launch with a disposable home and explicit DB override (from the same shell):

```sh
mkdir -p "$PWD/InventorySystemi/target/linux-manual-home" "$PWD/.tools/tmp"
DB_URL="jdbc:h2:file:$PWD/InventorySystemi/target/linux-manual-home/inventory;DATABASE_TO_LOWER=TRUE" \
DB_USER=sa DB_PASSWORD= \
JAVA_TOOL_OPTIONS="-Duser.home=$PWD/InventorySystemi/target/linux-manual-home -Djava.io.tmpdir=$PWD/.tools/tmp" \
InventorySystemi/target/linux-manual-check/InventorySystem/launch.sh
```

Do not run the packaging cleanup over your disposable manual database afterward: preserve it or move it intentionally yourself. For ordinary installation, extract the archive to its permanent location and run `./launch.sh`; optionally run `./install-desktop.sh` to register a menu entry. The launcher finds bundled Java/JavaFX relative to itself; it does not depend on an installed JDK. Standard GTK 3, X11/XWayland, font and graphics libraries must be available. Distribution-specific acceptance remains required.

## Manual acceptance on each new machine

1. Use an empty disposable home/database; preserve any real shop data.
2. Launch the packaged application. Complete terms acceptance, administrator signup, recovery-code acknowledgment and currency selection; verify login.
3. Create a weight product, receive stock and sell `230 g`; check live calculation, remaining stock and the receipt.
4. Create a backup and restore it to another disposable location; verify accounts, sale and stock.
5. Close the app, turn Wi-Fi off on that test machine, relaunch and repeat a sale/receipt/backup. Re-enable Wi-Fi afterward if desired.
6. Record OS version, CPU, JDK/build log and actual results in VERIFICATION.md. Only then mark that platform execution-tested.

Intel and Linux have **not** passed this checklist here. No Intel/Linux native binary was generated on the arm64 Mac, so no file/lipo output for those artifacts is claimed. Windows retains its previous structural-only status.

## Target isolation, checks and output

- Mac output: `InventorySystemi/dist/macos/InventorySystem-1.3.0-macos-arm64.app/.dmg` and, on an Intel host, separately `InventorySystem-1.3.0-macos-x86_64.app/.dmg`.
- Linux output: `InventorySystemi/dist/linux/InventorySystem-1.3.0-linux-x86_64.tar.gz`.
- Windows output stays `InventorySystemi/dist/windows/InventorySystem-Windows-x64.zip`. Its script and application Java were unchanged, so it was not rebuilt.
- Each target has its own `target/build-<target>` and `target/release-<target>` staging folders. Scripts rerun tests; they never run a shared `mvn clean`. An explicit root `./mvnw clean` still clears Maven's default target folder; use the isolated commands above when other staging outputs exist.
- Build logs: `InventorySystemi/target/logs/packaging-<target>.log`, including JDK path/version and host/JDK architecture. The scripts inspect launcher `file`/`lipo`, runtime native headers and native libraries inside every dependency JAR. All three OpenJFX components must have the expected classifier and no foreign classifier.
- Mac app signature and DMG integrity are checked before publication. Linux archives are extracted again and the extracted image is checked before publication. Architecture evidence is embedded in the app as `architecture-check.json`.
- Only after successful tests and checks are older **same-target** artifacts replaced. The original unlabelled Mac app is removed only after verifying it is arm64. No other architecture, Windows output or directory outside project `dist/` and `target/` is deleted. Deleted paths are recorded in `docs/evidence/packaging-deleted-files.txt`.
- `app-metadata.properties` remains the source of version/vendor/product identity. No application source, schema, data migration or native package icon was changed in this packaging-only update. Existing `~/.inventorysystem`, Downloads and /Applications are untouched.

Build-tool checks:

```sh
bash -n InventorySystemi/scripts/package-common.sh InventorySystemi/scripts/package-macos.sh InventorySystemi/scripts/package-linux.sh
python3 -m unittest discover -s InventorySystemi/scripts -p test_package_target.py
```
