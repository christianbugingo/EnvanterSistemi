# Shop setup and acceptance

## Install and first run

| Machine | Required build | Execution status |
|---|---|---|
| Apple Silicon Mac (M1+) | macOS arm64, macOS 11+ deployment target | Tested on the current arm64 host; other OS versions untested. |
| Intel Mac | macOS x86_64 | Not built/tested here; native Intel Mac required. |
| Linux x86_64 | Ubuntu/Debian-based graphical desktop, GTK 3 and X11/XWayland | Not built/tested here; distribution versions not yet validated. |
| Windows 10/11 x64 | Windows portable ZIP | Structure checked; execution not tested. |

Mac: Apple menu → About This Mac shows “Chip: Apple M…” (arm64) or “Processor: Intel…” (x86_64). Use the matching `InventorySystem-1.3.0-macos-<architecture>.dmg/.app`. Linux: `uname -m` must print `x86_64`; extract the Linux tar.gz and run `InventorySystem/launch.sh`. Java and JavaFX are bundled, but Linux graphical system libraries are still required. Windows: extract the entire portable ZIP and run InventorySystem.exe.

Close the old app before replacing it. Keep the shop data directory and backups. Only the arm64 Mac package was rebuilt in this change; the existing Windows ZIP remains unchanged. Intel and Linux artifacts must be produced on their native build machines using [PACKAGING.md](PACKAGING.md). Test launch, setup, login, weighted sale, receipt, backup and launch with Wi-Fi off before shop deployment.

The welcome page offers Create your administrator account. Read and accept the local Terms/Privacy Notice before setup can continue. Create your own administrator, save/print the one-time recovery code and acknowledge saving it. Then search/select the shop currency explicitly. Set optional tax (0–100%, up to four decimals) and English or Turkish. There is no default currency or login password.

Legal documents are drafts: the governing-law placeholder and all clauses marked for lawyer review must be resolved before commercial distribution. Acceptance is local and does not send data to COODIC LABS.

Sign in and create staff in Staff accounts. Give temporary passwords privately; staff must replace them at first login. Admin can reset/disable staff with confirmation. Use the same OS profile when sharing one laptop. A fresh PC creates an independent shop; restoring a backup moves a copy, without synchronization.

## Test a sale

For an explicit two-decimal test shop, choose USD and tax 0 in Admin settings. Add Milk as Piece, stock 20, price 30.00, and Apples as Weight, stock 10 kg, price/kg 49.99. Add five milk cartons and 230 g apples. Subtotal/total is 161.50 USD; stock becomes 15 cartons and 9.77 kg. Choose Cash/Card/Other and complete without an amount entry.

With tax 18%, a separate 230 g apples-only sale has subtotal 11.50 USD, tax 2.07 USD and total 13.57 USD. Tax is rounded once per checkout. A zero-tax receipt contains only total. Barcode scanners must type a code plus Enter: piece adds one, weight focuses its prompt. Plain weight numbers are kg; `0.23 kg` and `230 g` both mean 230 g.

Change currency to RWF with confirmation. Prices are reinterpreted and rounded, not exchanged: verify them. New amounts have zero decimals; old USD receipts still show USD with two decimals. Reports keep currencies separate. Old records with no currency stay `?` rather than being relabelled. Set your real shop currency/tax after disposable testing.

## Try dashboards and receive stock

Create Biscuits as Piece with starting stock 0 and a default price of 150 RWF in a disposable RWF shop. Select it → Receive stock: 10 pieces, total paid 1000 RWF. Receive another 10 for 1010 RWF, dated later. Leave the optional batch selling price empty to use the product default. Stock batches shows costs 100 and 101 per piece. Sell 5, then 10: FIFO costs are 500 and 1005 RWF; five pieces remain from the second batch. Dashboard gross profit is 745 RWF on revenue 2250 RWF with no tax. Setting a batch selling price overrides the product default only for that batch.

New sale has one name/barcode input. Type to filter, choose the dropdown item, or press Enter after scanning. Pieces add one immediately; weight selection focuses its quantity field. Type `19 kg`, `19.5 kg`, `0.23 kg` or `230 g` and inspect normalized weight and price before adding. Plain numbers mean kg. Stock must be sufficient; pieces reject decimals.

Dashboard shows KPI cards and three sales charts. Profit periods include Today, Monday-to-today week, month-to-date, year-to-date and inclusive custom dates. Compare revenue, cost, gross profit and margin. Unknown opening costs are listed as “cost not recorded”; the profit excludes those lines and is marked incomplete. Tax is excluded from revenue. Each currency remains separate; foreign purchase costs are not converted. This is gross profit before operating expenses.

Admin sees shop-wide sales. Staff see only their own sales; shared low-stock status remains visible. Staff profit is hidden until an administrator enables it in Admin settings, and then still shows only that staff member's results. The tax field explains that `00` means no tax and is stored as zero.

On upgrade, backed-up migration creates an unknown-cost opening batch from each existing positive stock quantity. Products and sales are preserved. Product edits do not change batch costs. Duplicate name/unit detection offers Receive stock instead; rename if a separate product is intended. Use stock correction only for physical count corrections, with a reason.

## Password tests

Use Change password on the login screen with username/current/new/confirmation. Minimum 10 characters, one letter and one digit, not username/recent passwords; maximum 72 UTF-8 bytes. Five failures lock for five minutes and show a countdown.

Staff Forgot password instructs them to ask the administrator for a temporary reset. Admin Forgot password consumes the saved recovery code, sets a temporary password, and displays a replacement code that must be saved. Change the temporary password at next login. Signed-in admins can replace the code from File. Existing admins should generate/save a code after upgrading if needed.

If password and recovery code are both lost, restore a backup with credentials/code you still know. Restoring does not discover an unknown password. With no usable credentials or backup, preserve the database; there is no bypass.

## Back up, restore, uninstall

Admin → File → Back up database creates a consistent ZIP with accounts, settings, acceptance, snapshots and receipt records. Receipts can be regenerated from the database; keep older standalone receipt files separately if needed. Backups include private business data and hashes. Store copies separately from the laptop.

Automatic pre-migration ZIPs are next to the database under migration-backups/. A failed backup stops the migration. They supplement regular user-controlled backups.

To restore: close all app windows, preserve any destination database, extract the backup to a temporary folder, and copy inventory.mv.db to ~/.inventorysystem/ on Mac or %USERPROFILE%\.inventorysystem\ on Windows (or your configured DB_URL location). Open the same/newer app, sign in with restored credentials and verify stock/history. Do not copy a live database. Independently modified copies do not merge.

To uninstall: close the app and remove only its app bundle or extracted portable application folder. Local data remains. Preserve exports/backups and comply with local retention rules before any separate manual data deletion.

## Tax and Legal

Receipts are simple sales records, not tax or fiscal invoices. The shop owner must follow local tax, invoicing and fiscal-device rules. An entered rate does not certify compliance. No card charge, cash transfer or automatic refund is performed.

## Deployment checklist

On disposable data, disconnect networking and test welcome/setup/legal acceptance, recovery saving, chosen currency, staff first-login password change, weight and piece sales, tax, oversell/fractional-piece rejection, reprint, void with reason, report currency separation and restart persistence. Create a backup and restore it to a disposable location. Repeat on the Windows x64 PC. Inspect About for acceptance date and database location. Physical scanner/printer/scale and disconnected-network deployment tests remain shop-specific.

Support: COODIC LABS · tresorbi29@gmail.com · https://coodic.org/ · Rwanda. The app does not open or contact support automatically.
