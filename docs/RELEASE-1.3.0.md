# Inventory System 1.3.0 — local arm64 release

Developer: **COODIC LABS**, Rwanda. Contact: tresorbi29@gmail.com. Website: https://coodic.org/. Proprietary code, all rights reserved.

- Version: **1.3.0**
- Build date (UTC, embedded in the application): **2026-11-06T12:20:10Z**
- Architecture: **macOS arm64 / Apple Silicon**
- Apple notarization submission: **574c51af-bf90-46f7-8be8-84d00e28afb6**
- Notarization / Gatekeeper result: **Accepted; app and DMG accepted as Notarized Developer ID**
- SHA-256 of the final stapled DMG: `1a319414f8290f2c2858a16f2aaaf62620e8d2584554e0ae32f661775123fdb9`

Only Apple notarization was authorized as an upload. No GitHub Release, website page, distribution upload, Git push or public download was created. “Release” here means locally prepared artifacts for the owner to publish manually.

## Local artifacts

- `/Users/a/Documents/EnvanterSistemi/InventorySystemi/dist/macos/InventorySystem-1.3.0-macos-arm64.app`
- `/Users/a/Documents/EnvanterSistemi/InventorySystemi/dist/macos/InventorySystem-1.3.0-macos-arm64.dmg`
- `/Users/a/Documents/EnvanterSistemi/InventorySystemi/dist/macos/InventorySystem-1.3.0-macos-arm64.dmg.sha256`

The sidecar contains the final DMG checksum and its basename. Verify from the repository root:

```sh
cd /Users/a/Documents/EnvanterSistemi/InventorySystemi/dist/macos
shasum -a 256 -c InventorySystem-1.3.0-macos-arm64.dmg.sha256
```

Expected: `InventorySystem-1.3.0-macos-arm64.dmg: OK`. Apple's submission hash is for the upload before stapling; use the sidecar for the delivered, stapled file.

The previous signed app/DMG is moved only after every new check passes to **`/Users/a/Documents/EnvanterSistemi/docs/evidence/previous-release/20261106T122451Z-94ce9ca2`**. It is preserved, not deleted. Earlier generated evidence and disposable test databases were also preserved before the required Maven clean. [Movement records](evidence/release-1.3.0/initial-target-moves.json) and [local installation record](evidence/release-1.3.0/local-release-install.json) list individual paths and hashes. Source/release deletion list: none; automated tests clean only their own disposable fixtures.

## Features and testing

Most requested features already existed and were reverified; this refresh adds consistent currency symbols to amounts/reloaded sales, guarded staging and archived replacement, the checksum sidecar and these release instructions.

| Feature | Verified scope |
|---|---|
| Currency | Bundled ISO catalogue; RWF/JPY 0, USD 2, KWD 3 minor units; first-run selection, admin-only changes, original currency/precision snapshots. Distinct symbols are displayed (`$ 12.50 USD`); RWF remains `1,250 RWF` because the bundled symbol equals its code. |
| Tax | Default 0, range 0–100, four decimals; `00` stores zero; visible hint; tax only above zero on receipts; original rate snapshots. |
| Weight | Exact integer grams; kg/g parsing; live BigDecimal pricing and HALF_UP precision. 0.2 kg at 100/kg = 20; 0.23 kg = 23; equivalent 230 g; 5 g currency rounding; stripped kg zeros; whole sales 1–23 kg. |
| Sales and stock | Merged name/barcode input; piece/weight flows; FIFO batches; duplicate detection; immutable costs/prices; no negative stock; transaction rollback and reasoned void restoration. Biscuit example produces COGS 500 then 1005, gross profit 745 and remaining five from batch two. |
| Dashboards/profit | JavaFX KPI/Pie/Bar/Line charts, vector branding, staff-scoped sales, admin-controlled staff profit (default hidden), all five profit periods, unknown-cost disclosure. Low-stock describes shared shop inventory; sales/profit remain scoped. |
| Legal/setup | Bundled COODIC LABS legal files, mandatory acceptance gate, About, admin/recovery/currency setup. Legal documents were retained unchanged. |
| Database | Empty schema, copied 1.2.1 fixture preserving products/sales and opening unknown-cost batches, restart idempotence, backup and restore. Fixture data is generated from the original schema; no live customer database was opened. |

Test commands and results:

- `./mvnw clean test`: **60 discovered, 59 passed, 1 opt-in desktop test skipped; zero failures/errors**.
- Separate real-JavaFX desktop test: **1 passed**. First-run terms/admin/recovery/currency, login/password flows, merged search, weight preview/sale and admin/staff dashboards were exercised.
- Packaging/archiving regression tests: **13 passed**, including refusal before acceptance/offline checks, changed-DMG refusal, preservation of other targets, rollback on failed install and previous-release archival.
- Arm64 staging build reran the Java tests successfully; native architecture/classifier checks passed.
- Developer ID signing, secure timestamps, hardened runtime and exactly three JVM entitlements; signed inner code including JAR natives; strict app signature checks.
- Apple accepted the submission on the first attempt in this task, with no issues. App and DMG tickets stapled/validated, Gatekeeper accepted both, and the unmodified app copied from the mounted DMG completed network-denied startup plus packaged service setup/weighted-sale/receipt/profit/backup/restore checks. Final post-install signatures and checksum passed.

Full Part B command output: [arm64 build](evidence/release-1.3.0/arm64-build.log), [signing/notarization workflow](evidence/release-1.3.0/signing-workflow.log), [submission](evidence/release-1.3.0/submission.json), [Apple log](evidence/release-1.3.0/notary-log.json), [offline workflow](evidence/release-1.3.0/signed-offline.json), [final command checks](evidence/release-1.3.0/final-checks.log). Files, artifacts, hashes and movements: [full manifest](evidence/release-1.3.0/files-artifacts-and-movements.json).

## Not verified / not updated

- Intel macOS: **not updated for this release; not yet released**, not built/signed/notarized/tested here.
- Linux x86_64: **not updated for this release; not yet released**, not built/signed/notarized/tested here.
- Windows ZIP: **not updated for this release; not yet released**, left byte-for-byte unchanged; no build/sign/notarization/execution test in this task.
- Actual browser-download quarantine and first launch on a clean Mac are **not tested**. Previous developer-machine trust may affect local results.
- Other Macs, other macOS versions, physical Wi-Fi-off acceptance, scanners/scales/printers and prolonged shop use are not verified.
- The packaged offline test starts the unchanged signed app and uses an external non-shipped Java agent for service flows. Desktop UI flows are tested separately with real JavaFX; this does not replace human acceptance of the downloaded package.

## Owner-run quarantine test (no protection bypass)

Use a separate Apple Silicon test Mac or fresh macOS test account when possible. Keep Gatekeeper enabled. Do not remove quarantine attributes or use Open Anyway to make a failed test pass. Use a copy and disposable data, leaving the original release files and your live shop untouched.

### Local quarantine simulation before any upload

Run these commands yourself from the repository root on a Mac whose `open -h` lists `--env` (the current host does). This sets quarantine on copies only; it does not claim a real website download:

```sh
cd /Users/a/Documents/EnvanterSistemi
quarantine_run="$PWD/InventorySystemi/target/quarantine-test-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$quarantine_run/home/shop" "$quarantine_run/tmp" "$quarantine_run/mount"
cp InventorySystemi/dist/macos/InventorySystem-1.3.0-macos-arm64.dmg "$quarantine_run/"
cp InventorySystemi/dist/macos/InventorySystem-1.3.0-macos-arm64.dmg.sha256 "$quarantine_run/"
(cd "$quarantine_run" && shasum -a 256 -c InventorySystem-1.3.0-macos-arm64.dmg.sha256)
quarantine_stamp=$(printf '%x' "$(date +%s)")
xattr -w com.apple.quarantine "0083;$quarantine_stamp;LocalReleaseTest;" "$quarantine_run/InventorySystem-1.3.0-macos-arm64.dmg"
hdiutil attach -readonly -nobrowse -noautoopen -mountpoint "$quarantine_run/mount" "$quarantine_run/InventorySystem-1.3.0-macos-arm64.dmg"
ditto "$quarantine_run/mount/InventorySystem.app" "$quarantine_run/InventorySystem.app"
hdiutil detach "$quarantine_run/mount"
xattr -wr com.apple.quarantine "0083;$quarantine_stamp;LocalReleaseTest;" "$quarantine_run/InventorySystem.app"
xattr -p com.apple.quarantine "$quarantine_run/InventorySystem.app"
codesign --verify --deep --strict "$quarantine_run/InventorySystem.app"
spctl --assess --type execute -v "$quarantine_run/InventorySystem.app"
open -n -a "$quarantine_run/InventorySystem.app" \
  --env "JAVA_TOOL_OPTIONS=-Duser.home=$quarantine_run/home -Djava.io.tmpdir=$quarantine_run/tmp" \
  --env "DB_URL=jdbc:h2:file:$quarantine_run/home/shop/inventory;DATABASE_TO_LOWER=TRUE" \
  --env "DB_USER=sa" --env "DB_PASSWORD="
```

This launch uses Launch Services to exercise normal quarantine handling while passing the disposable data location. Expect Gatekeeper assessment `accepted / source=Notarized Developer ID`. A standard first-open confirmation may appear; an unidentified-developer/damaged-app block is a failed test. Verify About shows the disposable database path before entering any data. On Macs without `open --env`, use a fresh macOS test account and Finder launch rather than your real shop account.

Complete terms acceptance, create your own administrator, save the recovery code privately, choose currency/tax and sign in. Create a weight product with zero stock, receive stock with a recorded purchase cost, sell 230 g and inspect its receipt and Profit and Loss. Back up to the quarantine-run folder. Quit, turn Wi-Fi off, relaunch with the same `open` command and repeat a sale/receipt/backup. Record Mac model/OS, quarantine attribute, prompt/result and About path. Keep your test data until verified; do not run Maven clean over it unintentionally.

### Actual browser download test after your own upload

Only after you choose to publish: use Safari on a clean test Mac/account to download the exact DMG and sidecar, selecting a dedicated test folder rather than Downloads. Verify the checksum there, confirm `xattr -p com.apple.quarantine <DOWNLOADED_DMG>` is present, mount and copy the app into a test folder rather than /Applications, then use the disposable-data launch above (or Finder in a fresh test account). Repeat first-run, weighted sale, receipt, profit, backup and Wi-Fi-off checks. This real-download result must be recorded separately from a manually applied quarantine flag. [Apple's Developer ID testing guidance](https://help.apple.com/xcode/mac/current/en.lproj/dev1cc22a95c.html) explains why a quarantined copy on a non-development Mac matters.

## Owner-only manual upload

No upload below has been performed. You decide when to publish.

1. Verify the local sidecar immediately before upload. Keep the DMG unchanged after stapling/checksum creation.
2. Ensure the repository commit/tag you choose contains this release's source and docs; this task did not push commits or tags.
3. In your GitHub repository, open Releases → Draft a new release. Select/create `v1.3.0` on the intended release commit; do not replace an existing tag blindly.
4. Set the title to “Inventory System 1.3.0 — macOS Apple Silicon”. Use the feature/test/not-tested notes above. Attach **only** `InventorySystem-1.3.0-macos-arm64.dmg` and `InventorySystem-1.3.0-macos-arm64.dmg.sha256` from local dist/macos/. Exclude previous-release/, staged candidates, test data, .env, credentials and historical Windows/Intel/Linux builds.
5. Save as draft and verify both filenames, sizes and the checksum text. Publish only when you choose. The repository's visibility determines public/private access.
6. Download both uploaded assets yourself to the test folder, run `shasum -a 256 -c ...sha256` there, and perform the browser-quarantine test above. For a website instead, upload those same two files using your own hosting controls, keep the bytes unchanged and link both files with the checksum visible.

[GitHub's release documentation](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository) describes drafts and binary attachments. No GitHub Release, draft, website page or artifact upload was created here.
