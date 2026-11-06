# Inventory System 1.3.0

**COODIC LABS** · Rwanda · tresorbi29@gmail.com · https://coodic.org/

An offline Java 21 / JavaFX application for local shop accounts, products, stock, sales, receipts, reports and backups. It has no network calls, cloud sync, telemetry, remote database or payment processing. Currency and optional tax are chosen by the shop; no country's currency is selected by default. Original code is proprietary, all rights reserved. Third-party software retains its own licenses.

## Download / 1.3.0 local release

**Only the macOS Apple Silicon arm64 DMG is designated for the 1.3.0 release.** The DMG and its SHA-256 sidecar are prepared locally in `InventorySystemi/dist/macos/`; no GitHub Release, website upload or public download was created by this task.

- `InventorySystem-1.3.0-macos-arm64.dmg`
- `InventorySystem-1.3.0-macos-arm64.dmg.sha256`

Intel macOS, Linux and Windows builds are **not yet released** and **not updated for this release**. An existing Windows ZIP is retained as a historical build; it is not part of this release. See [RELEASE-1.3.0.md](docs/RELEASE-1.3.0.md) for the accepted notarization ID, checksum, tested scope and manual quarantine/upload instructions.

## Requirements and installation

| Computer | Package / requirements | Real execution status |
|---|---|---|
| macOS Apple Silicon (M1 or newer) | arm64, macOS 11+ deployment target; `.app` / `.dmg` | Tested on this arm64 host, including a process-network-denied sale/receipt/backup workflow; other OS versions still need acceptance. |
| macOS Intel | x86_64 JDK/JavaFX build, macOS 11+ deployment target | **Not updated for this release; not yet released.** Native Intel build/execution not tested. |
| Linux x86_64 | 64-bit Ubuntu/Debian-based graphical desktop; GTK 3, X11/XWayland, font/OpenGL libraries; `.tar.gz` app image | **Not updated for this release; not yet released.** Linux build/execution not tested. |
| Windows 10/11 x64 | Complete portable ZIP, bundled runtime | ZIP structurally verified; **not execution-tested** on a Windows PC. **Not updated for this release; not yet released.** Existing ZIP retained unchanged. |

Check your Mac: **Apple menu → About This Mac**. “Chip: Apple M…” means arm64; “Processor: Intel…” means x86_64. On Linux, `uname -m` must show `x86_64`.

Mac: open `InventorySystemi/dist/macos/InventorySystem-1.3.0-macos-arm64.dmg` and copy the app to Applications, or open `InventorySystem-1.3.0-macos-arm64.app` directly. The Intel builder produces separately named `InventorySystem-1.3.0-macos-x86_64.app/.dmg` when run on an Intel Mac. Close older copies first. The published arm64 build is Developer ID signed, notarized by Apple and stapled. The ordinary development packager creates ad-hoc builds; use the separate signing/notarization workflow for public releases. See [NOTARIZATION.md](docs/NOTARIZATION.md).

Linux: on a Linux build machine, the builder creates `InventorySystemi/dist/linux/InventorySystem-1.3.0-linux-x86_64.tar.gz`. Extract the whole archive and run `InventorySystem/launch.sh`. Move the folder to a permanent location before optionally running `InventorySystem/install-desktop.sh` to add its menu entry. Remove `~/.local/share/applications/com.coodiclabs.inventorysystem.desktop` when uninstalling that entry (or the corresponding XDG_DATA_HOME location).

Windows: extract the **entire** `InventorySystemi/dist/windows/InventorySystem-Windows-x64.zip`, then open `InventorySystem/InventorySystem.exe`. Keep bundled folders together. `Start InventorySystem.cmd` is a fallback; `Run with console.cmd` provides diagnostics.

All package formats bundle Java 21, JavaFX and local legal notices. Installed shop use needs no internet or separate Java installation; Linux still needs ordinary graphical system libraries. The welcome page offers account creation on a fresh installation. Every new computer starts a separate shop; replacing an app leaves local data intact.

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

Amounts include the bundled symbol when it differs from the code, with consistent grouping and code suffixes independent of UI language: `1,250 RWF`, `$ 12.50 USD`, `1.234 KWD`. Names and symbols are shown in the currency chooser. Prices and line totals use BigDecimal and HALF_UP at the currency precision. Sales without a recorded currency from older versions remain `?`, with their historical two-decimal amounts; a new shop currency is never silently applied to them. Keep original records to identify those legacy amounts.

The tax field shows “If you do not use tax, enter 00.” Both `00` and `0` store zero; default is 0. Tax rate accepts 0–100%, with up to four decimals. Tax is calculated **once per checkout**, after rounded line totals are summed, at that checkout's currency precision. With tax above zero, the screen, receipt and report show subtotal, tax and total; with zero, the receipt shows only total. Reports keep currencies separate. Tax across unit-separated report rows is allocated with rounding residue on the last unit; voided lines are excluded and remaining checkout tax is recomputed using its original rate. Original receipts retain their original amounts and mark voids.

## Products, sales and accounts

Choose **Sold by: Piece / Weight (kg)** first. Piece quantities are whole counts; weight stock and sales use integer grams. Price is per piece or kg using the shop currency precision. Product names, category and supplier are required; optional barcodes are unique. Use **Receive stock** to record purchases as dated batches, with quantity, total paid and an optional batch-specific selling price. Cost per piece/kg is calculated automatically. Existing products with the same name and unit are detected: receive stock instead, or rename the new product. Product edits change name, barcode, low-stock threshold and default selling price; quantity, unit, category and supplier stay fixed. Stock corrections require a reason. To change a unit, create a new product.

New sale: type a name or barcode in the **single product search field**; its dropdown narrows without replacing your text. Select a result or scan a keyboard-style barcode ending with Enter. Piece scans add one piece; whole quantities such as 5 or 20 can be entered. Weight scans focus the weight prompt. Enter `0.23`, `0.23 kg` or `230 g`. Plain input in the kg field means kg: plain `230` means 230 kg. A live preview recalculates as you type: at 100 RWF/kg, 0.2 kg shows 200 g and 20 RWF; 0.23 kg or 230 g shows 23 RWF. A 5 g line is 1 RWF after HALF_UP rounding, or 0.50 in a two-decimal currency at 100/kg. Under 1000 g displays as grams; 1000 g and above as kg without trailing zeros: `1 kg`, `1.25 kg`, `19 kg`, `19.5 kg`. Every whole weight from 1 through 23 kg is covered by sale tests; larger available quantities are supported too.

Select Cash / Card / Other for reporting only and complete the sale. No amount received or change is requested. Stock, FIFO batch allocations and costs, sale snapshots, payment type, checkout and receipt record commit together or all roll back. A local receipt file can be regenerated if writing it fails. Admin can void with confirmation and a reason: history/actor/reason are retained and stock and the original batch quantities/costs restore once. Handle any real refund separately.

Admin creates staff in **Staff accounts**, with temporary passwords that must be changed at first login. Staff see their own sales; admin sees all activity and controls products, accounts, settings and backups.

Login-screen **Change password** asks for username/current/new/confirmation. Policy: at least 10 characters, one letter and one digit, different from username and recent passwords; maximum 72 UTF-8 bytes. BCrypt cost 12 protects passwords; older hashes upgrade on successful login. Five failed attempts lock an account for five minutes with a visible countdown; failures are audited.

**Forgot password:** staff ask their administrator for a temporary reset. Admin uses the saved recovery code, sets a temporary password, saves a replacement recovery code, then changes the temporary password at next login. Old recovery codes stop working. With both password and code lost, restore a usable backup; there is no bypass.

Shortcuts: Ctrl/Cmd+N new sale; Ctrl/Cmd+F search; Ctrl/Cmd+Enter complete sale. English is the default; missing optional Turkish translations fall back to English. View → About shows developer/contact, build, legal acceptance date, database location, full terms and the receipt notice.

## Dashboard, batches and profit

The Dashboard tab has a vector logo, today's sales before tax, completed transaction count, pieces/weight sold and low-stock status. Its payment pie, daily bars and trend line use one light theme. Staff sales/charts are restricted to their own activity; low-stock status describes the shared stock. Admin sees shop totals. Choose a currency to inspect its history separately.

Stock batches are consumed oldest received date first (FIFO), then by batch ID. Each sale stores the exact allocated purchase cost and price/currency snapshots. Optional batch prices apply only while that batch is consumed; an order spanning different prices has separate lines. A changed product default affects future lines without an override, never past sales or purchase costs. Historical batch prices in a different currency are not reinterpreted: after a currency change the current product default is used.

Upgrade preserves products and sales and creates one **opening batch** for each product with stock. Its cost is **not recorded**; no purchase cost is invented. Initial stock entered directly and positive stock corrections also have unknown cost. Prefer creating a product with stock 0 and receiving purchases with the actual total paid. Admin can inspect **Stock batches** from Products. New purchase costs do not retroactively assign costs to old sales.

Profit and Loss offers Today, This week (Monday to today), This month, This year and inclusive Custom dates. Revenue excludes tax and voids. Cost, gross profit and margin use only fully costed sale lines in the chosen currency. Unknown-cost lines and their revenue are listed separately; the result is clearly marked **incomplete**. Foreign-currency batch costs are also treated as unavailable rather than converted. The comparison chart uses the same fully costed revenue cohort. Margin is gross profit / fully costed revenue × 100; no margin is shown for zero revenue. This is gross trading profit, before operating expenses, not full financial accounts.

Staff profit is hidden by default. Admin → File → Admin settings → **Allow staff to view their own profit** enables only each staff member's own figures; the service enforces the restriction as well as the screen.

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
bash InventorySystemi/scripts/package-macos.sh arm64
# On a native Intel Mac: bash InventorySystemi/scripts/package-macos.sh x86_64
# On Linux x86_64: bash InventorySystemi/scripts/package-linux.sh
python3 InventorySystemi/scripts/package-windows-portable.py
python3 InventorySystemi/scripts/verify-macos-package.py --arch arm64
```

Build commands, prerequisites, target isolation, native-library verification and the machine acceptance checklist are in [PACKAGING.md](docs/PACKAGING.md). Build logs record the JDK path/version/architecture under `InventorySystemi/target/logs/`. Intel/Linux artifacts are pending native build hosts; no cross-built files are labelled as tested.

Brand/version/platform metadata comes from `app-metadata.properties`. In-app branding uses JavaFX vector shapes, with window icons rendered from them in memory. Native package icon files are unchanged. Build cleanup is recorded in [docs/DELETED_FILES.txt](docs/DELETED_FILES.txt); it never targets data outside the project. See [VERIFICATION.md](docs/VERIFICATION.md), [CHANGELOG.md](CHANGELOG.md) and [PROJECT_REVIEW.md](docs/PROJECT_REVIEW.md).

Support: **tresorbi29@gmail.com**. No email, browser or support-data transfer is opened automatically by the app.

After an update, reinstall/replace the old app with the newly built 1.3.0 package. Your existing local accounts, stock and sales are kept. Do not delete the shop data folder.
