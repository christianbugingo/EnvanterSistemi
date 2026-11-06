# Developer ID distribution verification — 6 November 2026

Current local release: submission **574c51af-bf90-46f7-8be8-84d00e28afb6**, Accepted without issues. See [RELEASE-1.3.0.md](RELEASE-1.3.0.md) and [current workflow output](evidence/release-1.3.0/signing-workflow.log). Only Apple notarization was uploaded; the previous signed app/DMG was archived locally rather than deleted. The earlier attempts documented below are historical.


The installed application remains fully offline. Signing and notarization are build/distribution operations only. No certificate files, private keys, account credentials or passwords were added to source or evidence. Public identity labels, Team IDs and submission IDs are metadata, not credentials.

Preflight: `security find-identity -v -p codesigning` found four valid identities, including the requested Developer ID Application identity. `xcrun notarytool history` using the existing Keychain profile succeeded with “No submission history.” No Apple ID or password was requested or exported.

## Submission attempts and fixes

1. `e6e2c89a-71cd-4369-8f01-0bf86fe5df03`: **Invalid**. Apple's log contains 70 findings across 34 native paths: 34 invalid Developer ID findings, 34 missing secure timestamps, and 2 missing hardened-runtime findings (launcher and jspawnhelper). The individually signed staging app verified correctly; the jpackage DMG bundler had replaced its internal copy's native signatures with ad-hoc signatures. **Fix:** build the DMG directly with hdiutil and preserve the signed app using ditto. Before uploading, mount the DMG read-only, compare all Contents files against the signed source, verify the mounted app, and require the Developer ID authority, timestamp and runtime flag. The original distribution stayed unchanged throughout the rejected attempt.
2. `078b7dd4-3248-410d-a981-3eb349dd1462`: **Accepted**, status code 0, “Ready for distribution”; no notarization issues. Both the DMG and standalone app were stapled and validated. Gatekeeper accepted both as “Notarized Developer ID”. The mounted-DMG copy passed signature/Gatekeeper checks, unmodified offline startup, the packaged service workflow and a final signature check. Only then were the original arm64 artifacts replaced.

Full issue paths/messages are in [attempt-1-notary-log.json](evidence/notarization/attempt-1-notary-log.json). There was also a local diagnostic restriction: sandboxed codesign could not resolve authority/entitlements and reported an invalid blob; the identical bundle verified successfully outside the sandbox. All actual signing/notary/Gatekeeper checks use native tools outside that restriction. Python’s default bytecode cache also attempted an outside-project cache write during a syntax check and was denied; PYTHONPYCACHEPREFIX now directs signing-tool caches into project target/.

## Entitlements

Exactly `allow-jit`, `allow-unsigned-executable-memory`, and `disable-library-validation` under `com.apple.security.cs`, all true. They support HotSpot JIT, JVM/native executable-memory compatibility and JNI loading respectively. No App Sandbox, network or debugger entitlement is enabled. See [PACKAGING.md](PACKAGING.md) for the rationale, official references and placeholder-only commands.

## Verification scope

The signed offline verifier launches a copy from the actual mounted DMG, without editing its signed contents, using a disposable project-local home/database and process network denial. An external, non-shipped Java verification agent exercises packaged services and generates its test password in memory without logging it. Manual first-run UI interaction on a fresh downloaded/quarantined Mac, physical Wi-Fi-off testing, other macOS versions, Intel and Linux execution remain separate acceptance checks. No Mac App Store submission is attempted.

## Final command results

| Command/check | Observed output/result |
|---|---|
| `security find-identity -v -p codesigning` | Four valid identities; requested Developer ID Application identity present. No certificate or key exported. |
| `xcrun notarytool history --keychain-profile <PROFILE>` | Initial response: `No submission history.` Authentication succeeded. |
| Inner native signatures | `Signed and verified 41 native items plus nested bundles and outer app.` All use Developer ID, secure timestamp and hardened runtime; executable entitlements are the exact three specified keys. |
| `codesign --verify --deep --strict --verbose=2 <APP>` | `valid on disk`; `satisfies its Designated Requirement`. |
| `codesign --verify --strict --verbose=2 <DMG>` | `valid on disk`; `satisfies its Designated Requirement`. |
| `hdiutil verify <DMG>` | Checksum `VALID`. |
| `notarytool submit ... --wait` (corrected attempt) | `{"id":"078b7dd4-3248-410d-a981-3eb349dd1462","status":"Accepted","message":"Processing complete"}` |
| `notarytool log <ID>` | `status=Accepted`, `statusSummary=Ready for distribution`, `statusCode=0`, no issues. |
| `stapler staple <DMG>` / `<APP>` | `The staple and validate action worked!` |
| `stapler validate <DMG>` / `<APP>` | `The validate action worked!` |
| `spctl --assess --type open --context context:primary-signature -v <DMG>` | `accepted`, `source=Notarized Developer ID`. |
| `spctl --assess --type execute -v <APP>` | `accepted`, `source=Notarized Developer ID`. |
| Read-only mount and copy | DMG mounted under project target/; copied app independently passed signature and Gatekeeper checks. DMG detached after testing. |
| Network-denied signed app | Startup passed without modifying app contents. External test agent passed terms/admin/recovery setup, currency, login, receipt of stock, 230 g sale, receipt, FIFO profit, backup and restore using disposable project-local data. |
| Static/regression tooling checks | Entitlements plutil check, shell syntax and Python compilation passed. Existing target-isolation/architecture packaging tests: 9 passed. Application Java code was unchanged; the previous 58-pass/one-skipped Java suite was not rerun for re-signing. |

Full command output: [successful workflow](evidence/notarization/attempt-2-full-workflow.log), [first attempt](evidence/notarization/attempt-1-signing.log), [submission result](evidence/notarization/submission.json), [Apple issue/ticket log](evidence/notarization/notary-log.json), [offline result](evidence/notarization/signed-offline.json). The complete paths/outputs for all individual native signing commands are in the workflow logs. Rejection issue paths are retained without truncation in the first-attempt JSON.

Final artifacts (same distribution paths, replaced only after success):

- `/Users/a/Documents/EnvanterSistemi/InventorySystemi/dist/macos/InventorySystem-1.3.0-macos-arm64.app`
- `/Users/a/Documents/EnvanterSistemi/InventorySystemi/dist/macos/InventorySystem-1.3.0-macos-arm64.dmg`

All changed file paths, artifact paths (including retained rejected/intermediate candidates) and hashes are in [files-and-artifacts.txt](evidence/notarization/files-and-artifacts.txt). Rejected candidates are diagnostic staging artifacts only and must not be distributed. Windows, Intel/Linux status and application code are unchanged. Live data, Downloads and /Applications were not modified. No Mac App Store submission was made.
