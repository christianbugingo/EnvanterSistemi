# Verification — 6 November 2026, Inventory System 1.3.0

Host: Apple Silicon macOS, JDK 21, JavaFX 21.0.6. Tests and package checks use disposable project-local homes/databases. The live ~/.inventorysystem database, receipts, logs and backups were not opened or modified. Downloads and /Applications were not touched.

| Check | Result |
|---|---|
| `./mvnw clean test` | **49 discovered; 48 passed, one opt-in desktop test skipped; zero failures/errors.** |
| Separate desktop test | **Passed** using real JavaFX scenes/event dispatch (TestFX equivalent). Welcome page, declining terms without creating an account, required acceptance, admin creation, mandatory recovery-code acknowledgment, searchable RWF selection, login, password change, offline recovery, forced temporary-password replacement, weight product, multi-piece/230 g sale, completion without amount entry, and About acceptance date/contact. English and Turkish are exercised. Fresh shops reach signup before login, currency can be typed in one dropdown, legal headings render without Markdown markers, and the home page has no email/website. |
| UI exception check | Final UI run has no uncaught application exception. The known JavaFX unnamed-module configuration warning remains. The login timer now stops when its window is hidden. |
| Currency and money | Bundled 178-entry ISO catalogue loads, with 165 priceable currencies; RWF/JPY 0, USD 2, KWD 3; stable grouping and RWF formatting; HALF_UP rounding; old currency/tax snapshots survive settings changes; unlike currencies are never added together. First currency selection rounds pre-existing product prices. |
| Tax | Zero/nonzero rate, range/precision validation, one calculation per checkout, three-decimal currency, unit-separated report allocation, original receipt preservation and post-void remaining tax. |
| Weight/stock | Flexible kg/g parsing, exact integer grams, normalized display, whole pieces, and live/persisted totals use the same calculation. At 100 RWF/kg, 200 g = 20 RWF and 230 g = 23 RWF. Equivalent kg/g inputs, 5 g rounding in 0/2/3-decimal currencies, arbitrary quantities, historical weight-price snapshots, oversell and concurrent checkout protection are tested. |
| Passwords | BCrypt cost 12, salts, legacy PBKDF2/plaintext upgrade compatibility, no plaintext copied into history on legacy staff reset, policy/history, one-time/replaced recovery codes, failed-attempt audit and exact five-minute timing using a controlled clock. |
| Legal | Setup/login service gate rejects unaccepted terms. UI cannot accept until checked; declining creates no account. Version, content digest and date are stored; About shows acceptance. Full terms/privacy/notices are bundled locally. Legal validity is not certified. |
| Migration and restore | Empty DB and a file copy generated from the actual 1.2.1 schema, preserving account/product/sale/payment values. Integer conversion, a historical 230 g sale, restart idempotence, interrupted conversion, automatic backup restore and backup failure before schema changes. Global money/currency/tax/legal migration runs as part of initialization. |
| Transactions and roles | Forced receipt-record failure rolls back stock, sale lines, checkout/payment and audit. Retry protection, price snapshots, unit-history protection, reasoned one-time voids, admin/staff boundaries including financial settings, reset/disable and backup denial. |
| Translations | English literal UI keys are required. Turkish missing keys are reported from the raw translation file as warnings and use English fallback; they do not fail the build. |

## Packages and offline checks

The final release targets are Mac Apple Silicon `.app`/`.dmg` and a Windows x64 portable ZIP. Native Mac metadata and Windows PE version resources are checked against app-metadata.properties, including COODIC LABS, product/version/copyright and bundle identifier com.coodiclabs.inventorysystem. Both distributions include legal texts and third-party license files. Final verification passed: Application class files match across platforms; Mac metadata, ad-hoc signature, Windows native version resource, ZIP CRC, classpath and legal resources passed. A structural check compares application classes and verifies Windows ZIP CRC, native x64 architecture, libraries, classpath entries, legal resources and absence of shop data.

The Mac package check uses a disposable copy of the native launcher and bundled runtime. `/usr/bin/sandbox-exec` denies all process network access while it starts and creates an administrator/product, records a weighted sale, generates a receipt, creates a backup and restores it. System Wi-Fi settings are left unchanged to respect the project-only restriction. The final native startup, sale, receipt, backup and restore checks passed; results are recorded in `docs/evidence/mac-offline.json` and `package-structure.json` for the delivered build.

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
