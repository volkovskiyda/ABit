# Releasing

Two channels, and they answer different questions.

**Continuous** is for "is the current state of main any good?" — every green push to `main` reaches
testers on Android and updates the web app, with no manual step.

**Curated** is for "what shipped?" — a pushed `v*` tag builds every platform, publishes a GitHub
Release with the binaries and their R8 mappings, and offers testers the same build under the tag's
name.

Neither requires editing a version anywhere. See *Versioning* in the [README](../README.md).

## Continuous — testers and the web

Handled by `ci.yml`'s `distribute` job, on `main` pushes only, and only after the checks, the
instrumented suite, the Firestore rules tests and Test Lab have all passed.

It restores the secrets, builds a signed release APK, uploads it to Firebase App Distribution for the
`testers` group with the commit subject as release notes, then deploys the web app and the Firestore
rules together. Those two ship as one step deliberately: a rules change that a client change depends
on must never lag behind it.

## Curated — a GitHub Release

```sh
scripts/release.sh
```

It refuses to run on a dirty tree or a branch that is not `origin/main`, asks for a version name,
checks the format, tags and pushes. `release.yml` does the rest.

Versions are `MAJOR.MINOR` with a non-zero major — `1.0`, `1.3`, `2.0`. That is not a style
preference: `jpackage` rejects anything else for a DMG, and the desktop build derives its package
version from the tag.

The release carries, for version `V` (the tag plus the commit count):

| Asset | What to do with it |
|---|---|
| `abit-V.apk` | The phone app. Also goes to App Distribution. |
| `abit-wear-V.apk` | The watch app. Sideload it: `adb install abit-wear-V.apk`. There is no tester app on Wear OS. |
| `ABit-V.dmg` | The macOS menu-bar app. Unsigned until the Developer Program is paid — see below. |
| `mapping-android-V.txt`, `mapping-wear-V.txt` | R8 mappings. Without the file from that exact build, a crash report from it cannot be read. Crashlytics holds its own copy; these are for stack traces pasted into an issue. |

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
# Once the OAuth clients below exist:
base64 -i oauth.properties                 | gh secret set OAUTH_PROPERTIES_BASE64
```

`scripts/restore-secrets.sh` puts all of them back on a runner, at the paths the build reads them
from. It fails loudly on a missing one rather than shipping a build that is quietly wrong — a build
with no Kotzilla key reports no sessions and uploads no mapping, which is the right behaviour for a
pull request and the wrong one for a release.

`OAUTH_PROPERTIES_BASE64` is the one exception: it is optional, because the client it carries does
not exist yet. **Make it required the moment it does** — from then on a DMG that silently cannot
sign in is a regression rather than the state of the project.

### Google sign-in: the two OAuth clients

Every platform's sign-in is written and every one of them reports itself unavailable, because the
project has no OAuth client to ask. Both need the **OAuth consent screen** configured first, in the
Google Cloud console for `abit-kmp` — an interactive step, which is why this is written down rather
than scripted.

APIs & Services → Credentials → Create credentials → OAuth client ID, twice:

| Type | Used by | Where it goes |
|---|---|---|
| **Web application** | Android, Wear (as Credential Manager's `serverClientId`) and the browser | Regenerate `google-services.json` from the Firebase console — the plugin turns it into the `default_web_client_id` resource the Android apps look up. Paste the same id into `FirebaseConfig.WEB_OAUTH_CLIENT_ID` for the web app, and add `https://abit-kmp.web.app` to its authorised JavaScript origins. |
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
  firebaseappdistribution.googleapis.com firebasehosting.googleapis.com --project=abit-kmp

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
confined to one throwaway project. Editor also covers App Distribution and Hosting, so one binding
serves every delivery job.

`toolresults.googleapis.com` is easy to miss: Test Lab stores every run's results through it, so the
job fails without it even though `testing.googleapis.com` is on. `firestore.googleapis.com` is the
other one — if it is not already enabled, the CLI enables it mid-deploy, then concludes the database
must be missing and tries to create one, which the service account cannot do and should not be able
to do.

For the same reason `firebase.json`'s `firestore` block carries only `rules` and `indexes`. Adding
`database` or `location` back tells the CLI it owns the database's lifecycle, and a deploy then
checks for it. The database is provisioned once, by a human, not by CI.

Finally, the tester group the distribute job uploads to:

```sh
firebase appdistribution:group:create "Testers" testers --project abit-kmp
firebase appdistribution:testers:add you@example.com --group-alias testers --project abit-kmp
```

The alias — `testers` — is what both workflows pass to `--groups`, not the display name. Without the
group the upload fails, and it fails at the end of a long job.

## Test Lab quota

The Spark plan allows **10 virtual and 5 physical device executions per Pacific day**, counted per
device in the matrix.

| Workflow | Devices | Cost per run |
|---|---|---|
| `ci.yml` on a `main` push | `SmallPhone.arm@30`, `MediumPhone.arm@36`, physical `tokay@36` | 2 virtual + 1 physical |
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
- **`gh release create` failed after App Distribution succeeded.** Safe to re-run: the upload is
  deduplicated by Firebase, which is exactly why it comes first.
- **The tag is not on main.** The `guard` job refuses it before spending any quota. Move the tag and
  push again.
- **A check job failed on a pull request.** Reproduce it locally with `scripts/run-tests.sh` — the
  job runs that same script, with no secrets restored.
