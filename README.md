# Inventory System 1.3.0

**COODIC LABS** · Rwanda · tresorbi29@gmail.com · https://coodic.org/

An offline Java 21 / JavaFX application for local shop accounts, products, stock, sales, receipts, reports and backups. It has no network calls, cloud sync, analytics, remote database or payment processing. Currency and optional tax are chosen by the shop; no country's currency is selected by default. Original code is proprietary, all rights reserved. Third-party software retains its own licenses.

## Requirements and installation

- **macOS Apple Silicon (arm64):** macOS 11 or later (JavaFX 21 minimum; test on the intended OS version). Open `InventorySystemi/dist/macos/InventorySystem.dmg` and copy the app to Applications, or open the `.app` directly. Close older copies first. This build is not Developer ID signed/notarized; deployment on other Macs requires acceptance testing. Intel Mac and Linux packages are not provided.
- **Windows x64:** Windows 10/11 x64 deployment target; acceptance testing on the intended PC is required. Extract the **entire** `InventorySystemi/dist/windows/InventorySystem-Windows-x64.zip`, then open `InventorySystem/InventorySystem.exe`. Keep all bundled folders together. `Start InventorySystem.cmd` is a fallback and `Run with console.cmd` provides diagnostics. Windows execution has not been tested on this Mac.

Both packages include Java, JavaFX and local legal notices; no separate Java installation or internet is required for shop use. The welcome page explains the workflow and offers account creation on a fresh installation.

## First-run setup

1. Open the app. Choose **Create your administrator account** on the welcome page.
2. Read Terms and Privacy (including the full local documents), tick acceptance and continue. Declining prevents setup/login. The accepted version, document digest and date are stored locally. **These legal documents are a draft requiring lawyer review; the governing-law placeholder must be completed before commercial distribution.**
3. A fresh installation opens administrator signup before showing a login form. Create your administrator with your own username and password. No default credentials are seeded.
4. Save or print the recovery code immediately, then tick **I have saved this code**. It is shown once and stored only as a hash.
5. Type or select in the single searchable currency dropdown by code, name or symbol. Choose your currency explicitly. The bundled catalogue covers current ISO monetary/fund codes with defined minor units (178 ISO entries; 165 priceable entries in this build), using Java currency metadata and the ISO register. Non-monetary test/precious-metal codes without defined minor units are not selectable. RWF and JPY use 0 decimals, USD 2, KWD 3. Choose optional tax, default 0%, and language, default English.
6. Sign in and add products and staff. Existing installations without a currency must have an administrator choose it before selling. Existing administrators should generate/save a recovery code from File if they do not have one.

Each fresh computer starts an independent shop. Copying the application does not copy another shop's accounts or data. Admin and staff share data when using the same operating-system profile. Independent installations do not synchronize or merge.

## Shop settings, currencies and tax

Admin → File → **Admin settings** changes currency, tax and language. Changing currency requires confirmation: existing sales retain their original currency, precision and tax. Current product prices are interpreted in the new currency and rounded to its minor units; **no exchange conversion** occurs. Review prices before selling.

Amounts use consistent grouping and code suffixes, independent of UI language: `1,250 RWF`, `12.50 USD`, `1.234 KWD`. Names and symbols are shown in the currency chooser. Prices and line totals use BigDecimal and HALF_UP at the currency precision. Sales without a recorded currency from older versions remain `?`, with their historical two-decimal amounts; a new shop currency is never silently applied to them. Keep original records to identify those legacy amounts.

Tax rate accepts 0–100%, with up to four decimals. Tax is calculated **once per checkout**, after rounded line totals are summed, at that checkout's currency precision. With tax above zero, the screen, receipt and report show subtotal, tax and total; with zero, the receipt shows only total. Reports keep currencies separate. Tax across unit-separated report rows is allocated with rounding residue on the last unit; voided lines are excluded and remaining checkout tax is recomputed using its original rate. Original receipts retain their original amounts and mark voids.

## Products, sales and accounts

Choose **Sold by: Piece / Weight (kg)** first. Piece quantities are whole counts; weight stock and sales use integer grams. Price is per piece or kg using the shop currency precision. Product names, category and supplier are required; optional barcodes are unique. Existing stock changes use **Receive / adjust stock**, with a reason. Units cannot change after sales history exists; create another product instead.

New sale: search/select a product or scan a keyboard-style barcode ending with Enter. Piece scans add one piece; whole quantities such as 5 or 20 can be entered. Weight scans focus the weight prompt. Enter `0.23`, `0.23 kg` or `230 g`. Plain input in the kg field means kg: plain `230` means 230 kg. A live preview recalculates as you type: at 100 RWF/kg, 0.2 kg shows 200 g and 20 RWF; 0.23 kg or 230 g shows 23 RWF. A 5 g line is 1 RWF after HALF_UP rounding, or 0.50 in a two-decimal currency at 100/kg. Under 1000 g displays as grams; 1000 g and above as kg with three decimals.

Select Cash / Card / Other for reporting only and complete the sale. No amount received or change is requested. Stock, sale snapshots, payment type, checkout and receipt record commit together or all roll back. A local receipt file can be regenerated if writing it fails. Admin can void with confirmation and a reason: history/actor/reason are retained and stock restores once. Handle any real refund separately.

Admin creates staff in **Staff accounts**, with temporary passwords that must be changed at first login. Staff see their own sales; admin sees all activity and controls products, accounts, settings and backups.

Login-screen **Change password** asks for username/current/new/confirmation. Policy: at least 10 characters, one letter and one digit, different from username and recent passwords; maximum 72 UTF-8 bytes. BCrypt cost 12 protects passwords; older hashes upgrade on successful login. Five failed attempts lock an account for five minutes with a visible countdown; failures are audited.

**Forgot password:** staff ask their administrator for a temporary reset. Admin uses the saved recovery code, sets a temporary password, saves a replacement recovery code, then changes the temporary password at next login. Old recovery codes stop working. With both password and code lost, restore a usable backup; there is no bypass.

Shortcuts: Ctrl/Cmd+N new sale; Ctrl/Cmd+F search; Ctrl/Cmd+Enter complete sale. English is the default; missing optional Turkish translations fall back to English. View → About shows developer/contact, build, legal acceptance date, database location, full terms and the receipt notice.

## Data, backup, restore and uninstall

Default data: `~/.inventorysystem/inventory.mv.db` on Mac; `%USERPROFILE%\.inventorysystem\inventory.mv.db` on Windows. Receipts and rotating logs use `receipts/` and `logs/` beneath that directory. Different OS profiles have different data.

Admin → File → **Back up database** saves a consistent ZIP including accounts, recovery hashes, settings, sales and receipt records. Before any pending schema upgrade, the app makes an automatic local ZIP under `migration-backups/` next to the database; failure stops the upgrade. Keep your own backups separately too.

Restore: close every app window, preserve the current database, extract a backup into a temporary folder, and copy its `inventory.mv.db` to the data location (or configured DB_URL location). Start the same or newer app and verify accounts, stock and recent sales. Do not copy a database while it is being written. See [SHOP_SETUP.md](docs/SHOP_SETUP.md).

Uninstall: close the app, remove its app bundle (Mac) or extracted portable folder (Windows). This does not delete shop data. Preserve/export your database, receipts and backups before considering any separate manual data deletion; follow local retention requirements.

## Tax and Legal

Receipts are simple sales records, not tax or fiscal invoices. The shop owner is responsible for following local tax, invoicing and fiscal-device rules. The optional percentage is not country-specific tax logic or compliance certification. Read [TERMS.md](TERMS.md), [PRIVACY.md](PRIVACY.md), [LICENSE.txt](LICENSE.txt) and [THIRD-PARTY-NOTICES.txt](THIRD-PARTY-NOTICES.txt). Legal drafts require qualified review before commercial release.

## Optional configuration

No configuration file is needed for normal use. `SHOP_CURRENCY` has been removed; settings are in the database.

| Key | Purpose |
|---|---|
| `DB_URL` | Optional local H2 file URL; remote URLs are rejected. |
| `DB_USER` | Embedded DB user, default `sa`; separate from app accounts. |
| `DB_PASSWORD` | Embedded DB password, empty by default. |
| `INVENTORY_ENV_FILE` | Environment variable selecting a configuration file. |

Otherwise configuration is read from `~/.inventorysystem/.env`, working-directory `.env`, then `InventorySystemi/.env`. JVM properties override environment variables, which override file values. No initial account/password keys exist.

## Development, verification and packaging

Development requires JDK 21, Maven wrapper and Python 3 for packaging. Initial dependency/runtime downloads are build tasks only. `.tools/jdk` is used when JAVA_HOME is unset. The local Maven repository is `.tools/m2`; tests use disposable project-local homes and temporary data.

```sh
./mvnw clean test
./mvnw javafx:run
./mvnw -Dinventory.guiTest=true -Dtest=DesktopSmokeTest test
bash InventorySystemi/scripts/package-macos.sh
python3 InventorySystemi/scripts/package-windows-portable.py
python3 InventorySystemi/scripts/verify-macos-package.py
```

Brand/version/platform metadata comes from `app-metadata.properties`. The original logo is bundled locally. Build cleanup is recorded in [docs/DELETED_FILES.txt](docs/DELETED_FILES.txt); it never targets data outside the project. See [VERIFICATION.md](docs/VERIFICATION.md), [CHANGELOG.md](CHANGELOG.md) and [PROJECT_REVIEW.md](docs/PROJECT_REVIEW.md).

Support: **tresorbi29@gmail.com**. No email, browser or support-data transfer is opened automatically by the app.

After an update, reinstall/replace the old app with the newly built 1.3.0 package. Your existing local accounts, stock and sales are kept. Do not delete the shop data folder.
