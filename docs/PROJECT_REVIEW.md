# Release scope — Inventory System 1.3.0

COODIC LABS's offline desktop application has Apple Silicon macOS and Windows x64 distributions. Guarded native builders are available for Intel macOS and Linux x86_64; those packages have not yet been built or execution-tested. See PACKAGING.md and VERIFICATION.md. Installed use needs no developer tools, internet, database server, payment network or cloud account.

The application includes a graphical welcome page, required local legal acceptance, first-administrator/recovery setup, searchable shop currency, optional percentage tax, staff permissions, piece/integer-gram stock, live sale pricing, immutable financial snapshots, transactional checkout/receipt records, reporting by original currency, admin/staff KPI dashboards and native JavaFX charts, FIFO stock purchases with immutable cost allocations, optional batch prices and scoped gross-profit periods, reasoned voids, backups, audit history, password controls, English/Turkish UI, rotating logs and branded native packages.

A currency change reinterprets current product prices with an explicit warning; it performs no exchange calculation. Older sales without recorded currency remain marked ?. Reports never add unlike currencies. Tax is a user-configured calculation aid and is rounded once per checkout. Source currency metadata: Java currency API and the current ISO 4217 register, bundled locally.

Limits: one local database per OS profile; no synchronization or merging independently modified copies. Card selection does not process payment. Hardware scales, camera scanners, fiscal devices and automatic refunds are not implemented. Operating-system/database file access is outside application role protection.

Unknown opening costs are never invented: profit is explicitly incomplete and uses only fully costed lines. Operating expenses are not tracked. Staff profit requires admin opt-in and is limited to their sales. Runtime branding is vector-based; native package icons stay unchanged. All features remain Java/JavaFX with local H2 storage.

## Tax and Legal

Receipts are simple sales records, not tax or fiscal invoices. Shop owners must follow local tax and fiscal-device rules. Terms/Privacy are drafts requiring lawyer review, including the governing-country placeholder and redistribution/source-availability obligations for third-party software. The application cannot be described as legally approved for every country.

Further deployment work: actual Windows acceptance, testing on each target Mac/Windows version and peripheral device, signing/notarization, installer reputation and legally reviewed commercial terms. See VERIFICATION.md for measured checks and unresolved items.
