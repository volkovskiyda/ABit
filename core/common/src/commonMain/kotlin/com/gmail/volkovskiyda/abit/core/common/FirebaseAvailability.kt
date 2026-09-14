package com.gmail.volkovskiyda.abit.core.common

/**
 * Whether Firebase actually initialised in this build.
 *
 * It can be false, by design. A fresh clone of this repository has no `google-services.json` — the
 * file carries credentials and is git-ignored — and the Google Services plugin is configured to warn
 * rather than fail, so the app builds and runs with no Firebase at all. Auth and sync then report
 * themselves unavailable and everything else works, which is what makes a pull-request build of this
 * project a usable app rather than a broken one.
 */
expect fun firebaseAvailable(): Boolean
