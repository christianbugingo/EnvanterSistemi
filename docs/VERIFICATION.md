# Verification — 2 November 2026

Host: Apple Silicon macOS, JDK 21 (local Corretto), JavaFX 21, application 1.1.0.

| Check | Result |
|---|---|
| Clean build/package | Passed; 17 non-GUI tests, no failures/errors |
| Opt-in desktop smoke test | Passed in English and Turkish, including admin/staff views, weighted cart entry and mandatory first-login password replacement |
| MySQL 8.4 independent-client test | Passed against an isolated Docker database; separate staff JVMs competed for stock, admin saw the sale/audit, void restored stock, and disabling staff blocked an already-open session |
| Native Apple Silicon app and DMG | Built successfully; packaged app launched against disposable data without startup errors |
| Windows installer | Prepared, not built/tested on this Mac |

The default test run reports 19 tests with 2 skipped: the desktop and external MySQL tests are opt-in. Each of those was run separately and passed. The desktop test logs JavaFX's classpath/unnamed-module warning; it did not log an application exception.

The service tests cover price snapshots, unit-separated reports, three-decimal kg quantities, integer-only pieces, oversell prevention, complete cart rollback including audit records, once-only void/restock, stale stock edits, staff authorization, temporary-password restrictions, resets/disabling, legacy password upgrades and password secrecy in activity records. Other tests cover additive legacy schema migration, restart persistence, translation coverage, UTF-8 receipts and safe CSV encoding.

Generated UI previews are in `InventorySystemi/target/ui-previews/`. They contain disposable example data. The temporary MySQL container and its anonymous data volume were removed after testing; the downloaded image remains cached for future use.

Limits: this verified separate application processes on one Mac, not two physical shop computers, Windows clients, external LAN/TLS configuration, MySQL migration of a real shop, or payment/receipt hardware. Your existing local inventory database was not used for these tests. The full two-computer checklist is in [SHOP_SETUP.md](SHOP_SETUP.md).
