# Shop-system review

## Delivered in 1.1

- Complete English/Turkish cart labels, button names, empty states, low-stock text and account dialogs. Currency remains the shop currency rather than changing with language.
- Admin-created staff accounts, temporary-password replacement before access, password resets, account disabling, and password hashing with legacy login migration.
- Permission checks in services as well as hidden/disabled controls. Staff see their own sales; admins see shop reports, staff management and activity.
- Piece/kg quantities using exact decimal stock and per-product low-stock thresholds. Repeated items in a cart are checked together; concurrent sales cannot oversell.
- Sale-time name/unit/price snapshots, exact line rounding, reports split by unit, retained voids with once-only restocking, optimistic version checks for stock/product edits, and transaction-bound audit writes.
- Proper UTF-8 CSV quoting, decimal values independent of UI number formatting, unique local receipt files and an additive schema upgrade.
- Automatic configuration-file loading for installed clients and instructions for a shared MySQL shop.

## What comparable projects suggest

The recommendations below are engineering priorities for this project, based on reviewing the linked documentation, not claims that these full systems are implemented here.

| Reference | Relevant behavior | Recommendation |
|---|---|---|
| [Odoo POS employee management](https://www.odoo.com/documentation/saas-18.4/applications/sales/point_of_sale/employee_login.html) | Individual employee identification and access levels in a POS session | Keep individual staff accounts; next add shift/session opening, closing and supervisor-approved returns. |
| [Odoo units of measure](https://www.odoo.com/documentation/19.0/applications/inventory_and_mrp/inventory/product_management/configure/uom.html) | Stock/sales units and purchase-to-stock conversion | Piece/kg are now explicit. Next add barcode, supplier pack sizes and validated unit conversion instead of creating ambiguous quantities. |
| [ERPNext POS profiles](https://docs.frappe.io/erpnext/pos-profile) | User assignments, stock validation, payment methods, currency and receipt defaults | Add per-register profiles, payment records, receipt reprinting and a checkout/order ID that groups multiple sale lines. |
| [ERPNext role permissions](https://docs.frappe.io/erpnext/role-based-permissions) | Role-based read/write and document permissions | Move enforcement to a shared server before deploying to untrusted staff PCs; keep cashier, supervisor and owner capabilities explicit. |

## Recommended order of work

1. **Deployment boundary:** an HTTPS backend owning the database credentials, server-verified sessions, rate limits, central backups and a restore test. The current JDBC connection is a trusted-LAN pilot design. Do not expose it directly over the internet.
2. **Checkout and reconciliation:** order IDs, tender/payment methods, shifts, cash counts, payment reconciliation, returns/void reasons and supervisor approval. There is no card-terminal integration in this release.
3. **Product operations:** barcode search/scanning, stock receiving/purchase orders, supplier pricing, adjustment reasons, per-unit minimum stock and optional electronic-scale input. kg entry is manual today.
4. **Reporting depth:** paginated/filterable audit history, staff/date filters, stock movement ledger, cost/margin reporting and register-level totals. Current sales/activity views deliberately cap at 1,000/500 rows; period reports use all matching non-voided sale lines.
5. **Operations and release quality:** asynchronous network requests, connection pooling, explicit versioned server-side migrations, MySQL backup scheduling, signed/notarized Mac releases and Windows installer testing. Existing periodic reads may temporarily block the UI on slow connections.

## Remaining limits to state in a final-project presentation

- Central server setup, certificate trust and physical PC/network configuration are deployment tasks; installing a desktop app alone does not create a shared shop.
- Schema upgrades preserve old sales but cannot reconstruct a price never stored by the old version; those legacy rows are backfilled with the current price once. Back up before upgrading, and upgrade all clients together.
- Voids retain the original sale and audit event. Receipts are stored on the selling PC, not centrally, and there is no built-in reprint flow yet.
- Taxes are not automatically added. The receipt is not a fiscal invoice or a payment confirmation.
- No offline transactions, automatic unit conversion, tamper-proof auditing or brute-force login protection is claimed.
