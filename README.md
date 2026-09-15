# ABit

**Change a bit.** A schedule-driven focus chime that follows you from your phone to your watch to
your Mac's menu bar to a browser tab, and sounds on all four at the same moment.

It is not a pomodoro. Nothing is started or stopped by hand: you write a schedule once and the app is
ambient after that.

## How it works

A **schedule** is a name, some weekdays, a start and an end time, a focus length and a break length —
`Workdays, Mon–Fri, 09:00–18:00, 45 min focus, 15 min break`. Times are **local wall clock plus
weekday, never instants**, so 09:45 stays 09:45 when you travel.

The **blocks are derived, never stored**: focus from the start time, then alternating focus and break
until the end time. The last focus is cut at the end and gets no break after it. A **session** is one
focus block plus the break that follows it — that is what the ring counts down and what "Session 3 of
9" numbers.

Every signed-in device chimes at every boundary, unless that device is told not to (a per-device
setting that deliberately does not sync). The only interventions are **Pause today**, **Skip next**
— which silences one boundary without changing the plan — and the per-schedule switches.

**One schedule runs a day.** Schedules may overlap; when two enabled ones do, the app marks the
conflict and asks which stays on. Until you answer, the most recently updated schedule plans the day,
so a schedule arriving from another device never makes the day go silent.

Schedules and today's overrides sync through Firestore when you are signed in with Google. Without an
account everything still works and stays on the device.

## What runs where

| | Android | Wear OS | macOS | Web |
|---|---|---|---|---|
| UI | Compose, Material 3 | Compose for Wear | Compose Desktop, menu bar | Compose for Web (wasm) |
| Local storage | Room | Room | Room | Room, in memory |
| Sync | Firestore | Firestore | Firestore | Firestore |
| Sign-in | anonymous, Google | anonymous | anonymous | anonymous |
| Crash reporting | Crashlytics | Crashlytics | — | — |
| Performance | Firebase Performance | Firebase Performance | — | — |
| Koin insight | Kotzilla | Kotzilla | Kotzilla | Kotzilla |
| Ships via | App Distribution, GitHub | GitHub, sideload | GitHub, DMG | Firebase Hosting |

Business logic is shared; **the UI is written per platform**. A watch face, a menu-bar popup and a
phone screen are different products, and pretending otherwise produces something that is nobody's
first choice.

## Module map

```
core/common          dispatchers, clock, logging, app scope
core/model           the types every layer speaks
core/domain          the planner, the conflict rules, repository interfaces — no implementations
core/database        Room 3; bundled SQLite on JVM targets, a Web Worker in the browser
core/datastore       preferences on Okio; localStorage in the browser
core/auth            Firebase Auth through GitLive
core/sync            Firestore document layout and the remote source
core/data            repositories, and the sync engine that reconciles local with remote
core/observability   crash reporting and tracing, as interfaces
core/testing         fakes, dispatchers, and the Firebase emulator harness
core/designsystem    the palette, bundled Inter, the session ring and the shared components
core/chime           the boundary maths and each platform's alarm

feature/today/api        navigation keys and types another feature may depend on
feature/today/impl       what the ring, the countdown and the two interventions render from
feature/schedules/{api,impl}   the list, the editor and the conflict resolution
feature/settings/{api,impl}    per-device chime settings, permissions, theme

app/shared     the composition root every app calls
app/android    app/wear    app/desktop    app/web
baselineprofile         profile generator and startup macrobenchmark
build-logic             convention plugins — where the build's decisions live
```

A feature's `api` module holds only what another feature needs to navigate to it. Nothing may depend
on an `impl`, except `app:shared`.

## Building

Needs a JDK (Gradle provisions 21 itself), the Android SDK, and — for the web and Firebase tooling —
Node. Run `git lfs install` once: the screenshot goldens and the baseline profile are LFS objects.

```sh
./gradlew :app:android:assembleDebug              # Android phone app
./gradlew :app:wear:assembleDebug                 # Wear OS app
./gradlew :app:desktop:run                        # macOS menu-bar app
./gradlew :app:web:wasmJsBrowserDevelopmentRun    # web app on localhost:8080
```

### Building without secrets

**A fresh clone builds and runs with no credentials at all.** That is a rule this project keeps, not
an accident: every pull request from a fork, and every check job in CI, builds exactly that way. Four
files are git-ignored, and the build degrades rather than failing when each is absent:

| Missing file | What happens | Where the real one comes from |
|---|---|---|
| `app/android/google-services.json`, `app/wear/…` | The Google Services plugin prints a warning. Firebase never initialises, so sync and sign-in report themselves unavailable and the app keeps everything on the device. | Firebase console → Project settings → your app → `google-services.json`. `app/android/google-services.example.json` shows the shape. |
| `app/shared/kotzilla.json` | The Kotzilla plugin disables itself. No sessions are reported and no mapping is uploaded. | [console.kotzilla.io](https://console.kotzilla.io). `.example.kotzilla.json` is the template. |
| `keystore.properties` and `abit-release.jks` | `assembleRelease` produces an **unsigned** APK instead of failing. | Generated once; `.example.keystore.properties` is the template. |

`debug.keystore` **is** committed, deliberately: it makes every machine and CI sign debug builds with
the same certificate, which is what the Firebase API key restriction is pinned to.

## Testing

```sh
scripts/run-tests.sh              # static analysis, unit, desktop UI, screenshot goldens
scripts/run-tests.sh --managed    # ... plus instrumented tests on an emulator Gradle boots itself
scripts/run-tests.sh --emulator   # ... plus the Firestore rules tests on the Firebase emulators
scripts/run-tests.sh --all        # everything
```

Layer by layer:

- **Unit tests** live in `commonTest` and run twice, on the desktop JVM and on the Android host JVM,
  from the same sources. `./gradlew desktopTest testAndroidHostTest`.
- **`nonAndroidTest`** is for tests that need a real platform: Room's bundled SQLite ships a JNI
  library an Android host test cannot load, so database tests run on the desktop JVM instead.
- **Compose UI tests** run on Android against a Gradle-managed emulator, and on the desktop JVM
  through Skiko, which needs no display server.
- **Screenshot tests** render `@Preview` composables through LayoutLib and diff them against
  committed PNGs. Re-bake with `./gradlew :app:android:updateDebugScreenshotTest` and look at the
  diff before committing it.
- **Firestore rules** are tested by attempting the access for real against the local emulator —
  including one user trying to read another's data, which must be refused.

## Static analysis

```sh
./gradlew detekt ktlintCheck lint   # all three, no baselines anywhere
./gradlew ktlintFormat              # fixes most formatting findings in place
```

**No baselines, ever.** A finding fails the build; fix the finding. A genuine third-party false
positive is scoped in that module's `lint.xml` by artifact name, which survives version bumps, rather
than recorded in a baseline that has to be regenerated on every one.

Formatting comes from `.editorconfig`, which is ktlint's own `ktlint_official` style at 140 columns.
Point Android Studio at it (Settings → Editor → Code Style → Enable EditorConfig support) so the IDE
and the build agree.

## Versioning

Nothing in this repository is edited per release:

```
versionCode = git rev-list --count HEAD
versionName = <latest tag without its "v">.<versionCode>
```

So tag `v1.3` at commit 348 ships as `1.3.348` with version code `348`. One monotonic code is shared
by App Distribution builds and tagged releases, so neither can install backwards over the other. A
local build passes neither property and stays at `1` / `1.0`.

Cut a release with `scripts/release.sh`. Everything else is [docs/RELEASING.md](docs/RELEASING.md).

## Sync

Sessions live at `users/{uid}/sessions/{id}` in Firestore, and `firestore.rules` lets a user read and
write only their own subtree — everything outside `/users` is denied outright.

Reads always come from the local database, so a screen renders without waiting for the network. The
sync engine reconciles the two in the background, **last write wins on `updatedAt`**. That is the
whole conflict policy, chosen rather than defaulted to: a session records something that already
happened, so two devices editing one is rare and the loser is a correction rather than lost work.
Deletions are deliberately not propagated — without tombstones, a row missing on one side is
indistinguishable from one the other side has not seen yet.

Anonymous sign-in is the front door: someone can use ABit with no account, and their sessions stay on
the device. Signing in with Google **links** that anonymous account rather than replacing it, so the
history already there survives and starts syncing.

## Observability

Crashlytics and Firebase Performance are Android-only, and collect **only in release builds** — a
developer's crashes and a debug build's timings would otherwise pollute the numbers a release is
judged on. Kotzilla watches the Koin graph on every platform, under a separate app for debug builds
so development sessions stay out of production data.

## Docs

- [docs/RELEASING.md](docs/RELEASING.md) — how a build reaches a tester, and the one-time setup
- [docs/BASELINE-PROFILE.md](docs/BASELINE-PROFILE.md) — regenerating the startup profile
- [CLAUDE.md](CLAUDE.md) — orientation for an AI assistant working in this repository
