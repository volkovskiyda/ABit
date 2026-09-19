# Releasing

**GitHub Releases is the only delivery channel.** There is no Play Store listing and no Firebase App
Distribution: every build anyone installs comes off the Releases page, and Firebase Hosting serves
the web app. That is a deliberate choice with one real cost, recorded under *What no store costs*
below — read it before wondering where the update prompt is.

Two channels, and they answer different questions.

**Continuous** is for "is the current state of main any good?" — every green push to `main`
publishes a **release** carrying all four platforms, and updates the web app, with no manual step.
Nothing here is a pre-release, so `releases/latest` is the newest main build.

**Curated** is for "what shipped?" — a pushed `v*` tag publishes the same four platforms under a
`v<version>` title, after a second Test Lab matrix on three devices `ci.yml` never runs.

Neither requires editing a version anywhere. See *Versioning* in the [README](../README.md).

Both build their binaries through the same reusable workflow, `binaries.yml`, and compose their
notes through the same script, `scripts/publish-release.sh`. The two channels differ in their tag
and their title, and the curated one in what it was tested on. They do not differ in what they
contain.

## Continuous — a release per main push

Handled by `ci.yml`'s `version` → `binaries` → `publish` chain, on `main` pushes only, and only
after the checks, the instrumented suite, the Firestore rules tests and Test Lab have all passed.

It tags the commit `build-<versionCode>` and publishes a full release named for the full version, so
`releases/latest` resolves to the newest main build and a link to "the latest version" follows
`main`. A `v*` tag is therefore "latest" only until the next push lands; link to a curated release by
its own tag URL, `releases/tag/v1.3`, which never moves.

One release per build, and never a single rolling release that overwrote itself: the Releases page is
the archive of every main build — including that build's R8 mappings, which are the only way to read
a crash report from it, and which a rolling release would throw away the moment N+1 landed.

The `publish` job also deploys the web app and the Firestore rules together. Those two ship as one
step deliberately: a rules change that a client change depends on must never lag behind it. Hosting
is served the bytes out of `abit-web-V.zip` rather than a rebuild, so what is deployed and what is
downloadable are the same build and not merely the same commit.

One consequence to expect: a `build-*` tag per main push, so the tag list grows with the history.
Everything that reads tags — `scripts/release.sh`, both `version` jobs — passes `--match 'v*'` for
exactly that reason.

## Curated — a GitHub Release

```sh
scripts/release.sh
```

It refuses to run on a dirty tree or a branch that is not `origin/main`, asks for a version name,
checks the format, tags and pushes. `release.yml` does the rest.

Versions are `MAJOR.MINOR` with a non-zero major — `1.0`, `1.3`, `2.0`. That is not a style
preference: `jpackage` rejects anything else for a DMG, and the desktop build derives its package
version from the tag.

## What a release carries

The same assets on both channels, for version `V` (the latest tag plus the commit count):

| Asset | What to do with it |
|---|---|
| `abit-V.apk` | The phone app. Installs over an older build in place — same key, higher `versionCode`. |
| `abit-wear-V.apk` | The watch app. There is no tester app on Wear OS, and no way onto a watch but `adb` — [INSTALL.md](INSTALL.md#wear-os) is the procedure the release notes link to. |
| `ABit-V.dmg` | The macOS menu-bar app. Unsigned until the Developer Program is paid — see below. |
| `abit-web-V.zip` | The web bundle, byte for byte what Hosting is serving. |
| `mapping-android-V.txt`, `mapping-wear-V.txt` | R8 mappings. Without the file from that exact build, a crash report from it cannot be read. Crashlytics holds its own copy; these are for stack traces pasted into an issue. |

The notes are composed by `scripts/publish-release.sh`: the commit subjects since the previous
published tag of either kind, a compare link, and the table above. They are built from `git log`
rather than left to `gh --generate-notes`, because generated notes list merged pull requests and
this history is mostly commits pushed straight to `main` — the generated section would be empty
exactly when it is most needed.

## What no store costs

Nothing that is already installed learns that a new release exists. Play normally supplies that; off
Play, each platform has to be told.

- **Phone.** The APK installs over itself, so updating is a download and a tap. Anyone who wants it
  automated can point [Obtainium](https://github.com/ImranR98/Obtainium) at this repository.
- **Wear OS.** `adb install -r` and nothing else. There is no tester app on Wear OS, Firebase App
  Distribution does not support it, and the legacy "embedded wear app inside the phone APK" route
  was removed by Google years ago. A watch only updates when someone plugs it in.
- **macOS.** A DMG per release, downloaded by hand.
- **Web.** Updates on reload, like any site.

If the watch app ever needs to update by itself, the only supported route is a Play Console internal
testing track — which does not require a public listing, and so does not hit the twelve-tester rule
that gates production access for personal accounts.

### The unsigned DMG

macOS refuses to open an unsigned app on a double click. A tester right-clicks it and chooses **Open**
once; after that it launches normally. The release notes say so automatically while the Apple secrets
are absent.

Signing and notarization are already wired and gated on three secrets existing. When the Apple
Developer Program is paid for, adding them is the whole change — no code moves:

```sh
# Export "Developer ID Application: <name>" from Keychain Access as a .p12 first.
base64 -i certificate.p12 | gh secret set APPLE_CERT_P12_BASE64
gh secret set APPLE_CERT_PASSWORD           # the password you set when exporting
gh secret set APPLE_SIGNING_IDENTITY        # e.g. "Developer ID Application: Dmitriy Volkovskiy (TEAMID)"
gh secret set NOTARIZATION_APPLE_ID         # your Apple ID email
gh secret set NOTARIZATION_PASSWORD         # an app-specific password from appleid.apple.com
gh secret set NOTARIZATION_TEAM_ID          # the 10-character team id
```

With `NOTARIZATION_TEAM_ID` present the workflow runs `notarizeReleaseDmg` instead of
`packageReleaseDmg`, submits the bundle to Apple and waits for the ticket.

The gate is on the secret being **non-empty**, not on the environment variable existing. GitHub
Actions defines `APPLE_SIGNING_IDENTITY` for a secret that was never set, with an empty value, so a
plain `Provider.isPresent` reads as "configured" on every runner: signing turns on with an empty
identity and `createReleaseDistributable` fails with `Could not find certificate for '' in keychain
[]` — nowhere near the secret it is actually missing. `app/desktop/build.gradle.kts` runs all four
Apple variables through `envOrAbsentIfBlank`, which maps blank back to absent. A new gated secret
belongs in that helper too, never in a bare `environmentVariable`.

### The minified DMG

Both tasks build the `release` build type, which is the one Compose Desktop runs ProGuard over before
jlink bundles a JRE: 107 MB unminified, 86 MB shipped. The keep rules are
`app/desktop/compose-desktop.pro`, and every rule there names the framework that needed it.

Two things about that file are worth knowing before touching it. Obfuscation is **off** — the
repository is public, so renaming hides nothing while breaking every framework in the app that
resolves a class by name. The ProGuard **optimizer** is off too, and that one is not a preference: it
rewrote `okio.Okio.sink(Socket)` into bytecode the JVM verifier rejects, and the packaged app died on
startup with `VerifyError: Bad type on operand stack`.

A missing keep rule does not fail the build — it fails the app, on whichever code path needed the
class. That is why `ci.yml` builds `packageReleaseDmg` on every push rather than leaving the minified
bundle to be built for the first time at tag time. After changing a dependency or a keep rule, run
the packaged app, not just the task:

```sh
./gradlew :app:desktop:createReleaseDistributable
./app/desktop/build/compose/binaries/main-release/app/ABit.app/Contents/MacOS/ABit
```

The bundled JRE's module list is `:app:desktop:suggestRuntimeModules`, verbatim. Re-run it whenever a
dependency with native or reflective code arrives — a module missing there fails the same way a
missing keep rule does.

## One-time setup

Everything below is already done for `abit-kmp`. It is written down so it can be redone.

### Repository secrets

Run these from a checkout that has the real files, and never paste their contents anywhere:

```sh
base64 -i keystore.properties              | gh secret set KEYSTORE_PROPERTIES_BASE64
base64 -i abit-release.jks                 | gh secret set KEYSTORE_BASE64
base64 -i app/shared/kotzilla.json         | gh secret set KOTZILLA_JSON_BASE64
base64 -i google-services.json             | gh secret set GOOGLE_SERVICES_JSON_BASE64
gh secret set FIREBASE_SERVICE_ACCOUNT < firebase-ci.json && rm firebase-ci.json
base64 -i oauth.properties                 | gh secret set OAUTH_PROPERTIES_BASE64
```

`scripts/restore-secrets.sh` puts all of them back on a runner, at the paths the build reads them
from. It fails loudly on a missing one rather than shipping a build that is quietly wrong — a build
with no Kotzilla key reports no sessions and uploads no mapping, which is the right behaviour for a
pull request and the wrong one for a release.

All five are required. `OAUTH_PROPERTIES_BASE64` was the one exception while the Desktop-app client
it carries did not exist; it stopped being one the day that client was created, because from then on
a DMG that silently cannot sign in is a regression rather than the state of the project.

### Google sign-in: the two OAuth clients

Both exist on `abit-kmp` now. Both needed the **OAuth consent screen** configured first, in the
Google Cloud console — an interactive step, which is why this is written down rather than scripted.

Google Auth Platform → Clients → Create client, twice:

| Type | Used by | Where it goes |
|---|---|---|
| **Web application** | Android, Wear (as Credential Manager's `serverClientId`) and the browser | Regenerate `google-services.json` from the Firebase console — the plugin turns it into the `default_web_client_id` resource the Android apps look up. The same id is committed as `FirebaseConfig.WEB_OAUTH_CLIENT_ID` for the web app. Its authorised JavaScript origins are `https://abit-kmp.web.app`, `https://abit-kmp.firebaseapp.com` and `http://localhost:8080`; a browser sign-in from an origin missing there fails with nothing in the UI, only a GIS error in the console. |
| **Desktop app** | The macOS app's loopback flow | `oauth.properties` at the repository root, git-ignored — see `.example.oauth.properties`. It needs no redirect URI of its own: a Desktop-app client accepts any `http://127.0.0.1` port, which is what the flow binds. |

The desktop client's secret is shipped inside the DMG on purpose. Google's own documentation says an
installed app's secret is not treated as confidential — it cannot be — and PKCE is what actually
secures the exchange. Using the **web** client for the desktop flow instead would put a genuinely
confidential secret in a file anyone can download, which is why there are two clients rather than
one.

### The signing keystore

```sh
keytool -genkeypair -v -keystore abit-release.jks -storetype PKCS12 -alias abit \
  -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=ABit, O=<you>, C=<cc>"
```

Then write `keystore.properties` beside it (see `.example.keystore.properties`) and register the
certificate's SHA-1 **and** SHA-256 on the `ABit` Firebase app, or Google sign-in will not work in
release builds.

> **Back up the keystore and its password somewhere you will still have them in five years.** Losing
> them means no future build can ever update an already-installed app.

### The CI service account

```sh
gcloud services enable iam.googleapis.com toolresults.googleapis.com testing.googleapis.com \
  firebasehosting.googleapis.com --project=abit-kmp

gcloud iam service-accounts create firebase-ci --project=abit-kmp --display-name=firebase-ci

gcloud projects add-iam-policy-binding abit-kmp --condition=None \
  --member=serviceAccount:firebase-ci@abit-kmp.iam.gserviceaccount.com --role=roles/editor

gcloud iam service-accounts keys create firebase-ci.json --project=abit-kmp \
  --iam-account=firebase-ci@abit-kmp.iam.gserviceaccount.com
```

**Why `roles/editor` and not something narrower.** The Test Lab job writes to the free results bucket
Firebase provides, and gcloud requires `roles/editor` on the principal to use it. The narrower
`roles/cloudtestservice.testAdmin` only suffices alongside `roles/firebase.analyticsViewer` *and* a
results bucket you own — a bucket, a lifecycle rule and a workflow flag, to narrow a key already
confined to one throwaway project. Editor also covers Hosting and the rules deploy, so one binding
serves every job that touches Firebase.

`toolresults.googleapis.com` is easy to miss: Test Lab stores every run's results through it, so the
job fails without it even though `testing.googleapis.com` is on. `firestore.googleapis.com` is the
other one — if it is not already enabled, the CLI enables it mid-deploy, then concludes the database
must be missing and tries to create one, which the service account cannot do and should not be able
to do.

For the same reason `firebase.json`'s `firestore` block carries only `rules` and `indexes`. Adding
`database` or `location` back tells the CLI it owns the database's lifecycle, and a deploy then
checks for it. The database is provisioned once, by a human, not by CI.

Nothing else is needed on the Firebase side. Firebase App Distribution was used once and is not any
more: binaries go to GitHub Releases, which needs no account, no tester group and no invitation. If
it is ever reinstated, the API to enable is `firebaseappdistribution.googleapis.com` and the group
alias — not the display name — is what a workflow passes to `--groups`.

## Test Lab quota

The Spark plan allows **10 virtual and 5 physical device executions per Pacific day**, counted per
device in the matrix.

| Workflow | Devices | Cost per run |
|---|---|---|
| `ci.yml` on a `main` push | `MediumPhone.arm@30`, `MediumPhone_ps16k.arm@36`, physical `tokay@36` | 2 virtual + 1 physical |
| `release.yml` on a tag | `MediumPhone.arm@30`, `MediumTablet.arm@35`, physical `tangorpro@36` | 2 virtual + 1 physical |

That is five pushes to `main` in a day before either bucket runs dry, and both run out together — a
fourth device in either matrix would cost two pushes a day. A tag only runs dry on a day that already
saw four pushes.

Pull requests spend no quota at all: their instrumented run is on a Gradle-managed emulator on a
free runner. Check the live numbers with:

```sh
gcloud alpha services quota list --service=testing.googleapis.com --project=abit-kmp
gcloud firebase test android models list --project=abit-kmp   # what is available at all
```

## When a run fails

- **Test Lab out of quota.** The tag exists and nothing published. Re-run the job the next Pacific
  day and the release follows; the tag never needs to move.
- **`gh release create` failed after the Hosting deploy succeeded.** Safe to re-run: a deploy is
  idempotent, which is exactly why it comes first.
- **`gh release create` says the release already exists.** The run got all the way through once.
  Delete the release *and* its tag before re-running, or the second attempt will keep failing —
  `gh release delete build-<n> --cleanup-tag`.
- **The tag is not on main.** The `guard` job refuses it before spending any quota. Move the tag and
  push again.
- **A check job failed on a pull request.** Reproduce it locally with `scripts/run-tests.sh` — the
  job runs that same script, with no secrets restored.
