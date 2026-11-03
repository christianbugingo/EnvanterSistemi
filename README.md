# Inventory System 1.1

A JavaFX shop application for an owner and staff, with English/Turkish interfaces, piece/kg stock, staff account management, and an activity dashboard. The Maven project is `InventorySystemi/`; the root wrapper delegates to it.

## Run on this Mac

```sh
./mvnw clean package
./mvnw javafx:run
```

This checkout has an ignored Apple Silicon JDK 21 at `.tools/jdk`. The wrapper uses it when `JAVA_HOME` is unset. On another computer install JDK 21 of the correct architecture. VS Code's **Run Inventory System (JavaFX)** task runs the app; select the same JDK for editor features/debugging.

Close the older 1.0 app before using 1.1. Back up your database with the app closed before upgrading. The new version adds schema columns and changes stock quantities to decimals. Existing accounts/products/sales are retained; do not run old clients against an upgraded database.

## Admin and staff

For an empty database, click **Create first administrator** and choose a username and password (8–128 characters). There is no built-in password. If you created `admin` / `AdminDemo123!` during the earlier demo, that account keeps its password. Existing plaintext account passwords are upgraded to salted PBKDF2 hashes on successful login.

After admin sign-in:

- **Products:** create/edit stock, unit, per-unit selling price, and a configurable low-stock threshold.
- **Sales:** view all staff sale lines and void a sale if necessary. A void restores stock once and retains the original sale/audit records.
- **Reports:** choose dates, see quantities separated by unit and revenue based on sale-time prices, and export CSV.
- **Staff accounts:** add staff with temporary passwords, reset a temporary password, or enable/disable a staff account.
- **Staff activity:** see usernames, actions, record details, timestamps and workstation names. The latest 500 events refresh every 10 seconds.

Staff must replace an admin-issued temporary password before using the dashboard. Canceling that dialog leaves them signed out. Staff can sell and view their own latest 1,000 sale lines; they cannot edit products, manage accounts, void sales, or read shop-wide reports/activity. Both roles can change their own password from the File menu. Password resets and account disabling are checked again on subsequent operations, including already-open sessions.

## Products by piece and kilogram

| Product type | Example stock | Example sale | Price means |
|---|---:|---:|---|
| Piece | 20 | 2 | Price of one piece |
| kg | 10.000 | 1.250 | Price of one kilogram |

Piece quantities and thresholds must be whole numbers. kg supports up to 3 decimal places. Enter a decimal point or comma without thousands separators. Prices allow 2 decimal places. Stock updates use exact database decimals and transactions, not floating-point subtraction.

Each sale records its product name, unit and price. Later product price/name changes do not rewrite sales history. Sale line totals round to 2 decimal places, and reports sum those same rounded totals. The product unit cannot change after sales exist; create a new product instead. Old sales without price snapshots are backfilled once using the current product price because their original price was not stored.

Receipts no longer add an arbitrary fixed tax. They record selling prices and are **not fiscal invoices**. Payment methods, jurisdiction-specific tax configuration, shift/cash closing and order-level receipts remain future work.

## Configuration and data

`.env` is now read automatically when the app starts. The first file found is used:

1. `~/.inventorysystem/.env` (Windows: `%USERPROFILE%\.inventorysystem\.env`). Recommended for installed apps.
2. `.env` in the current working directory.
3. `InventorySystemi/.env` when running from the repository root.

Set `INVENTORY_ENV_FILE` to explicitly choose a file. JVM `-D` properties override environment variables, which override file values. Values are literal: quoted values work, but shell commands, variable expansion and inline comments are not evaluated. Restart after changing configuration.

The existing ignored `.env` uses local H2 with database user `sa` and an empty database password. This is separate from application login accounts. `SHOP_CURRENCY` defaults to `TRY` and stays the same when the UI language changes; configure it identically on every shop PC.

Default local data: `~/.inventorysystem/inventory.mv.db`. Receipts: `~/.inventorysystem/receipts`. A packaged app keeps data outside the installation directory. Tests use temporary/in-memory databases. The earlier `database/demo-users.sql` now contains a read-only account query; use the UI to create accounts with hashed passwords.

**For staff on different computers, follow [the shared-shop setup guide](docs/SHOP_SETUP.md).** Every computer needs the same shared MySQL database, not a separate H2 file. The guide includes server/database setup, the `.env.shop.example` profile and a two-computer acceptance test. Existing local H2 data is not automatically migrated into MySQL.

## Test the final project

```sh
./mvnw test
# Local desktop required; opens/closes test windows using disposable data:
./mvnw -Dinventory.guiTest=true -Dtest=DesktopSmokeTest test
```

Checks cover migration/persistence, translation-key coverage, UTF-8 receipts, fractional kg sales, concurrent checkout, cart rollback, price snapshots, stale product edits, void/restock, staff restrictions, forced password changes, resets and disabled sessions. The GUI test checks both languages, admin/staff tabs, cart entry and the actual forced-change dialog. It saves previews under `InventorySystemi/target/ui-previews/`.

The optional `MySqlMultiPcTest` requires an **empty disposable** `inventory_test` schema and the environment variables `INVENTORY_MYSQL_TEST_URL`, `INVENTORY_MYSQL_TEST_USER`, and `INVENTORY_MYSQL_TEST_PASSWORD`. Run `./mvnw -Dtest=MySqlMultiPcTest test`. It starts independent staff JVMs against the same server, tests competing checkouts, verifies the admin audit/report, and checks that disabling staff rejects an already-open remote session. Never point it at a real shop database; it refuses a nonempty schema.

Manual acceptance:

1. Admin creates a staff member. Staff changes the temporary password; the old password then fails.
2. Add 20 milk cartons (Piece, 30.00) and 10 kg of apples (49.99/kg). Sell 2 cartons and 1.250 kg of apples.
3. Check stocks 18 and 8.750; revenue must be 122.49. Verify the staff username and computer in admin activity.
4. Attempt an oversell and a fractional piece sale: both must fail without partial changes.
5. Change a product price: existing report revenue must stay unchanged. Void the test sale: stock must restore once and the report must exclude the void.
6. Switch English/Turkish, inspect the cart and low-stock report, export CSV, reopen the app, and verify persistence.
7. Repeat with two clients against the shared database, following the network guide.

## Native applications

macOS, from the repository root:

```sh
bash InventorySystemi/scripts/package-macos.sh
open InventorySystemi/dist/macos-arm64-1.1.0/InventorySystem.app
```

The script creates a bundled `.app` and `.dmg` under `dist/macos-<architecture>-1.1.0` inside `InventorySystemi/`. It preserves the earlier 1.0 app. To rebuild into another folder, set `INVENTORY_DIST_DIR` (relative paths are relative to `InventorySystemi/`). Signing/notarization is not configured. Build separately on an Intel Mac for Intel users.

Windows: install JDK 21 plus the installer tooling required by its `jpackage` (WiX for MSI), then run from PowerShell:

```powershell
.\mvnw.cmd clean package
powershell -ExecutionPolicy Bypass -File InventorySystemi/scripts/package-windows.ps1
```

Output: `InventorySystemi/dist/windows-1.1.0/InventorySystem/InventorySystem.exe` and an MSI. Java is bundled in native app images. Windows packaging must be executed/tested on Windows.

## Scope and recommendations

The direct database client is suitable for a trusted-LAN project/pilot. Staff roles are checked in the application services, but clients still have database credentials. For production, move authentication, permissions and transactions into an HTTPS server, add login throttling and enforce audit integrity there. Database administrators can currently change audit records. No offline selling or automatic H2-to-MySQL migration is implemented. Periodic database calls are synchronous and can delay the UI on a slow/unavailable server.

See [the review and recommended next features](docs/PROJECT_REVIEW.md) for comparisons with Odoo/ERPNext and the deployment priorities.

Verification results and their limits are recorded in [docs/VERIFICATION.md](docs/VERIFICATION.md).
