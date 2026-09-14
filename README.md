# ABit

Change a bit. A Kotlin Multiplatform pomodoro timer that syncs across an Android phone, a Wear OS
watch, a macOS menu-bar app and the web.

**Infrastructure in progress.** Business logic is shared through Kotlin Multiplatform; the UI is
written per platform. The full README, release runbook and baseline-profile runbook arrive in
`docs/` once the infrastructure plan lands.

## Building

Requires JDK 21 (auto-provisioned by Gradle) and the Android SDK.

```sh
./gradlew :androidApp:assembleDebug     # Android phone app
./gradlew :desktopApp:run               # macOS desktop app
./gradlew :sharedLogic:allTests         # shared logic tests on every target
```

A fresh clone builds with no secrets: release signing, Kotzilla monitoring and Firebase are each
switched off when their (git-ignored) config file is absent.
