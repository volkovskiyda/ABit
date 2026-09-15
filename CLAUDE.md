# CLAUDE.md

Orientation for an AI assistant working in this repository. Read the [README](README.md) for what the
project is; this file is about how to work in it without undoing decisions that were made carefully.

## Commands

```sh
scripts/run-tests.sh            # static analysis, unit, desktop UI, screenshot goldens
scripts/run-tests.sh --all      # ... plus managed-emulator and Firebase emulator layers
./gradlew detekt ktlintCheck lint
./gradlew ktlintFormat          # run this before assuming a ktlint finding needs a code change
./gradlew desktopTest testAndroidHostTest       # the same commonTest sources, on both JVM hosts
./gradlew ciGroupDebugAndroidTest               # instrumented, on an emulator Gradle boots itself
scripts/emulator-tests.sh                       # Firestore rules, against the local emulators
```

Watching a CI run:

```sh
gh run list --limit 1 --json databaseId,status,name,headBranch,createdAt
gh run watch <run-id> --exit-status --compact --interval 10
```

## Decisions that are not open

Each of these cost something to arrive at. Change one only on purpose, and update this file when
you do.

- **Kotlin is pinned to 2.4.10.** The Kotzilla Gradle plugin (2.3.6) registers its Compose
  instrumentation through the K1 `ComponentRegistrar` interface that Kotlin 2.4.20 deleted. Bumping
  Kotlin breaks the build in a way whose error message does not mention Kotzilla.
- **DI is Koin, not Metro or Hilt.** Kotzilla reads the Koin graph; that is what it is for.
- **The Kotzilla plugin is applied per module, never at the root project.** Root application fails
  configuration with "The value for property 'languageVersion' is final". The vendor documents the
  per-module path as the supported alternative.
- **UI is written per platform.** There is no shared *screen* and there should not be one.
  `core:designsystem` holds the tokens, the bundled Inter, the theme and the shared components (the
  session ring, the timeline row, the schedule card) — four independent copies of one arc calculation
  is a worse problem than the one the original rule avoided.
- **Features split `api` / `impl`.** Nothing may depend on an `impl` except `app:shared`.
- **A checkout with no credentials must build and run.** Signing, Kotzilla and Firebase each degrade
  rather than fail when their git-ignored config file is absent. Every check job in CI proves this on
  every push. If a change would make a missing file fatal, that is the change that is wrong.
- **No analysis baselines.** Not detekt, not lint, not ktlint. A finding gets fixed. A genuine
  third-party false positive gets a scoped `<ignore regexp="artifact-name">` in that module's
  `lint.xml`, which survives version bumps.
- **Schedules are wall-clock plus weekday, never instants.** A schedule says "09:00 on Mondays", not
  "this epoch second". Someone reaching for `Instant` out of habit would make the chimes shift by the
  time-zone offset the moment the user travels, which is the one thing the model exists to prevent.
  `LocalClock` in `core:common` is how a caller gets today's local date and time.
- **Sync is last-write-wins on `updatedAt`, and *hard* deletions do not propagate.** Both are
  deliberate; the reasoning is in `SyncEngine`'s KDoc. A schedule is not hard-deleted: it is deleted
  by stamping `deletedAt`, which syncs like any other edit and is filtered out of every read.
- **Crashlytics and Performance collect in release builds only.**

## Layout

`core/*` holds shared logic — including `core:designsystem`, the one Compose-enabled library, and
`core:chime`, which owns the boundary maths and each platform's alarm. `feature/{today,schedules,
settings}/{api,impl}` holds the three destinations, `app/{shared,android,wear,desktop,web}` holds the
four apps plus their composition root, and `build-logic` holds the convention plugins.

Dependency direction: `app:*` → `app:shared` → `feature:*:impl` → `core:*`. A `core` module never
depends on a feature, and `core:domain` never depends on an implementation.

Build configuration belongs in `build-logic`, not in a module's build file. A module's
`build.gradle.kts` should be a `plugins {}` block and the projects it depends on. If you find
yourself repeating configuration in two modules, that is a convention plugin.

## Source sets worth knowing

- `commonTest` runs on both JVM hosts.
- `nonAndroidTest` is for tests needing a real platform — Room's bundled SQLite ships a JNI library an
  Android *host* test cannot load, so database tests live here and run on the desktop JVM.
- `nonAndroidMain` is desktop + web; `jvmSharedMain` is Android + desktop.
- `app/shared/src/kotzilla{Enabled,Disabled}` is a seam: exactly one is compiled, depending on whether
  `kotzilla.json` exists. The two must stay signature-identical, and no build compiles both, so only
  running both compiles proves it.

## Secrets

`keystore.properties`, `*.jks`, `google-services.json` and `kotzilla.json` are git-ignored and each
has a committed `.example.*` twin. **Never commit one, never print its contents, never paste a key
into a commit message or a PR description.** `scripts/restore-secrets.sh` is how CI gets them.

`debug.keystore` *is* committed on purpose — it gives every machine the same debug SHA-1, which the
Firebase API key restriction is pinned to.

## Working notes

`internal/` is git-ignored working space: plans, scratch notes, screenshots. The infrastructure plan
that built this project is `internal/plan/20260914-infrastructure-plan/`, and its `00-overview.md`
records every locked decision with its reasoning — read it before re-deciding anything.

## Commits

Imperative mood with a `type:` prefix, matching the existing history (`feat:`, `build:`, `test:`,
`ci:`, `docs:`, `perf:`). **No trailers and no attribution of any kind** — no `Co-Authored-By`, no
session links, no generated-with footers.

When asked to change something already committed, put the change in a **separate commit whose message
starts with `Fix`**; never amend or rewrite the original.

## External services

- Firebase project `abit-kmp`. `.claude/settings.json` sets `CLOUDSDK_CORE_PROJECT`, so `gcloud`
  needs no `--project` flag here.
- Kotzilla apps `ABit` (release) and `ABit Debug` (debug, and everything non-Android).
- GitHub `volkovskiyda/ABit`, public.
