# Verification — 6 November 2026, Inventory System 1.3.0

Host: Apple Silicon macOS, JDK 21, JavaFX 21.0.6. Tests and package checks use disposable project-local homes/databases. The live ~/.inventorysystem database, receipts, logs and backups were not opened or modified. Downloads and /Applications were not touched.

| Check | Result |
|---|---|
| `./mvnw clean test` | **59 discovered; 58 passed, one opt-in desktop test skipped; zero failures/errors.** |
| Separate desktop test (prior dashboard refresh) | **Passed** using real JavaFX scenes/event dispatch (TestFX equivalent). Welcome page, declining terms without creating an account, required acceptance, admin creation, mandatory recovery-code acknowledgment, searchable RWF selection, login, password change, offline recovery, forced temporary-password replacement, weight product, multi-piece/230 g sale, completion without amount entry, and About acceptance date/contact. English and Turkish are exercised. New checks cover the merged search filtering without changing typed text, barcode addition, focused weight entry, admin/staff Dashboard tabs, staff-scoped KPI data, hidden staff profit and the tax `00` hint. Fresh shops reach signup before login, currency can be typed in one dropdown, legal headings render without Markdown markers, and the home page has no email/website. |
| UI exception check | Final UI run has no uncaught application exception. The known JavaFX unnamed-module configuration warning remains. The login timer now stops when its window is hidden. |
| Currency and money | Bundled 178-entry ISO catalogue loads, with 165 priceable currencies; RWF/JPY 0, USD 2, KWD 3; stable grouping and RWF formatting; HALF_UP rounding; old currency/tax snapshots survive settings changes; unlike currencies are never added together. First currency selection rounds pre-existing product prices. |
| Tax | Zero/nonzero rate, range/precision validation, one calculation per checkout, three-decimal currency, unit-separated report allocation, original receipt preservation and post-void remaining tax. |
| Weight/stock | Flexible kg/g parsing, exact integer grams, normalized display, whole pieces, trailing-zero-free kg display (`19 kg`, `19.5 kg`, `1.25 kg`, `230 g`), every sale weight from 1 through 23 kg, and live/persisted totals use the same calculation. At 100 RWF/kg, 200 g = 20 RWF and 230 g = 23 RWF. Equivalent kg/g inputs, 5 g rounding in 0/2/3-decimal currencies, arbitrary quantities, historical weight-price snapshots, oversell and concurrent checkout protection are tested. |
| Passwords | BCrypt cost 12, salts, legacy PBKDF2/plaintext upgrade compatibility, no plaintext copied into history on legacy staff reset, policy/history, one-time/replaced recovery codes, failed-attempt audit and exact five-minute timing using a controlled clock. |
| Legal | Setup/login service gate rejects unaccepted terms. UI cannot accept until checked; declining creates no account. Version, content digest and date are stored; About shows acceptance. Full terms/privacy/notices are bundled locally. Legal validity is not certified. |
| Migration and restore | Empty DB and a file copy generated from the actual 1.2.1 schema, preserving account/product/sale/payment values. Integer conversion, a historical 230 g sale, restart idempotence, interrupted conversion, automatic backup restore and backup failure before schema changes. Global money/currency/tax/legal and FIFO opening-batch migrations run as part of initialization. Both a copied 1.2.1 schema/database and an existing 1.3.0 global schema retain data and gain exactly one unknown-cost opening batch per positive-stock product; restart is idempotent. The historical fixture is generated from the original schema, not a customer database. |
| Transactions and roles | Forced receipt-record failure rolls back stock, sale lines, checkout/payment and audit. Retry protection, price snapshots, unit-history protection, reasoned one-time voids, admin/staff boundaries including financial settings, reset/disable and backup denial. |
| Batches and profit | Biscuit example: sell 5 then 10 from batches 10/1000 and 10/1010 RWF; COGS 500 and 1005, remaining 5 from batch 2, gross 745 and margin 33.11%. Partial allocation conserves total purchase cost; void restores exact original batches once. Receipt failure rolls back costs/allocations too. Optional prices create separate historical lines, receipt unit prices match those lines and survive default price edits. Foreign-currency costs stay unavailable and old-currency batch price overrides are not reused. Unknown costs mark profit incomplete. Duplicate name/unit rejection, Today/Week/Month/Year/Custom ranges, zero-revenue margin, staff opt-in and user-scoped profit are covered. |
| Translations | English literal UI keys are required. Turkish missing keys are reported from the raw translation file as warnings and use English fallback; they do not fail the build. |

## Current packaging targets — 6 November 2026

| Target | Artifact action | Execution and architecture status |
|---|---|---|
| macOS arm64 | Rebuilt because the Mac script and Maven target configuration changed; old unlabelled arm64 artifacts replaced only after tests and native/signature/DMG checks passed. | Native launcher reports Mach-O arm64; lipo reports arm64; 41 native files/libraries checked with no foreign architecture. New package passed native startup and process-network-denied setup/login/stock receipt/weighted sale/FIFO profit/receipt/backup/restore on this host. |
| macOS x86_64 | **Not built**: native Intel Mac and x86_64 JDK unavailable. Script prepared. | **Not tested**; no binary exists, so no file/lipo result is claimed. Host guard refused Darwin arm64. Rosetta refusal has a regression test. |
| Linux x86_64 | **Not built**: Linux x86_64 host/JDK unavailable. Script prepared. | **Not tested** on Ubuntu/Debian or another distribution; no ELF artifact exists, so no file output is claimed. Host guard refused Darwin arm64. |
| Windows x64 | **Not rebuilt**: Windows script and application Java unchanged. | Existing ZIP SHA-256 is unchanged: `70d52d8467371506ea6b32eac52f17959913ba74c3f94f33e5bb2de6959348a5`. ZIP/native metadata/resources verified; all 64 Java class files match the rebuilt Mac. Windows execution remains untested. |

`./mvnw clean test`: **59 discovered, 58 passed, 1 opt-in desktop test skipped, no failures/errors**. The arm64 package's isolated Maven build reran the same tests successfully. Build-tool tests: **9 passed**, including host/JDK mismatches, Rosetta refusal, wrong native libraries/classifiers, same-target cleanup, data/symlink protection and incomplete-release preservation. Bash syntax checks passed for all three shell scripts. The separate desktop test in the earlier dashboard refresh passed; it was not rerun for this packaging-only change.

Current output: `InventorySystemi/dist/macos/InventorySystem-1.3.0-macos-arm64.app` and `.dmg`. Native output:

```text
InventorySystem-1.3.0-macos-arm64.app/Contents/MacOS/InventorySystem: Mach-O 64-bit executable arm64
Non-fat file: .../InventorySystem is architecture: arm64
```

JDK used: `/Users/a/Documents/EnvanterSistemi/.tools/jdk/Contents/Home`, Amazon Corretto **21.0.13**, JDK architecture `aarch64`, native host `Darwin arm64`. Full path/version/profile/native checks are in [evidence/packaging-macos-arm64.log](evidence/packaging-macos-arm64.log); direct file/lipo output is in [evidence/arm64-architecture.log](evidence/arm64-architecture.log). The embedded architecture JSON is structural evidence, not a runtime-test claim; the separate [offline workflow result](evidence/mac-offline.json) records actual execution.

New packages pending native hosts: `InventorySystem-1.3.0-macos-x86_64.app/.dmg` and `InventorySystem-1.3.0-linux-x86_64.tar.gz`. Both need real-machine launch, terms/admin/recovery/currency setup, login, weighted sale, receipt, backup/restore and Wi-Fi-off launch before execution support can be marked tested. Exact build and acceptance commands are in [PACKAGING.md](PACKAGING.md). No Intel/Linux output was relabelled or fabricated from arm64.

For this packaging-only change, no application Java, schema, migration, native icons or app metadata changed. No shop data was opened or deleted. Cleanup is restricted to matching target outputs/staging in project dist/ and target/; paths are in [evidence/packaging-deleted-files.txt](evidence/packaging-deleted-files.txt). File/artifact paths are in [evidence/packaging-deliverables.txt](evidence/packaging-deliverables.txt). Old cleanup and dashboard evidence files remain historical records.

## Packages and offline checks

Existing verified release artifacts are Mac Apple Silicon `.app`/`.dmg` and a Windows x64 portable ZIP. Native-host Intel Mac and Linux x86_64 builders are now provided, but those artifacts were not generated here. Native Mac metadata and Windows PE version resources are checked against app-metadata.properties, including COODIC LABS, product/version/copyright and bundle identifier com.coodiclabs.inventorysystem. Both distributions include legal texts and third-party license files. Final verification passed: Application class files match across platforms; Mac metadata, ad-hoc signature, Windows native version resource, ZIP CRC, classpath and legal resources passed. A structural check compares application classes and verifies Windows ZIP CRC, native x64 architecture, libraries, classpath entries, legal resources and absence of shop data.

The Mac package check uses a disposable copy of the native launcher and bundled runtime. `/usr/bin/sandbox-exec` denies all process network access while it starts and creates an administrator/product, receives stock, records a weighted sale, verifies FIFO gross profit, generates a receipt, creates a backup and verifies restored batch quantities. System Wi-Fi settings are left unchanged to respect the project-only restriction. The final native startup, sale, receipt, backup and restore checks passed; results are recorded in `docs/evidence/mac-offline.json` and `package-structure.json` for the delivered build.

Commands:

```
./mvnw clean test
./mvnw -Dinventory.guiTest=true -Dtest=DesktopSmokeTest test
python3 InventorySystemi/scripts/verify-macos-package.py
python3 InventorySystemi/scripts/verify-release.py
```

## Not verified / release limitations

- **Windows execution and offline sale/receipt/backup on a Windows PC have not been tested.** A Windows ZIP structural check cannot establish those results.
- **Physical Wi-Fi-off testing on the Mac has not been performed.** Network access is denied to the test processes instead; no system network settings are changed.
- macOS 11 minimum is from the [JavaFX 21 release information](https://openjfx.io/highlights/21/); this app has only been run on the current host, not every supported OS release. Windows 10/11 x64 are deployment targets requiring acceptance.
- Mac packages are ad-hoc signed after metadata changes, not Developer ID signed/notarized. Windows is not publisher-signed. Public installer trust/reputation is not verified.
- No physical scanner, scale, printer, fiscal device, long-running shop load or power-loss test.
- **Legal review is required before commercial release.** Complete `[GOVERNING COUNTRY — REQUIRED PLACEHOLDER; LAWYER REVIEW REQUIRED]` in TERMS.md. Counsel must review all marked terms, privacy/ownership representations, consumer rights, liability/warranty limits, tax wording and third-party redistribution/source-availability obligations. Actual contact details are filled; no email placeholder remains.

Cleanup paths are listed in DELETED_FILES.txt (distinct generated file paths, including build outputs removed during packaging). Final evidence is retained under docs/evidence and snapshots under .tools/ui-previews-1.3.0 before generated target output is removed. No claim of legal compliance or readiness on every computer is made.
