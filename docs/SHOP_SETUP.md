# Offline shop: one laptop, separate logins

This replaces the earlier shared-MySQL plan. No internet, Wi-Fi, server or cloud account is required to use the packaged application. Admin and staff take turns on the same laptop and operating-system profile. Each app login has its own role; all logins use the same local database.

## First setup

1. Close older app windows. Open the latest Mac app, or extract the entire Windows ZIP and open InventorySystem.exe.
2. Choose Create first administrator if the database is empty. Otherwise sign in with your existing account. No account is automatically reset during an update.
3. In Staff accounts, create a username and a temporary password (8–128 characters). Staff signs in and replaces that password. Admin can later reset it or disable the account.
4. Create shop products. Piece quantities must be whole numbers; kg quantities allow 3 decimal places. Selling price is per selected unit. A unique barcode is optional. Barcode scanners must behave like a keyboard and send Enter; camera and weighing-scale drivers are not included.
5. Use Receive / adjust stock to record deliveries, losses or corrections, with a reason. Price/name/category edits do not silently change stock.

## Cashier handover

Open a shift with the cash physically in the drawer. Enter sales, selecting cash or card bookkeeping for each checkout. Cash received must cover the total; the receipt includes change.

At handover, count the drawer and close the shift. A discrepancy must have a note. Sign out. The next staff member signs in and opens the next shift. Only one shift may remain open on this laptop. An administrator can close a forgotten staff shift using the actual counted cash and a note. Signing out does not discard an open shift.

Expected cash = opening cash + non-voided cash sale line totals. The cashier must perform any recorded cash refund manually. Card payments do not affect the cash count and must be confirmed separately on a real card terminal. The app does not connect to payment networks.

## Back up and restore

Admin → File → Back up database saves a consistent ZIP while the app is open. It includes the database, not the separate receipt text folder. Receipts for 1.2 checkouts can be regenerated from the database; back up older receipt files separately if required. Backup files contain business data and account hashes, so store them privately.

To restore on the same or another laptop:

1. Close every InventorySystem window on that laptop.
2. Keep a copy of its current `~/.inventorysystem/inventory.mv.db` (Windows: `%USERPROFILE%\.inventorysystem\inventory.mv.db`) before replacing anything.
3. Extract the backup ZIP into a temporary folder. Copy its `inventory.mv.db` to that user data location. Create `.inventorysystem` if necessary. Replace only after preserving the previous database.
4. Start the same or newer app version. Sign in with an account from the restored backup and verify product quantities and recent sales.

Do not copy a live database file while the app is writing; use the backup command. A copy moved to another laptop is an independent shop database, not a synchronized second register. This offline edition does not merge independently modified copies.

## Final-project acceptance test

Disconnect Wi-Fi and Ethernet before starting.

1. Admin signs in, creates a staff account, signs out. Staff changes its temporary password. Confirm its temporary password no longer works.
2. Admin adds `Milk`, Piece, quantity 20, price 30.00, barcode `8691234567890`; and `Apples`, kg, quantity 10.000, price 49.99.
3. Admin receives 5 more milk cartons with reason `Supplier delivery`. Quantity must become 25 and activity must record the adjustment.
4. Staff signs in, opens a shift with 100.00, scans/enters the milk barcode twice, and adds 1.250 kg of apples. Checkout total: 122.49. Enter 150.00 cash. Change: 27.51. Stocks: 23 cartons and 8.750 kg.
5. Select one sale line and reprint its receipt. All lines from that checkout, its ID, payment, cash received and change must be present.
6. Record one additional milk carton as Card. Confirm expected drawer cash remains 222.49. Card selection records the external payment, not an actual charge.
7. Try an oversell, fractional piece quantity and insufficient cash: each must be rejected without partial stock/payment changes.
8. Admin may void a sale while the shift is open. Its stock restores once and expected cash decreases if the sale was cash. The cashier gives the corresponding cash refund manually. A second void must fail.
9. Close the shift using its expected cash value. Try a different count without a note: it must fail. With a discrepancy note it may close and record the difference. Closed-shift sales must not be voidable.
10. Admin checks Reports, Staff activity, Staff accounts and Shifts / cash. Staff must not see admin-only tabs/actions. Sign out and restart while still offline; data must persist.
11. Admin creates a backup and verifies the restore procedure on a disposable copy. Repeat this checklist on the Windows test PC using the extracted portable app.

Changing the interface language must translate buttons/dialogs and number formatting, while keeping product data and the shop currency intact.
