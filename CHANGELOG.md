# Changelog

## 1.3.0 — 6 November 2026

- Added guarded, isolated native packaging targets and explicit JavaFX profiles for Mac arm64, Intel Mac and Linux x86_64. Arm64 rebuilt; Intel/Linux packages await native build machines. Windows ZIP retained unchanged.
- Added native-library/classifier checks, JDK build logs, Linux launcher/desktop entry templates, target-only cleanup and repeatable build/acceptance documentation.

- Added scoped admin/staff dashboards with KPI status cards, payment pie, daily sales bars, trend line and native vector branding.
- Added dated stock receipts, FIFO purchase-cost allocation, optional batch prices, duplicate-name/unit detection and backed-up opening-batch migration.
- Added currency-separated gross profit periods, unknown-cost disclosure and admin-controlled staff profit visibility.
- Merged product name/barcode search and selection, retained live weight pricing and removed unnecessary kilogram zeros; verified 1–23 kg sales.
- Added the tax-zero hint and canonical `00` handling; extended migration, transaction rollback, permission and desktop UI coverage.

- Unified live/persisted weight pricing and added exact 100/kg examples, 5 g currency rounding and historical weight-price regression coverage.
- Fresh shops open signup first; home no longer shows email/website.
- Currency selection uses a single searchable dropdown.
- Legal pages display styled headings/emphasis without raw Markdown markers.
- Refreshed Mac/Windows packages; reinstall the new app while keeping local data.


- Added COODIC LABS identity, shared packaging metadata, local legal documents/notices, required acceptance and a graphical welcome page.
- Added searchable per-shop ISO currencies, currency-aware precision/formatting, financial snapshots and currency-separated reports. No country-specific currency default remains.
- Added optional 0–100% tax, one calculation per checkout, immutable original rates and conditional subtotal/tax display.
- Simplified sale recording to products, quantities and Cash/Card/Other reporting; removed the former drawer workflow from active use while preserving historical records through backed-up migration.
- Added piece/weight-first entry, integer grams, flexible weight parsing, live pricing and stock safeguards.
- Added BCrypt cost 12, password policy/history, audited lockouts, login-screen password changes and offline one-time administrator recovery.
- Added atomic sale/payment/receipt records, snapshots, retained reasoned voids, backups/restoration, logs, About, shortcuts, focus/empty states, translations and fallback warnings.
- Rebuilt Mac and Windows packages with legal notices and native branding metadata; recorded old generated-file cleanup.

Windows execution, physically disconnected networking, hardware acceptance, signing/notarization and legal approval require separate verification. See docs/VERIFICATION.md.
