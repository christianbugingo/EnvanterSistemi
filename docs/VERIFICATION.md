# Verification — local Inventory System 1.3.0, 6 November 2026

Host: Apple Silicon macOS, Java 21 / JavaFX 21.0.6. The application remains fully offline. All tests used disposable project-local homes/databases; live ~/.inventorysystem, Downloads and /Applications were not modified. Only the explicitly authorized Apple notarization upload occurred. No GitHub Release/draft, website page, Git push or distribution upload was created.

## Current local release

Accepted submission: **574c51af-bf90-46f7-8be8-84d00e28afb6**, status code 0, Ready for distribution, no issues. This task required no resubmission. Previous signing attempts in NOTARIZATION.md are historical.

| Check | Result |
|---|---|
| `./mvnw clean test` | **60 discovered, 59 passed, one opt-in desktop test skipped; zero failures/errors.** |
| Separate desktop test | **1 passed**, real JavaFX scenes/event dispatch: terms gate, admin/recovery/currency setup, login/password flows, merged product search, weight preview/sale, admin/staff dashboards and About. English/Turkish exercised. |
| Packaging/release regression tests | **13 passed**: architecture/classifier guards, cleanup isolation, incomplete release preservation, acceptance/offline/hash gates, previous-release archival and rollback on failed install. |
| Currency | Bundled catalogue loads; RWF/JPY 0, USD 2, KWD 3 minor units. RWF stable formatting; distinct symbol display; reloaded original-currency symbols with original precision; admin-only settings and snapshot preservation. |
| Tax | `00` canonical zero; default/range/four-decimal validation, tax only above zero on receipts, once-per-checkout rounding, original snapshots, void/report allocation. |
| Weight | Integer grams; 0.2 kg at 100/kg = 20; 0.23 kg = 23; equivalent kg/g entry; 5 g rounding at 0/2/3 minor units; stripped kg zeros; every whole sale from 1–23 kg and arbitrary stock-limited amounts. |
| FIFO/profit | Biscuit costs 500 then 1005 from batches 10/1000 and 10/1010; five remain from batch two; gross profit 745 and margin 33.11%. Cost conservation, original batch restoration, rollback, price snapshots, unknown-cost disclosure, all five periods and staff opt-in/user scoping. |
| Migration/backup | Empty schema and copied 1.2.1 fixture retain products/sales, add unknown-cost opening batches and resume idempotently. Backup/restore and backup-failure safeguards passed. This is a schema-generated fixture, not a live customer shop. |
| Architecture | arm64 native launcher and matching JavaFX classifier; 41 native files/libraries inspected. JDK 21.0.13, aarch64. |
| Developer ID signing | 41 native items, including JAR natives, signed with hardened runtime and secure timestamps; executables use exactly three requested JVM entitlements. Inner code before bundles/outer app; strict verification passed. |
| DMG preservation | hdiutil-built DMG mounted before upload, compared byte-for-byte with the signed app; mounted app retains Developer ID, timestamp and hardened runtime. DMG signature and checksum valid. |
| Apple notarization | Accepted submission above; no new rejection/issues. |
| Stapling/Gatekeeper | App and DMG stapled/validated. Final installed artifacts both `accepted / source=Notarized Developer ID`; strict signature checks passed after installation. |
| Signed offline workflow | Actual DMG mounted read-only; copied app left unmodified; native startup, packaged terms/admin/recovery/currency/login, receive stock, 230 g sale, receipt, FIFO profit, backup and restore passed with process network denied. External verification agent is not shipped. |
| Local replacement/retention | Old signed release remained in dist/ until every check passed, then moved to docs/evidence/previous-release/ with its original hash intact. No source/release files deleted; prior generated evidence/test data moved intact before Maven clean. |
| Other platforms | Windows ZIP SHA-256 unchanged. **Windows, Intel macOS and Linux are not updated for this release and not yet released.** No build, signing, notarization or execution test of those targets occurred. Older cross-platform class-matching checks are historical and do not apply to this refreshed Java code. |

## Evidence and manual acceptance

[RELEASE-1.3.0.md](RELEASE-1.3.0.md) contains the actual build timestamp, submission ID, final stapled DMG checksum, exact quarantine commands and owner-only manual-upload instructions. [Current evidence](evidence/release-1.3.0/) includes command output, test logs, Apple JSON and every persistent moved file. The [full manifest](evidence/release-1.3.0/files-artifacts-and-movements.json) lists full paths/hashes, including archived/staged artifacts. Automatic test-fixture cleanup is distinct from project/release file deletion.

Not verified: physical Wi-Fi-off testing; a real browser download/quarantine launch; first-run interaction on a fresh downloaded package; other Macs/macOS versions; Intel, Linux and Windows execution; physical scanner/scale/printer; prolonged shop load/power-loss behavior. The developer-machine signed service workflow plus separate JavaFX UI test does not replace human acceptance of a quarantined download. Existing legal review markers and governing-law placeholder were retained unchanged.

No claim of support tested on every computer is made. To test quarantine, preserve Gatekeeper, use a copy and disposable shop, verify the database location in About, and follow RELEASE-1.3.0.md. Do not remove quarantine or bypass a failure to turn it into a pass. Only the owner may choose to upload the DMG and checksum later.
