# Inventory System 1.2.1 — offline desktop edition

One laptop can be used by the owner or staff, depending on who signs in. The application stores everything in an embedded local H2 database. **Using the packaged app requires no internet, database server, or separate Java installation.** The first developer build may download Maven/JavaFX; that is separate from using the installed app.

Use the same macOS/Windows user profile when sharing the laptop. Different operating-system profiles have separate home directories and therefore separate shop databases. Signing out of the app does not erase stock, accounts, shifts or sales.

## Latest applications

- Mac (Apple Silicon): `InventorySystemi/dist/macos/InventorySystem.app`; installer `InventorySystemi/dist/macos/InventorySystem.dmg`.
- Windows x64: `InventorySystemi/dist/windows/InventorySystem-Windows-x64.zip`. Extract the **whole ZIP**, then double-click `InventorySystem/InventorySystem.exe`. Java, Windows JavaFX, and the native Windows launcher are bundled. `Start InventorySystem.cmd` is a fallback; `Run with console.cmd` shows startup diagnostics.

The Windows package is assembled on this Mac and must be tested on a Windows PC. It is a portable application, not an MSI installer. The optional `scripts/package-windows.ps1` builds an MSI when run on Windows with JDK 21 and its required installer tooling.

Close the previous app before opening the new version. Back up an existing database before upgrading. Existing local products, users and sale history are preserved through additive schema upgrades. Legacy sales without checkout/payment information remain marked as older records; their missing payment method is not invented.

## Daily workflow

1. At first launch, choose **Create first administrator** and set your own password. Existing accounts keep their passwords; no default account/password is seeded.
2. Admin uses **Staff accounts** to create staff with temporary passwords. Staff must choose a different password on their first login.
3. Admin adds products with a Piece or kg unit, per-unit selling price, optional unique barcode, and low-stock threshold. Existing stock is changed through **Receive / adjust stock**, with a required reason.
4. The cashier signs in and opens a shift in **Shifts / cash**, entering the opening cash float. Only one shift can be open on this laptop.
5. Use **New sale**. Search by name or scan a barcode using a keyboard-style USB scanner configured to send Enter. A piece barcode adds one item; a kg barcode selects the product and asks for its weight. kg supports three decimal places; pieces require whole quantities.
6. Choose **Cash** or **Card (record only)**. Cash requires the amount received and displays change. Card selection is bookkeeping: complete the actual payment separately on the shop's card terminal.
7. Complete the sale. Its checkout ID groups every sale line and payment details. The receipt is saved locally. **Reprint receipt** recreates it from the database, including any voided-line markers. Older pre-1.2 lines have no grouped receipt; use CSV export for those.
8. Before handing the laptop to another person, count cash and close the shift. The expected amount is opening cash plus non-voided cash sales; card sales do not increase the drawer. A discrepancy requires a note. Then sign out and let the next person sign in/open a shift.
9. Admin reviews all staff sales, shifts, reports and **Staff activity**, even while offline. Staff can see their own sales/shifts and cannot manage products, accounts, shop-wide reports, backups or voids.

Admins can void sale lines while their shift is still open. Voids preserve history, restore stock once and reduce expected cash for a cash sale. Sales in a closed shift cannot be voided because that would rewrite a completed cash count. Post-shift refunds need a separate return workflow and are not implemented here. Give any cash refund manually when recording a cash void; the app does not dispense or transfer money.

Prices/names/units are snapshotted at sale time. Reports group quantities by unit and payment method and sum the same rounded line amounts as receipts. Later product edits do not rewrite history. Receipts add no arbitrary tax and are not fiscal invoices.

## Local data and backups

Data: `~/.inventorysystem/inventory.mv.db` on Mac, `%USERPROFILE%\.inventorysystem\inventory.mv.db` on Windows. Receipts: the `receipts` subfolder. App updates/cleanup never remove these files.

Admin: **File → Back up database** creates a consistent ZIP, including accounts, stock, sales, shifts and activity. Keep backups somewhere separate from the laptop. Restore instructions and the complete acceptance checklist are in [docs/SHOP_SETUP.md](docs/SHOP_SETUP.md).

`.env` is optional. Defaults are already offline. A file at `~/.inventorysystem/.env`, `.env` in the launch directory, or `InventorySystemi/.env` in the repository is read at startup (first one found). `INVENTORY_ENV_FILE` can choose a file explicitly. JVM properties override environment variables, which override file settings. `SHOP_CURRENCY` defaults to TRY. Remote database URLs are rejected in this edition; remove stale MySQL settings instead of starting a server.

## Development and tests

From the repository root:

```sh
./mvnw clean package
./mvnw javafx:run
./mvnw test
# Desktop required; disposable database and test windows:
./mvnw -Dinventory.guiTest=true -Dtest=DesktopSmokeTest test
```

JDK 21 is required for development. This checkout's ignored `.tools/jdk` is used automatically when JAVA_HOME is unset. Maven is supplied by the wrapper. VS Code tasks build/run/test the project.

Build both platforms on this Mac, in this order:

```sh
bash InventorySystemi/scripts/package-macos.sh
python3 InventorySystemi/scripts/package-windows-portable.py
```

Windows packaging downloads/caches a Windows JDK on the first build. Both delivered apps run offline afterward. The native portable Windows launcher uses the bundled JDK's [OpenJDK launcher template](https://github.com/openjdk/jdk21u/blob/master/src/jdk.jpackage/windows/classes/jdk/jpackage/internal/WindowsAppImageBuilder.java); its notices are retained in the package.

Each successful package replaces the previous build for that platform. Known old versioned apps/installers in the project's `dist/` and earlier generated app caches are removed. The latest Mac and Windows packages are both kept. Failed builds keep the last complete release. This cleanup does not search Downloads or /Applications and does not delete shop data.

See [verification results](docs/VERIFICATION.md) and [scope/recommendations](docs/PROJECT_REVIEW.md). Mac signing/notarization, Windows execution, electronic-scale integration, card processing and a post-shift return workflow are not claimed as complete.

## Logo and other computers

The supplied artwork is stored unchanged at `InventorySystemi/src/branding/inventory-logo.png`. It is bundled inside the application JAR and displayed on the login screen, dashboard and window icon. Native icons are in `InventorySystemi/assets/`. On macOS, `python3 InventorySystemi/scripts/create-icons.py` regenerates the ICNS/ICO files from that source. The portable Windows builder embeds the ICO into the launcher while preserving its code and other resources.

The current Mac package targets **Apple Silicon (arm64)**. The Windows ZIP targets **Windows x64** and includes its own Java/JavaFX runtime. The Mac UI has been tested; Windows execution still needs testing on a Windows PC. Intel Macs, native Windows ARM and Linux do not have tested packages in this release. The Mac app is not Developer ID signed/notarized.

On a fresh computer, create its first administrator or restore your database backup. Installed apps do not contain your current users/products and do not synchronize computers automatically. Use the offline acceptance checklist before relying on a new machine for shop work.
