#!/usr/bin/env bash
# Runs the integration tests that need a real Firebase, against the local Emulator Suite rather than
# the live project — so they can assert on security rules and on cross-user isolation without any
# credentials and without leaving data behind.
#
# `emulators:exec` starts Auth and Firestore, exports FIREBASE_AUTH_EMULATOR_HOST and
# FIRESTORE_EMULATOR_HOST into the child process, runs the command, and shuts the emulators down
# afterwards — including when the command fails. Those two variables are also the switch the tests
# themselves read: without them each one reports itself skipped, which is what makes
# `./gradlew desktopTest` safe to run on a machine with no emulator.
#
# Needs Node (for npx) and the JDK the rest of the build uses. The emulators need no Firebase login:
# --project names a project id, it does not contact it.
set -euo pipefail

cd "$(dirname "$0")/.."

exec npx --yes firebase-tools@15 emulators:exec \
  --only auth,firestore \
  --project abit-kmp \
  "./gradlew :core:auth:desktopTest :core:sync:desktopTest --rerun-tasks"
