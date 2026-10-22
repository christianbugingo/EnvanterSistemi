# Inventory Management System

This is the existing JavaFX desktop inventory application. The only Maven project is in `InventorySystemi/`; its source remains in `InventorySystemi/src`, and `main.Main` starts the login window. The application uses embedded H2 by default and retains optional MySQL settings through `DB_URL`, `DB_USER`, and `DB_PASSWORD`.

## Requirements

- JDK 21 (the project compiles with `--release 21`; use JDK 21 to run JavaFX and package the app).
- Maven 3.9.x is downloaded by the included Maven Wrapper on first use. A globally installed Maven is not required.
- VS Code Java, Java Debugger, and Maven extensions are recommended in `.vscode/extensions.json`.

The current Codespace is Linux x86_64 with Microsoft OpenJDK 25. It can compile and run tests targeting Java 21, but it is not your Intel Mac and does not provide an interactive desktop display. Use a local JDK 21 on macOS for the actual desktop GUI. `uname -m` reports `x86_64` for Intel or `arm64` for Apple Silicon; Maven selects the JavaFX native dependencies for the current build host.

## Build and run from the repository root

The root-level wrapper delegates into the one existing Maven project; no second POM is maintained:

```sh
./mvnw clean package
./mvnw test
./mvnw javafx:run
```

On Windows, use `mvnw.cmd clean package`, `mvnw.cmd test`, or `mvnw.cmd javafx:run`. From inside `InventorySystemi/`, use `./mvnw ...` on macOS/Linux or `mvnw.cmd ...` on Windows. The wrapper pins Maven 3.9.9.

In VS Code, open the repository root. Use **Tasks: Run Task → Run Inventory System (JavaFX)** to start it, **Test Inventory System** for tests, or **Run and Debug → Inventory System (JavaFX)** to debug. Select an installed JDK 21 through the Java extension/runtime selector; no machine-specific Java path is committed.

On a Mac, run `./mvnw javafx:run` from the repository root in Terminal or VS Code. This should show the native JavaFX login window when run in the local graphical session. A compile/package success only verifies compilation and tests; a Maven JavaFX launch without a visible display does not verify that an interactive window appeared.

## Database and data safety

H2 initializes missing tables without dropping existing tables or rows. Its persistent database is stored at `~/.inventorysystem/inventory` on macOS/Linux and `%USERPROFILE%\\.inventorysystem\\inventory` on Windows. No database server is needed. Tests use isolated in-memory databases; builds and tests do not reset the real database. Receipt files in `InventorySystemi/receipts/` are retained.

MySQL is optional. Configure credentials through environment variables or an untracked `.env` file. MySQL data does not automatically migrate to H2; back it up and use a separate, explicit migration process if needed. Never put real credentials in Git.

## Tests and GUI limitations

`./mvnw test` covers additive database initialization/persistence, English and Turkish resource loading (including UTF-8), sale stock deduction, and oversell rollback. The sale stock update is conditional and transaction-bound so concurrent requests cannot decrement stock below zero. The Codespace has no `DISPLAY`, Xvfb, or X server. Do not treat a headless JavaFX launch failure (`Unable to open DISPLAY`) as a compile failure or a passed GUI test. For interactive testing, launch on the local Mac; no privileged Xvfb/system package installation is required.

## Packaging

Native packages must be built on their target OS with JDK 21 and `jpackage` so the runtime and JavaFX native libraries match that platform and architecture:

- macOS Intel or Apple Silicon: from `InventorySystemi/`, run `bash scripts/package-macos.sh`. It produces an `.app` image and `.dmg` under `InventorySystemi/dist/` when run on macOS. The script rejects non-macOS hosts and requires active JDK 21.
- Windows: from `InventorySystemi/`, run `powershell -ExecutionPolicy Bypass -File scripts/package-windows.ps1`. It produces an app image and `.msi` under `InventorySystemi/dist/` when run on Windows with JDK 21 and `jpackage`.

The installer app image bundles a Java runtime; the database remains in the user's profile, outside the installation directory. The packaging scripts are prepared but have not been executed on macOS or Windows in the Linux Codespace. Therefore neither native installer is claimed as built or tested. Test each package on the target OS with a disposable account/data set. macOS signing/notarization is not configured.

## Known application limitations

Password storage is currently plaintext; replacing it requires a backwards-compatible login-time migration and has not been implemented. Several database failures elsewhere in the UI/services still use stderr logging, and some dialogs/reports/receipts still contain hardcoded Turkish text. These should be addressed in follow-up changes without invalidating existing accounts or removing application features.
