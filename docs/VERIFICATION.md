# Verification — 3 November 2026, offline version 1.2.1

Host: Apple Silicon macOS, Corretto JDK 21, JavaFX 21.

| Check | Result |
|---|---|
| Clean build and service/unit tests | 24 passed; no failures/errors. The opt-in desktop test is skipped in the normal run. |
| Desktop test | Passed separately in English/Turkish: role switching, required password change, barcode cart entry, weighted quantities and cash input. |
| Offline database workflows | Tested checkout retry protection, cash/card separation, change, insufficient cash, shift opening/closing/discrepancies, single active shift, closed-shift void prevention and retained sale history. |
| Backup and restore | A persistent temporary H2 database was backed up and restored; login, product and stock data survived. |
| Mac release | Native .app and DMG built; final app launched using its bundled runtime and disposable data without startup errors. |
| Windows portable release | ZIP built with InventorySystem.exe, Windows x64 Java runtime and Windows JavaFX. ZIP CRC, launcher/classpath paths, PE x64 architecture, DLL architecture, module resolution and matching Mac/Windows app-JAR hashes checked. |
| Old release cleanup | Only dist/macos and dist/windows remain as application release folders; earlier generated app caches removed. Shop data was not removed or used for these checks. |

The Windows executable has **not** been run on Windows. Test it there using the checklist in [SHOP_SETUP.md](SHOP_SETUP.md). The release is a portable ZIP, not a tested MSI installer. The app's offline database restriction is tested before any remote JDBC connection can occur; physically disconnect networking during final acceptance on each laptop as an additional deployment check.

The desktop test logs JavaFX's classpath/unnamed-module warning; it did not log an application exception. Barcode input was simulated as keyboard text plus Enter; physical scanner and scale hardware were not tested. Card selection records an externally completed payment and does not process a charge. Native app signing/notarization is not configured.

The GUI test normally writes target/ui-previews. Previews retained during packaging are in the ignored .tools/ui-previews-1.2 folder at the repository root.

Branding update: the supplied PNG is included byte-for-byte in the JAR. The English/Turkish desktop check verified that the stage icon loads and captured the branded login/dashboard. The Mac bundle's ICNS matches the generated logo asset. The Windows native launcher receives six icon sizes; its previous code sections and non-icon resources are verified unchanged after resource embedding. Version 1.2.1 package validation includes the original image, native icons and matching application JARs. Windows execution remains untested.

Latest branding previews are in `.tools/ui-previews-1.2.1` at the repository root.
