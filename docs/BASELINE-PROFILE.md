# Baseline profile

A list of the classes and methods the app runs on its way to the first screen. Android compiles them
ahead of time at install, instead of interpreting them on first launch, which is worth a visible
fraction of cold start on a mid-range phone.

It lives at `app/android/src/release/generated/baselineProfiles/`, is **committed**, and is tracked
in Git LFS. A release build packages whatever is checked in — it never generates one, because a
release that needed an emulator could not be built in CI.

## What you need

Nothing, if you use the workflow. Locally: the Android SDK and enough disk for a system image. The
managed device downloads and boots itself; there is no AVD to create and no device to plug in.

## Generate

Preferred, and reviewable:

```
Actions → Baseline profile → Run workflow
```

It generates on a Gradle-managed Pixel 6 (API 35, AOSP), and if the result differs it opens a pull
request. The profile is a 24,000-line artefact that changes every user's first launch, so it lands
through review rather than by being pushed.

Locally, if you want to see it now:

```sh
./gradlew :app:android:generateReleaseBaselineProfile
```

Three minutes or so on an Apple Silicon Mac. It builds a non-minified release, boots the emulator,
runs `BaselineProfileGenerator`, and writes both files.

`baseline-prof.txt` and `startup-prof.txt` are currently identical, and that is expected: the journey
is startup only, so the startup profile and the full profile cover the same ground. They will diverge
when the generator learns a longer journey.

## Verify before committing

```sh
# 1. Plausible size and shape. Mostly AndroidX and Compose, a few hundred lines of our own.
wc -l app/android/src/release/generated/baselineProfiles/baseline-prof.txt   # ~24k
grep -c volkovskiyda app/android/src/release/generated/baselineProfiles/baseline-prof.txt   # ~400

# 2. It is an LFS object, not a 2.5 MB blob in the pack.
git check-attr filter -- app/android/src/release/generated/baselineProfiles/baseline-prof.txt

# 3. It actually reaches the APK.
./gradlew :app:android:assembleRelease
unzip -l app/android/build/outputs/apk/release/*.apk | grep dexopt   # assets/dexopt/baseline.prof

# 4. A release build must never start an emulator.
./gradlew :app:android:assembleRelease   # no device boots; automaticGenerationDuringBuild = false
```

**A much smaller profile is the failure to watch for.** It usually means the generator stopped at the
splash screen instead of waiting for real content, and the profile then covers the framework's
startup but none of the app's. That is why the generator waits for the text "ABit" rather than for
the window.

## Measuring whether it earns its place

```sh
./gradlew :baselineprofile:pixel6Api35BenchmarkReleaseAndroidTest
```

`StartupBenchmark` measures cold start twice, with no ahead-of-time compilation and with the profile
applied. The pair is the point: absolute numbers depend on the device, but the difference between
them is what the profile is worth.

Run it on real hardware for numbers worth quoting. An emulator's timings are too noisy to compare
across runs, which is why no workflow runs the benchmarks automatically.

The profiled variant uses `CompilationMode.Partial(BaselineProfileMode.Require)` — `Require`, not
`Enable`, so a missing profile fails the run rather than quietly measuring the same thing twice and
reporting no improvement.

## When to regenerate

When the **shape** of startup changes: a different first screen, a new dependency resolved at launch,
a library swapped out under the composition root. Not once per release — a handful of times a year.

## The `.benchmark` variants

The baseline-profile plugin generates two build types, and both carry a `.benchmark` application id
suffix:

- `nonMinifiedRelease` — what the generator profiles. R8 is forced **off** for it, because a profile
  recorded against obfuscated names is silently useless. The plugin only clears the legacy
  `isMinifyEnabled` flag, which this project never sets (R8 comes from the release type's
  `optimization` block), so `build-logic` turns it off explicitly.
- `benchmarkRelease` — what the macrobenchmarks measure. Deliberately left minified: it exists to be
  release-like.

The suffix is load-bearing. Without it a generation run installs *over* the release build on the
device and the connected-test teardown then uninstalls it, taking that install's data with it. The
suffix costs a client entry in `google-services.json` — hence the `ABit Benchmark` app in the Firebase
project — and cannot reach the profile, which is a list of names in a namespace that does not change.

## When it goes wrong

| Symptom | Cause |
|---|---|
| `generateReleaseBaselineProfile` fails on a device that will not install the APK | The non-minified variant came out unsigned. `build-logic` falls back to the debug keystore when release signing is absent; check that fallback still applies. |
| The profile is a few hundred lines | The generator did not reach real content. Check the `Until.hasObject` wait in `BaselineProfileGenerator`. |
| `expandReleaseArtProfileWildcards` fails in CI | The checkout did not fetch LFS and the profile is a pointer file. Every job that builds a release needs `lfs: true`. |
| The benchmark reports no improvement | `CompilationMode.Partial` without `Require` will silently skip a missing profile. It is set to `Require`; if that changed, change it back. |
