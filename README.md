# Inventory Management System

JavaFX desktop inventory application. The existing source tree remains under `InventorySystemi/src`; `main.Main` starts JavaFX and opens the login screen. It uses an embedded H2 database by default, so an installer does not require a MySQL server.

## Current project state

The original project was an Eclipse-only Java 21 project. Its `.classpath` referenced JavaFX and MySQL JARs under one developer's Windows `C:/Users/...` directory, and `DBConnection.java` contained a root password. It had no schema setup script or repeatable build. It already had English and Turkish property files, but startup forced Turkish and multiple UI and receipt strings were hardcoded. The Turkish properties file was not valid UTF-8. The sale service grouped updates in a transaction but did not protect against concurrent overselling.

The Maven build uses Java release 21, JavaFX 21, H2, and retains MySQL Connector/J for optional external/legacy databases. `main.Main` is the entry point. English is the default. Language selection is available on login and in the dashboard; selecting a language rebuilds the dashboard immediately. On first launch, create the initial administrator account on the login screen. The current language files cover most dashboard keys, but several dialogs, receipt strings, number formats and reports still contain hardcoded Turkish text, so localization is not yet complete.

## Requirements and database setup

Install JDK 21, Maven 3.9+, VS Code, and the VS Code extensions recommended by `InventorySystemi/.vscode/extensions.json`. No database server or environment variables are required for a normal install. H2 creates the database automatically and stores it persistently at `~/.inventorysystem/inventory` on macOS/Linux or `%USERPROFILE%\\.inventorysystem\\inventory` on Windows. The app creates only missing tables; it does not drop or reset data. Each computer has its own inventory and sales data. This is local storage, not a shared cloud database.

For an existing MySQL installation, its data is not automatically copied into H2. Back up the MySQL database and export/import records as a separate migration before switching. To keep using MySQL, set `DB_URL`, `DB_USER`, and `DB_PASSWORD` to a configured server; `database/schema.sql` is the additive MySQL schema. The first-run H2 database is initialized by the application itself.

For optional MySQL only, copy `.env.example` to `.env`, uncomment/configure the values, and keep `.env` untracked. The app reads database settings from environment variables or matching Java system properties. Never store real credentials in committed files.

The current login implementation compares the supplied password directly with the database `password` field. Existing accounts therefore need the schema/application's current plain text password format. Password hashing should be implemented as a separate migration before professional deployment.

## VS Code on macOS

1. Install JDK 21 (for example, a current Temurin 21 distribution) and Maven 3.9+.
2. Install the recommended Java extensions when VS Code prompts, then open `InventorySystemi` as the workspace folder.
3. In VS Code's Java runtime settings, select JDK 21. JavaFX and Connector/J are managed by Maven; do not add machine-specific JAR paths.
4. No database setup is needed for local H2. On first launch, select **Create first administrator**, choose a username and a password of at least 8 characters, then sign in.
5. In the integrated terminal, run `mvn clean package` to build and `mvn javafx:run` to start. To debug, select **Run and Debug → Inventory System** from `.vscode/launch.json`.

### First-run smoke test

1. Start with `mvn javafx:run`. On the first run, choose **Create first administrator**. Enter a username such as `demo_admin` and a password of at least 8 characters; there is no shared default password.
2. Sign in with the account you just created. The role is `admin`.
3. In **Products**, add a disposable product, for example name `Test item`, category `Test`, quantity `12`, price `2.50`, and supplier `Local test`.
4. In **Sales**, add a sale for 2 units. Refresh the product list and confirm quantity is 10. This checks basic UI/database connectivity; it does not validate overselling or concurrency.
5. Close and relaunch the app, sign in again, and confirm the product still exists. The persistent data file is under your user profile. To discard test data, use the app's delete functions; do not remove that database file if it contains real records.

## Tests

From `InventorySystemi`, run `mvn test`. Tests verify English/Turkish resource loading and UTF-8 Turkish characters, and create/reinitialize an isolated in-memory H2 schema while checking that existing rows remain. They do not exercise sales, authentication, reports, CSV or JavaFX interaction.

## Packaging

Native packages must be built and tested on their target operating system and architecture. This repository is currently being edited in a Linux environment, so no macOS `.app`/`.dmg` or Windows `.msi`/`.exe` has been built or tested here. Packaging scripts are provided at `InventorySystemi/scripts/package-macos.sh` and `InventorySystemi/scripts/package-windows.ps1`; each invokes Maven and `jpackage` with the target platform's JavaFX dependencies and bundled runtime. On a Mac, install JDK 21, Maven 3.9+, and run `bash scripts/package-macos.sh` from `InventorySystemi`. On Windows, install JDK 21 with `jpackage`, Maven 3.9+, and run `powershell -ExecutionPolicy Bypass -File scripts/package-windows.ps1` from `InventorySystemi`. Review the produced app image and exercise it against a disposable database before distributing it. The packaging commands themselves remain unverified and may need platform-specific JavaFX launcher tuning. A Windows runner, VM, or Windows CI job is required for a Windows installer. Signing and notarization require Apple developer credentials; unsigned macOS builds may trigger Gatekeeper warnings.

Packaging still needs an end-to-end platform-specific input/runtime assembly and launch verification. H2 is included as a Java dependency; each installed copy keeps its own database file. Optional MySQL remains external.

## Known functional gaps to address

- `SaleService` needs conditional stock updates and row-count checks to prevent overselling, plus service-level validation and propagated failures.
- Authentication currently stores/checks plain text passwords and broad exception handling hides database failures.
- Database errors across services are often only printed to stderr; report and table failure paths need consistent user-visible handling.
- Several screens, receipts and reports remain Turkish-only despite the language selector.
- Existing receipt files in `receipts/` are user data and must be preserved.
- No dedicated test database or end-to-end MySQL/JavaFX test suite is configured.
