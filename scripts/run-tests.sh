#!/usr/bin/env bash
#
# run-tests.sh
#
# Runs every check in the project, prints one summary at the end, and leaves an HTML page behind
# (`:testSummary`) that aggregates every layer's results and all three analysis tools:
#
#   1. Static analysis  (detekt, ktlintCheck, lint)                 host-side, always
#   2. Unit tests       (desktopTest, testAndroidHostTest)          host-side, always
#   3. Desktop UI tests (:app:desktop:test)                         host-side, always
#   4. Screenshot       (:app:android:validateDebugScreenshotTest)  host-side, always
#   5. Instrumented     (managed emulator, or a connected device)   opt in, see below
#   6. Emulator suite   (scripts/emulator-tests.sh)                 opt in, needs Node
#
# The HTML page lands at build/reports/test-summary/index.html and reports on whichever layers ran —
# a layer nobody ran reads "not run" rather than "0 passed".
#
# Layers 1 to 4 need nothing but a JDK, so this is safe to run anywhere — including on a pull-request
# runner with no emulator. That is why the instrumented and Firebase layers are opt in rather than
# skipped-by-default: a layer that quietly does nothing reads as a pass, and this script would rather
# be asked.
#
# Usage:
#   scripts/run-tests.sh                 layers 1-4
#   scripts/run-tests.sh --managed       ... plus the instrumented suite on a Gradle-managed emulator
#                                        (downloads and boots it; no AVD setup needed)
#   scripts/run-tests.sh --connected     ... plus the instrumented suite on an attached device
#   scripts/run-tests.sh --emulator      ... plus the Firebase Emulator Suite tests
#   scripts/run-tests.sh --all           every layer
set -uo pipefail

cd "$(dirname "$0")/.."

run_managed=false
run_connected=false
run_emulator=false

for arg in "$@"; do
  case "$arg" in
    --managed)   run_managed=true ;;
    --connected) run_connected=true ;;
    --emulator)  run_emulator=true ;;
    --all)       run_managed=true; run_emulator=true ;;
    -h|--help)   sed -n '2,29p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *)           echo "Unknown option: $arg (try --help)" >&2; exit 2 ;;
  esac
done

results=()
failed=0

layer() {
  local name="$1"; shift
  echo
  echo "── $name ─────────────────────────────────────────────"
  if "$@"; then
    results+=("PASS  $name")
  else
    results+=("FAIL  $name")
    failed=1
  fi
}

layer "Static analysis" ./gradlew detekt ktlintCheck :app:android:lintDebug :app:wear:lintDebug
layer "Unit tests" ./gradlew desktopTest testAndroidHostTest
layer "Desktop UI tests" ./gradlew :app:desktop:test
layer "Screenshot goldens" ./gradlew :app:android:validateDebugScreenshotTest

if [ "$run_managed" = true ]; then
  layer "Instrumented (managed emulator)" ./gradlew ciGroupDebugAndroidTest
fi

if [ "$run_connected" = true ]; then
  # The convention plugin's onlyIf skips these with a log line when nothing is attached, so this
  # cannot fail merely for want of a device.
  layer "Instrumented (connected device)" ./gradlew connectedDebugAndroidTest
fi

if [ "$run_emulator" = true ]; then
  layer "Firebase emulator suite" scripts/emulator-tests.sh
fi

echo
echo "── Summary ───────────────────────────────────────────"
printf '%s\n' "${results[@]}"

# Always, and `|| true` on purpose: the page is most useful after a failing run, and a summary task
# that could itself fail the script would be one more thing to debug when something is already
# broken. It only reads XML that is already on disk, so it never makes a layer run.
echo
./gradlew testSummary --console=plain -q || true

exit "$failed"
