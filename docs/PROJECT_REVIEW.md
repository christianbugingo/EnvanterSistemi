# Offline edition: delivered scope and next steps

Version 1.2 deliberately uses one laptop with role-based logins rather than a network server. The shared-MySQL template, driver and multi-PC test have been retired. There is no runtime internet requirement.

Implemented priorities:

- Unique optional product barcodes; keyboard-style barcode scanning with Enter; piece scanning adds one item and kg scanning requests the weight.
- Checkout IDs grouping sale lines, persisted cash/card bookkeeping, cash received/change and repeat-checkout protection.
- One active shift per laptop, opening floats, counted/expected cash, discrepancy notes, and admin review of staff shifts.
- Sale-time price snapshots, unit/payment-separated reports, receipt regeneration and retained voids. Voids cannot rewrite a closed shift.
- Reasoned stock receiving/adjustments, admin database backups and a verified restore procedure.
- Self-contained Mac and Windows x64 portable releases with one current package per platform. Older generated apps are removed only after a new build succeeds.

Possible next work, if needed: supervised post-shift returns with refund/payment records, cash-in/cash-out transactions, stock adjustment reports, pagination and staff/date filters, printer/PDF receipts, electronic-scale integration, installer signing, and login throttling. None requires adding a cloud dependency. A backend can remain out of scope unless simultaneous registers are requested later.

Important scope: card selection does not process a charge. USB keyboard scanners are supported, while camera barcode recognition and scale hardware have not been tested. Shift accounting assumes no unrecorded cash movements beyond opening float, cash sales and same-shift voids. A receipt is not a fiscal invoice. People with direct access to the operating-system profile/database files remain outside the application's role boundary.
