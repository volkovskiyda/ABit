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
setting that deliberately does not sync). The only interventions are **Skip today** — which silences
the rest of the day without changing the plan — and the per-schedule switches.

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
| Sign-in | anonymous, Google | anonymous, Google | anonymous, Google | anonymous, Google |
| Crash reporting | Crashlytics | Crashlytics | — | — |
| Performance | Firebase Performance | Firebase Performance | — | — |
| Koin insight | Kotzilla | Kotzilla | Kotzilla | Kotzilla |
| Ships via | GitHub Releases | GitHub Releases, [sideload](docs/INSTALL.md#wear-os) | GitHub Releases, DMG | Firebase Hosting |

Business logic is shared; **the UI is written per platform**. A watch face, a menu-bar popup and a
phone screen are different products, and pretending otherwise produces something that is nobody's
first choice.

## Install

Every platform ships from the [Releases page](https://github.com/volkovskiyda/ABit/releases), except
the web app, which is always live at <https://abit-kmp.web.app>. ABit is on no app store, so nothing
updates itself and the watch app has to be sideloaded over `adb` — that is the only way onto a Wear
OS watch without Google Play.

[**docs/INSTALL.md**](docs/INSTALL.md) has the steps for all four, the Gatekeeper dance for the
unsigned DMG, and what to do when `adb` refuses.

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

### Hot reload on the desktop app

```sh
./gradlew :app:desktop:hotRun --auto    # start the tray app; every save re-composes it
./gradlew reload                        # or reload by hand, without --auto
```

[Compose Hot Reload](https://github.com/JetBrains/compose-hot-reload) replaces the edit-build-relaunch
loop with a recomposition, which matters most here because the app under test is a menu-bar item
whose state (a running schedule, a countdown) is tedious to get back to after every restart. The
plugin provisions a JetBrains Runtime the first time it runs — enhanced class redefinition is a JBR
feature, and the toolchain the rest of the build uses does not have it — so the first `hotRun`
downloads a JDK and later ones do not.

It is applied to `:app:desktop` and to nothing else, and no packaging task goes near it: `run`,
`packageDmg` and `packageReleaseDmg` build the same bytes with or without the plugin.

### Building without secrets

**A fresh clone builds and runs with no credentials at all.** That is a rule this project keeps, not
an accident: every pull request from a fork, and every check job in CI, builds exactly that way. Five
files are git-ignored, and the build degrades rather than failing when each is absent:

| Missing file | What happens | Where the real one comes from |
|---|---|---|
| `google-services.json` | The Google Services plugin prints a warning. Firebase never initialises, so sync and sign-in report themselves unavailable and the app keeps everything on the device. | Firebase console → Project settings → your app → `google-services.json`. One file at the repository root serves both Android apps; `.example.google-services.json` shows the shape. |
| `app/shared/kotzilla.json` | The Kotzilla plugin disables itself. No sessions are reported and no mapping is uploaded. | [console.kotzilla.io](https://console.kotzilla.io). `.example.kotzilla.json` is the template. |
| `keystore.properties` and `abit-release.jks` | `assembleRelease` produces an **unsigned** APK instead of failing. | Generated once; `.example.keystore.properties` is the template. |
| `oauth.properties` | The macOS app packages and runs; its popover reports Google sign-in unavailable and anonymous sign-in carries it. | Google Cloud console → Credentials → OAuth client ID → **Desktop app**. `.example.oauth.properties` is the template. |

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

Every run leaves one page behind at `build/reports/test-summary/index.html`: every layer's totals,
the three analysis tools, and — when something failed — the modules it failed in with the cases
named. `scripts/run-tests.sh` writes it last, on a failing run too; `./gradlew testSummary` rebuilds
it from whatever is already on disk without running a thing.

A row reads **not run** rather than "0 passed" when the layer produced no XML at all. That
distinction is the point of the page: a layer skipped for want of a device is not a layer that
passed, and a summary that could not tell them apart would report a green wall for a run that
tested a third of the project.

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
by the pre-release every main push publishes and by the tagged releases, so neither can install
backwards over the other. A local build passes neither property and stays at `1` / `1.0`.

Every green push to `main` publishes a pre-release on the [Releases page][releases] carrying all four
platforms and a changelog; a `v*` tag publishes the same four as the full release that
`releases/latest` points at. There is no Play Store listing and no Firebase App Distribution — which
means nothing updates itself except the web app. Cut a release with `scripts/release.sh`; everything
else, including what going store-free costs, is [docs/RELEASING.md](docs/RELEASING.md).

[releases]: https://github.com/volkovskiyda/ABit/releases

## Sync

Sessions live at `users/{uid}/sessions/{id}` in Firestore, and `firestore.rules` lets a user read and
write only their own subtree — everything outside `/users` is denied outright.

Reads always come from the local database, so a screen renders without waiting for the network. The
sync engine reconciles the two in the background, **last write wins on `updatedAt`**. That is the
whole conflict policy, chosen rather than defaulted to: a session records something that already
happened, so two devices editing one is rare and the loser is a correction rather than lost work.
Deletions are deliberately not propagated — without tombstones, a row missing on one side is
indistinguishable from one the other side has not seen yet.

Anonymous sign-in is the front door: someone can use ABit with no account, and their schedules stay
on the device. Signing in with Google **links** that anonymous account rather than replacing it, so
the history already there survives and starts syncing.

When the Google account already exists as a separate Firebase user the link is refused — one uid
cannot be made out of two — and ABit signs into the existing account and discards the anonymous one,
which held nothing in Firestore anyway. The schedules made anonymously are still on the device, and
the first sync afterwards merges them **in the account's favour**: where both sides know a schedule
the account keeps its own copy, and anything only this device has is added. Last-write-wins is
deliberately not used for that one pass, because its loser would be an account someone has been
using on their phone for months, beaten by a throwaway identity that happened to be edited more
recently.

Google sign-in reaches all four platforms through **one seam**: each platform obtains an id token its
own way and `core:auth` is the single place that exchanges it for a Firebase user and does the
linking. The phone and the watch use Credential Manager; the Mac opens the system browser and
listens on a loopback port for the redirect, which is the flow Google documents for a desktop app;
the browser uses Google Identity Services. Nothing platform-specific reaches `core:auth` but the
token.

The project has **two** OAuth clients, and which platform uses which is the only asymmetry. The
phone, the watch and the browser share the Web application client: Android resolves it out of
`google-services.json` as `default_web_client_id`, and the browser reads the same id from
`FirebaseConfig.WEB_OAUTH_CLIENT_ID`. Its authorised JavaScript origins are what gate it —
`https://abit-kmp.web.app`, `https://abit-kmp.firebaseapp.com` and `http://localhost:8080` for the
development server — so a new origin is a console change, not a code one. The Mac has a **Desktop
app** client of its own in `oauth.properties`, because an installed app cannot keep the web client's
genuinely confidential secret inside a downloadable DMG.

A checkout without `oauth.properties` still builds, packages and runs; the popover says sign-in is
unavailable and anonymous sign-in carries the Mac, which is what a fork's pull request gets.

## Observability

Crashlytics and Firebase Performance are Android-only, and collect **only in release builds** — a
developer's crashes and a debug build's timings would otherwise pollute the numbers a release is
judged on. Kotzilla watches the Koin graph on every platform, under a separate app for debug builds
so development sessions stay out of production data.

Which platform a session came from is reported as a prefix on the version — `android-1.0-debug`,
`wear-1.0`, `desktop-1.0`, `web-1.0` — because Kotzilla groups sessions by app and by version and
has no platform facet of its own. One app per platform would have given the same separation and
taken away the comparison worth having: the graph is one graph, and "is this binding slow
everywhere, or only on the Mac?" is a question four dashboards cannot answer. The phone and the
watch need it most, since they share an application id and a version and are otherwise
indistinguishable. Each app's entry point passes its own `AbitPlatform` to `initKoin`, and the
Android apps pass `BuildConfig.VERSION_NAME` with it, so AGP's `versionNameSuffix` stays the one
place `-debug` is spelled.

## Docs

- [docs/RELEASING.md](docs/RELEASING.md) — how a build reaches a tester, and the one-time setup
- [docs/BASELINE-PROFILE.md](docs/BASELINE-PROFILE.md) — regenerating the startup profile
- [CLAUDE.md](CLAUDE.md) — orientation for an AI assistant working in this repository
