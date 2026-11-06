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

## Developer ID signing and notarization (arm64, outside the Mac App Store)

Use a native Apple Silicon Mac with the valid Developer ID Application identity and a working notarytool profile already present in Keychain. Never put certificates, private keys, account credentials or passwords into the project, environment files, command history or logs. The environment variables below contain only a public identity label and the name of an existing Keychain profile; no Apple ID/password is accepted by this workflow.

From the repository root, using your own values in place of the placeholders:

```sh
security find-identity -v -p codesigning
xcrun notarytool history --keychain-profile "<EXISTING_KEYCHAIN_PROFILE>"
export MAC_SIGN_IDENTITY="<DEVELOPER_ID_APPLICATION_IDENTITY>"
export MAC_NOTARY_PROFILE="<EXISTING_KEYCHAIN_PROFILE>"
bash InventorySystemi/scripts/sign-and-notarize-macos.sh
```

Do not enable shell tracing (`set -x`). The script fails before staging if the identity/profile/host/JDK check fails. It does not create or export Keychain credentials. Notarization uploads the staged app/DMG to Apple, and secure timestamping also requires network access on the build machine. These are distribution operations only; no network call, updater, telemetry or online dependency is added to the installed application.

For a new release, first run `bash InventorySystemi/scripts/package-macos.sh arm64 --stage-only` and set `MAC_SIGN_SOURCE_APP` to the resulting absolute `.app` path under `InventorySystemi/target/release-macos-arm64/`. This builds fresh application code without replacing dist/. The existing arm64 app/DMG stays in dist/ while a unique `InventorySystemi/target/notarize-arm64-*` candidate is signed and checked. Failed attempts retain their candidate and evidence for diagnosis and do not replace the existing release. No other platform's artifacts are touched. This workflow re-signs the already-tested arm64 app rather than rebuilding or changing Java application code.

### Entitlements and signing order

`InventorySystemi/packaging/macos/entitlements.plist` contains exactly these three requested JVM hardened-runtime compatibility exceptions:

| Entitlement | Purpose in this Java/JNI distribution |
|---|---|
| `com.apple.security.cs.allow-jit` | Allows HotSpot's just-in-time compiler to create executable code using MAP_JIT. |
| `com.apple.security.cs.allow-unsigned-executable-memory` | Allows JVM/native-generated executable-memory paths that do not use MAP_JIT. This is a broader JVM compatibility exception; it is not an App Sandbox permission. |
| `com.apple.security.cs.disable-library-validation` | Allows JVM/JavaFX JNI loading, including extracted third-party native resources, without requiring every loaded library to share the host's signing identity. Bundled native code is still individually signed and verified. |

There is no App Sandbox, network entitlement, debugger entitlement or `get-task-allow`. These exceptions do not add application networking. They are applied to executables/the outer application; libraries are signed without executable entitlements.

`sign_macos_components.py` signs arm64 native members extracted from dependency JARs, repacks those JARs preserving non-native members, signs standalone Mach-O executables/libraries, seals nested runtime bundles, then signs the outer app. Every native signature uses `--options runtime --timestamp`; executables additionally use `--entitlements`. It refuses foreign native code or an already JAR-signed dependency whose Java signature would be invalidated. It never uses `codesign --deep` to perform signing; `--deep` is used for verification.

### Submission, rejection and final checks

The driver verifies the app with `codesign --verify --deep --strict --verbose=2`, creates `InventorySystem-1.3.0-macos-arm64.dmg` with `hdiutil`, signs the DMG with Developer ID and a secure timestamp, then submits:

```sh
xcrun notarytool submit "<STAGED_DMG>" --keychain-profile "<EXISTING_KEYCHAIN_PROFILE>" --wait --output-format json
xcrun notarytool log "<SUBMISSION_ID>" --keychain-profile "<EXISTING_KEYCHAIN_PROFILE>" "<STAGED_NOTARY_LOG_JSON>"
```

The DMG is deliberately built with `hdiutil`, not the `jpackage` DMG bundler: this JDK’s bundler re-signs its internal app copy ad hoc and strips the Developer ID timestamp/runtime settings. A pre-upload check mounts the image, compares all app contents byte-for-byte, verifies its signature and requires Developer ID, timestamp and hardened runtime. This catches container creation changing signatures before another submission.

Each attempt saves the submission ID/status and Apple's issue log in its staging evidence directory. If rejected, it prints the issues and exits without publishing. Fix the reported paths/signatures/entitlements, then rerun for a new submission; never treat an upload as acceptance. No automatic resubmission loop masks failures.

After acceptance it staples and validates the DMG ticket. It also staples/validates the separately distributed app, then runs:

```sh
xcrun stapler staple "<STAGED_DMG>"
xcrun stapler validate "<STAGED_DMG>"
spctl --assess --type open --context context:primary-signature -v "<STAGED_DMG>"
spctl --assess --type execute -v "<STAGED_APP>"
```

`verify-signed-macos.py` mounts the actual DMG read-only inside project target/, copies its app to another project-local temporary folder, verifies its signature and Gatekeeper assessment, and launches its unmodified native executable with process network access denied and a disposable home/database. It then uses an external, non-shipped Java verification agent (`SignedOfflineSmoke.java`) to exercise packaged services: terms, admin/recovery setup, currency, login, stock receipt, weighted sale, receipt, FIFO profit, backup and restore. The agent creates an ephemeral test password in memory and does not print/store it. No signed app file/configuration is altered; signature verification is repeated afterward. The agent and all test data remain outside the app and are never shipped. The DMG is detached even on test failure.

Only when every check passes does the local installer move the original arm64 app/DMG (and any checksum) into `docs/evidence/previous-release/<timestamp>/`, then install the new arm64 app/DMG and checksum sidecar. A failed install rolls back to the previous release; no distribution upload is performed. Full command output, submission metadata and offline results are copied to `docs/evidence/notarization/`; staging evidence/data remains under target/ for inspection. Physical Wi-Fi is not changed: the verifier denies network access to the launched processes. Manual Wi-Fi-off testing and fresh-machine acceptance remain separate checks. No /Applications, Downloads or live ~/.inventorysystem data is opened or modified; no Mac App Store submission is attempted.

References: [Apple: distribution signing](https://developer.apple.com/documentation/xcode/creating-distribution-signed-code-for-the-mac/), [Apple: notarization](https://developer.apple.com/documentation/security/notarizing-macos-software-before-distribution), [Oracle: Java 21 jpackage guide](https://docs.oracle.com/en/java/javase/21/jpackage/packaging-tool-user-guide.pdf).

## Current local-only release policy

Only the Apple Silicon arm64 DMG is prepared for release 1.3.0. Intel macOS, Linux and the historical Windows ZIP are **not updated for this release** and are **not yet released**. Do not run their builders as part of this release. Apple notarization is the sole authorized upload; GitHub/website/public distribution remains a manual owner action.

The final checksum is at `dist/macos/InventorySystem-1.3.0-macos-arm64.dmg.sha256` relative to InventorySystemi. Check it from that directory using `shasum -a 256 -c InventorySystem-1.3.0-macos-arm64.dmg.sha256`. The hash describes the final stapled DMG, so it differs from Apple's pre-stapling upload hash. See [RELEASE-1.3.0.md](RELEASE-1.3.0.md) for the tested build, exact quarantine steps, manual upload steps, and file/artifact/movement reports.
