#!/usr/bin/env bash
set -euo pipefail
# Never enable shell tracing: no credentials or key material belong in logs.
cd "$(dirname "$0")/.."
export PYTHONPYCACHEPREFIX="$(pwd)/target/notary-pycache"
: "${MAC_SIGN_IDENTITY:?Set MAC_SIGN_IDENTITY to a Developer ID Application identity already in Keychain}"
: "${MAC_NOTARY_PROFILE:?Set MAC_NOTARY_PROFILE to an existing notarytool Keychain profile}"
[[ "$(uname -s)" == Darwin && "$(uname -m)" == arm64 ]] || { echo 'Native Apple Silicon macOS is required.' >&2; exit 2; }
[[ "$MAC_SIGN_IDENTITY" == 'Developer ID Application: '* ]] || { echo 'Developer ID Application identity required.' >&2; exit 2; }
# Explicit prerequisites: fail before making any staging/output changes.
identities=$(security find-identity -v -p codesigning)
printf '%s\n' "$identities" | grep -F -- "\"$MAC_SIGN_IDENTITY\"" >/dev/null || { echo 'Requested valid signing identity is absent.' >&2; exit 2; }
xcrun notarytool history --keychain-profile "$MAC_NOTARY_PROFILE"
if [[ -z "${JAVA_HOME:-}" ]]; then
  if [[ -d ../.tools/jdk/Contents/Home ]]; then export JAVA_HOME="$(cd ../.tools/jdk/Contents/Home && pwd)";
  else export JAVA_HOME="$(/usr/libexec/java_home -v 21)"; fi
fi
export PATH="$JAVA_HOME/bin:$PATH"
settings=$(java -XshowSettings:properties -version 2>&1)
[[ "$(printf '%s\n' "$settings" | awk '$1=="java.version" {print $3}')" == 21.* ]] || { echo 'JDK 21 required.' >&2; exit 2; }
jdk_arch=$(printf '%s\n' "$settings" | awk '$1=="os.arch" {print $3}')
python3 scripts/package_target.py host macos-arm64 "$jdk_arch"
version=$(python3 scripts/metadata.py version)
stem="InventorySystem-$version-macos-arm64"
source_app="${MAC_SIGN_SOURCE_APP:-$(pwd)/dist/macos/$stem.app}"
[[ -d "$source_app" ]] || { echo "Existing arm64 app missing: $source_app" >&2; exit 2; }
python3 scripts/package_target.py natives macos-arm64 "$source_app"
# A unique staging directory means failed attempts preserve the existing app and DMG.
mkdir -p target ../.tools/tmp ../.tools/build-home
work=$(mktemp -d "$(pwd)/target/notarize-arm64-XXXXXXXX")
mkdir "$work/release" "$work/evidence"
app="$work/release/$stem.app"
dmg="$work/release/$stem.dmg"
export TMPDIR="$(cd ../.tools/tmp && pwd)"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Duser.home=$(cd ../.tools/build-home && pwd) -Djava.io.tmpdir=$TMPDIR"
exec > >(tee "$work/evidence/sign-and-notarize.log") 2>&1
trap 'result=$?; if [[ $result -ne 0 ]]; then echo "SIGNING/NOTARIZATION FAILED ($result). Prior release retained in dist/ or previous-release archive. Evidence: $work/evidence" >&2; fi' EXIT
echo "Staging: $work"
echo "JDK: $JAVA_HOME"
java -version
# Copy only app content, never a shop database or Keychain item.
ditto "$source_app" "$app"
python3 scripts/sign_macos_components.py "$app" packaging/macos/entitlements.plist
codesign --verify --deep --strict --verbose=2 "$app"
codesign --display --verbose=4 "$app"
# jpackage's DMG bundler re-signs its copied app ad hoc. Preserve our Developer ID signatures.
mkdir "$work/dmg-content"
ditto "$app" "$work/dmg-content/InventorySystem.app"
hdiutil create -volname 'Inventory System' -srcfolder "$work/dmg-content" -format UDZO -fs HFS+ "$dmg"
# Check the actual mounted bytes/signatures before uploading; never trust only the source app.
python3 - "$dmg" "$work/preflight-mount" "$app" <<'PY'
from pathlib import Path
import os,subprocess,sys
image,mount,source=map(Path,sys.argv[1:]);mount.mkdir()
subprocess.run(['hdiutil','attach','-readonly','-nobrowse','-noautoopen','-mountpoint',str(mount),str(image)],check=True)
try:
    copied=mount/'InventorySystem.app'
    subprocess.run(['diff','-qr',str(source/'Contents'),str(copied/'Contents')],check=True)
    subprocess.run(['codesign','--verify','--deep','--strict','--verbose=2',str(copied)],check=True)
    detail=subprocess.check_output(['codesign','--display','--verbose=4',str(copied)],stderr=subprocess.STDOUT,text=True)
    print(detail)
    if 'Authority='+os.environ['MAC_SIGN_IDENTITY'] not in detail or 'Timestamp=' not in detail or '(runtime)' not in detail:
        raise RuntimeError('Mounted app lost Developer ID, timestamp or hardened runtime')
finally:
    subprocess.run(['hdiutil','detach',str(mount)],check=True)
PY
codesign --force --sign "$MAC_SIGN_IDENTITY" --timestamp "$dmg"
codesign --verify --strict --verbose=2 "$dmg"
hdiutil verify "$dmg"
# Machine-readable output contains submission metadata, never a password.
set +e
xcrun notarytool submit "$dmg" --keychain-profile "$MAC_NOTARY_PROFILE" --wait --output-format json > "$work/evidence/submission.json"
submission_result=$?
set -e
cat "$work/evidence/submission.json"
submission_id=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["id"])' "$work/evidence/submission.json")
xcrun notarytool log "$submission_id" --keychain-profile "$MAC_NOTARY_PROFILE" "$work/evidence/notary-log.json"
status=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["status"])' "$work/evidence/submission.json")
[[ $submission_result -eq 0 && "$status" == Accepted ]] || { cat "$work/evidence/notary-log.json"; echo 'Notarization not accepted. Fix reported issues and rerun; release not replaced.' >&2; exit 1; }
xcrun stapler staple "$dmg"
xcrun stapler validate "$dmg"
# Also staple the standalone app for direct app-bundle distribution.
xcrun stapler staple "$app"
xcrun stapler validate "$app"
codesign --verify --deep --strict --verbose=2 "$app"
spctl --assess --type open --context context:primary-signature -v "$dmg"
spctl --assess --type execute -v "$app"
# The verifier mounts this DMG, checks the copied original and launches without changing its signed contents.
python3 scripts/verify-signed-macos.py "$dmg" "$work/evidence"
# Record evidence before publication so a logging/copy failure also preserves the old release.
mkdir -p ../docs/evidence/notarization
cp "$work/evidence/"* ../docs/evidence/notarization/
# Replace local artifacts only after every check; archive the old signed release instead of deleting it.
python3 scripts/finalize_macos_release.py "$work/release" "$work/evidence"
echo "SUCCESS: submission $submission_id. Installed local $stem.app, $stem.dmg and checksum; previous release archived."
