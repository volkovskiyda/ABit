#!/usr/bin/env bash
# Launches the packaged macOS app and fails if the ProGuard shrink broke it.
#
# `packageReleaseDmg` proves the bundle *builds*. It cannot prove the bundle *runs*, and the gap
# between the two is where this project has already lost two keep rules: sqlite-jdbc's JNI callbacks
# and protobuf-lite's reflected message fields, both reached by name, both invisible to ProGuard's
# analysis, and both fatal only once Firestore touches local persistence. Every released DMG before
# this script existed opened its tray icon and then silently failed to sync.
#
# Nothing else in the build can catch that class of bug. `:app:desktop:run` and the desktop UI tests
# both run against an unshrunk classpath, so they are green by construction; a packaged build is the
# only place the shrink exists. This is therefore the one check that reads the artifact that ships.
#
# The app is launched exactly as a user would launch it, on a machine that has never run it. That
# machine is signed out — nothing signs in on its own, and a signed-out app reads and writes
# nothing in the real abit-kmp project — so the store this reads for is opened deliberately at
# launch, by `initFirebase` in app/shared's desktopMain, rather than by the sync a signed-in user
# would start. That is what makes a fresh runner able to prove anything: without it the app
# started, printed one line, and sat there.
#
# What a signed-out launch proves is the sqlite-jdbc half. The protobuf half is exercised only when
# a listener is allocated, which takes a Google-linked user; a run on a developer's own Mac, where
# one is signed in, covers it, and a CI runner does not.
set -euo pipefail

cd "$(dirname "$0")/.."

APP=${ABIT_APP:-app/desktop/build/compose/binaries/main-release/app/ABit.app/Contents/MacOS/ABit}
WATCH_SECONDS=${ABIT_SMOKE_SECONDS:-60}

if [ ! -x "$APP" ]; then
  echo "No packaged app at $APP."
  echo "Build one first: ./gradlew :app:desktop:createReleaseDistributable"
  exit 1
fi

LOG=$(mktemp -t abit-smoke)
trap 'rm -f "$LOG"' EXIT

echo "Launching $APP for ${WATCH_SECONDS}s"
"$APP" >"$LOG" 2>&1 &
APP_PID=$!

# A tray app never exits on its own, so the watch is a fixed wait rather than a `wait`. Poll instead
# of sleeping through it: a bundle that dies early should be reported as the crash it is rather than
# waited out in full.
for _ in $(seq "$WATCH_SECONDS"); do
  kill -0 "$APP_PID" 2>/dev/null || break
  sleep 1
done

if kill -0 "$APP_PID" 2>/dev/null; then
  kill "$APP_PID" 2>/dev/null || true
  sleep 1
  kill -9 "$APP_PID" 2>/dev/null || true
  EXITED_EARLY=false
else
  EXITED_EARLY=true
fi

report() {
  echo
  echo "--- what the app printed ---"
  cat "$LOG"
}

if [ "$EXITED_EARLY" = true ]; then
  echo "::error::The packaged app exited on its own. A tray app should still be running."
  report
  exit 1
fi

# Anything ProGuard removed surfaces as one of these. `Exception in thread` rather than a bare
# `Exception`, because the healthy log names GooglePlayServicesNotAvailableException in passing —
# the JVM Firebase SDK is a port of the Android one and says so on every start.
if grep -nE 'Exception in thread|NoClassDefFoundError|ClassNotFoundException|Internal error in Cloud Firestore' "$LOG"; then
  echo "::error::The packaged app threw on startup — see the lines above. A missing keep rule in app/desktop/compose-desktop.pro is the usual cause."
  report
  exit 1
fi

# Positive evidence, not merely the absence of a stack trace. This line is Firestore reading its
# local store, which means sqlite-jdbc's native library loaded with every JNI callback it resolves
# by name — the first of the two things that were broken, and the one a signed-out launch reaches.
# Without it the app started but never got as far as the code this script exists to exercise, and
# a silent pass would be worse than a failure.
if ! grep -q 'SQLiteCursor received count' "$LOG"; then
  echo "::error::The app started but Firestore never opened its local store, so this proved nothing."
  echo "If the Firebase SDK changed that log line, update the marker in this script rather than dropping the check."
  report
  exit 1
fi

echo "The packaged app runs: Firestore opened its local store and nothing threw."
