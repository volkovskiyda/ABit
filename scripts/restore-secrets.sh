#!/usr/bin/env bash
# Recreates the git-ignored build inputs on a CI runner from the base64 repository secrets, at the
# paths the build reads them from. Used by the delivery jobs only — never by the check jobs, which
# are meant to prove that a checkout with no credentials still builds.
#
# Fails loudly if any secret is missing rather than shipping a build that is quietly wrong:
#   - without keystore.properties the signing config is never created and the APK is unsigned;
#   - without kotzilla.json the Kotzilla plugin disables itself, so the build reports no sessions
#     and uploads no mapping, leaving every crash from it unsymbolicated;
#   - without google-services.json Firebase never initialises, so the app ships with sync switched
#     off and no crash reporting at all.
# Each of those is the right behaviour for a pull-request checkout and the wrong one here.
set -euo pipefail

cd "$(dirname "$0")/.."

: "${KEYSTORE_PROPERTIES_BASE64:?set the KEYSTORE_PROPERTIES_BASE64 repository secret}"
: "${KEYSTORE_BASE64:?set the KEYSTORE_BASE64 repository secret}"
: "${KOTZILLA_JSON_BASE64:?set the KOTZILLA_JSON_BASE64 repository secret}"
: "${GOOGLE_SERVICES_JSON_BASE64:?set the GOOGLE_SERVICES_JSON_BASE64 repository secret}"

printf '%s' "$KEYSTORE_PROPERTIES_BASE64" | base64 -d > keystore.properties
printf '%s' "$KEYSTORE_BASE64"            | base64 -d > abit-release.jks
# Under app/shared/, where the Kotzilla plugin looks — it reads the file from the module it is
# applied to, and that is the module that calls monitoring().
printf '%s' "$KOTZILLA_JSON_BASE64"       | base64 -d > app/shared/kotzilla.json
# The same file for both Android apps: they share one application id, so they share one client.
printf '%s' "$GOOGLE_SERVICES_JSON_BASE64" | base64 -d > app/android/google-services.json
cp app/android/google-services.json app/wear/google-services.json

echo "Restored keystore.properties, abit-release.jks, app/shared/kotzilla.json and both google-services.json files"
