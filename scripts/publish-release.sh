#!/usr/bin/env bash
# Publishes one GitHub Release from whatever is sitting in artifacts/, and composes its notes.
#
# Both delivery paths call it — ci.yml for the release every green main push produces, and
# release.yml for the curated release a v-tag produces — so that what a release *says* is written
# once. Both publish a full release; the only differences between the two are the tag and the title,
# and those arrive through the environment.
#
# The changelog is composed from `git log` rather than left to `gh --generate-notes`. Generated
# notes list merged pull requests, and this project's history is mostly commits pushed straight to
# main, so a generated section would be empty exactly when it is most needed.
#
#   RELEASE_TAG        the tag to create, at $GITHUB_SHA — v1.3, or build-348
#   RELEASE_TITLE      what the release is called on the page
#   RELEASE_VERSION    the full version, 1.3.348, as it appears in every asset's name
#   PREVIOUS_TAG       the release this one follows; empty means "the whole history"
#   NOTARIZATION_TEAM_ID  present only once the Apple Developer Program is paid for; its absence is
#                         what puts the Gatekeeper warning in the notes
set -euo pipefail

cd "$(dirname "$0")/.."

: "${RELEASE_TAG:?set RELEASE_TAG}"
: "${RELEASE_TITLE:?set RELEASE_TITLE}"
: "${RELEASE_VERSION:?set RELEASE_VERSION}"
PREVIOUS_TAG="${PREVIOUS_TAG:-}"

V="$RELEASE_VERSION"

# %s is the commit subject, which conventional commits already make a changelog line.
RANGE=(HEAD)
if [ -n "$PREVIOUS_TAG" ]; then RANGE=("$PREVIOUS_TAG..HEAD"); fi
CHANGES=$(git log "${RANGE[@]}" --pretty='- %s (%h)')
# Empty when a tag is cut at the commit the previous release already published. Rare, and an empty
# heading reads like a bug rather than like nothing having happened.
if [ -z "$CHANGES" ]; then CHANGES="- Nothing since \`$PREVIOUS_TAG\` — same commit, republished."; fi

NOTES="## What's changed"$'\n\n'"$CHANGES"
if [ -n "$PREVIOUS_TAG" ]; then
  NOTES+=$'\n\n'"**Full changelog**: $GITHUB_SERVER_URL/$GITHUB_REPOSITORY/compare/$PREVIOUS_TAG...$RELEASE_TAG"
fi

# Linked at main, not at this release's own tag. Pinning it to the tag would freeze the instructions
# at the version they described, which sounds right until a build-N release is cleaned up: the
# tag goes with it and every link in its notes 404s. An install page that always resolves is worth
# more here than one that is exactly contemporary with a release nobody can reach any more.
DOCS="$GITHUB_SERVER_URL/$GITHUB_REPOSITORY/blob/main/docs/INSTALL.md"

NOTES+=$'\n\n## Downloads\n\n'
NOTES+=$'| File | What to do with it |\n|---|---|\n'
NOTES+="| \`abit-$V.apk\` | The phone app. Installs over an older build in place. |"$'\n'
NOTES+="| \`abit-wear-$V.apk\` | The watch app, over \`adb\` — there is no tester app and no store on a watch: \`adb install -r abit-wear-$V.apk\`. |"$'\n'
NOTES+="| \`ABit-$V.dmg\` | The macOS menu-bar app. |"$'\n'
NOTES+="| \`abit-web-$V.zip\` | The web app, exactly as deployed to Firebase Hosting. |"$'\n'
NOTES+="| \`mapping-android-$V.txt\`, \`mapping-wear-$V.txt\` | R8 mappings. Without the file from this exact build, a crash report from it cannot be read. |"$'\n'
NOTES+=$'\nInstall steps for all four platforms: '"$DOCS"$'\n'

# An unsigned DMG is a fact someone needs before they download it, not after.
if [ -z "${NOTARIZATION_TEAM_ID:-}" ]; then
  NOTES+=$'\n> The macOS app is **not signed or notarized** yet. macOS will refuse to open it on a double click — right-click the app and choose Open, once.\n'
fi

ARGS=(--title "$RELEASE_TITLE" --notes "$NOTES")
# --target only when the tag has to be created, which is the main-push path: on the tag path the tag
# is what triggered the run, and naming a target for a ref that already exists is at best ignored.
if ! git rev-parse -q --verify "refs/tags/$RELEASE_TAG" >/dev/null; then
  ARGS+=(--target "$GITHUB_SHA")
fi

gh release create "$RELEASE_TAG" artifacts/* "${ARGS[@]}"
