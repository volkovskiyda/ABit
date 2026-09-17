import com.gmail.volkovskiyda.abit.buildlogic.AbitVersioning
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // Compose Hot Reload: `./gradlew :app:desktop:hotRun` starts the tray app and re-composes it on
    // every save instead of rebuilding and relaunching. Applied here and nowhere else — it adds a
    // JVM agent and a dev runtime classpath, neither of which belongs anywhere near `package` or
    // `packageReleaseDmg`, and its own tasks are the only ones that use them.
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.abit.versioning)
}

// Read at the project level on purpose: inside `compose.desktop { }` the nested blocks are
// extension-aware themselves, so a `the<AbitVersioning>()` in there looks up the wrong container.
val abitVersioning = the<AbitVersioning>()

// Read once, outside the compose.desktop block: the nested blocks are extension-aware themselves,
// so a provider looked up in there resolves against the wrong container.
val appleSigningIdentity = providers.environmentVariable("APPLE_SIGNING_IDENTITY")

kotlin {
    jvmToolchain(
        libs.versions.jvmToolchain
            .get()
            .toInt(),
    )
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.app.shared)

    // Same reason as app:web: a plain JVM module gets no Koin BoM from the convention plugins, and
    // the koin-compose artifacts carry no version of their own.
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutines.swing)

    implementation(libs.compose.ui.toolingPreview)

    // Compose Multiplatform's own UI test, which renders through Skiko in-process — no emulator and
    // no display server, so it runs on any CI machine.
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(compose.desktop.currentOs)
    testImplementation(libs.kotlin.test)
}

compose.desktop {
    application {
        mainClass = "com.gmail.volkovskiyda.abit.MainKt"

        // The packaged app, not `run` or `hotRun`: a menu-bar process that sits there all day has no
        // use for a heap sized as a fraction of the machine's RAM, and the default AWT look does not
        // follow the system's dark mode on macOS. Both are runtime flags baked into the bundle.
        jvmArgs +=
            listOf(
                // Measured on the packaged app after start-up: 12 MB of live heap in a 141 MB G1
                // heap, so 512 MB is a ceiling with two orders of magnitude of room. Its job is to
                // turn a leak in a process that runs all day into a crash report rather than into
                // the quarter of physical RAM the JVM would otherwise help itself to.
                "-Xmx512m",
                "-Dapple.awt.application.appearance=system",
            )

        buildTypes.release.proguard {
            // Off, and this is a decision rather than a default: the repository is public, so
            // obfuscation hides nothing from anyone, while renaming is what breaks every framework
            // in this app that resolves a class by name — Room's generated `_Impl`, Firebase's
            // component registrars, kotlinx.serialization's generated serializers.
            obfuscate.set(false)
            // Off, and measured rather than assumed: with the optimizer on, the packaged app dies
            // on startup with `VerifyError: Bad type on operand stack` in `okio.Okio.sink(Socket)`
            // — ProGuard rewrote a constructor call into bytecode the JVM verifier rejects. The
            // shrink is what this build wants anyway; the optimizer's few extra megabytes are not
            // worth a crash that only a packaged build can show.
            optimize.set(false)
            // One jar rather than one per input. jlink walks the result either way, but a single
            // jar is what makes `unzip -l` on the bundle a usable answer to "what shipped".
            joinOutputJars.set(true)
            // `maxHeapSize` is deliberately not set: the Compose plugin composes the flag as
            // `-Xmx:<value>` (measured with 1.12.0), which no JVM accepts, so setting it at all
            // fails the task with "Invalid maximum heap size". ProGuard runs in its own process at
            // the JVM default and finishes this classpath comfortably.
            configurationFiles.from(project.file("compose-desktop.pro"))
        }

        nativeDistributions {
            // Dmg only: macOS is the one desktop target ABit ships. Msi and Deb would be untested
            // artifacts nobody asked for.
            targetFormats(TargetFormat.Dmg)
            packageName = "ABit"
            // Never edited per release — the same git-derived numbers the Android apps report.
            packageVersion = abitVersioning.packageVersion
            description = "Change a bit — a focus chime that follows you"
            vendor = "Dmitriy Volkovskiy"

            // Modules the bundled JRE must keep: jlink strips everything the analysis does not
            // see, and every one of these is reached reflectively. This is exactly what
            // `:app:desktop:suggestRuntimeModules` reports for the current classpath — re-run it
            // whenever a dependency with native or reflective code is added, because a module
            // missing here fails at runtime, on the one code path that needed it, in a packaged
            // build nobody ran locally. java.sql is Room's bundled SQLite, java.naming and
            // java.prefs come with the Firebase Java SDK, java.instrument and java.compiler with
            // gRPC's and protobuf's runtime code generation.
            modules(
                "java.compiler",
                "java.instrument",
                "java.naming",
                "java.prefs",
                "java.sql",
                "jdk.unsupported",
            )

            macOS {
                bundleID = "com.gmail.volkovskiyda.abit"
                dockName = "ABit"

                // Signing and notarization are gated on the environment rather than on a flag,
                // because the Apple Developer Program is not paid for yet. With no APPLE_SIGNING_
                // IDENTITY the DMG is built unsigned and macOS shows the Gatekeeper warning; a
                // tester opens it with right-click → Open once. When the certificates exist, the
                // same command produces a signed and notarized DMG with nothing else changed.
                signing {
                    sign.set(appleSigningIdentity.isPresent)
                    identity.set(appleSigningIdentity)
                }
                notarization {
                    appleID.set(providers.environmentVariable("NOTARIZATION_APPLE_ID"))
                    // An app-specific password from appleid.apple.com, never the account password.
                    password.set(providers.environmentVariable("NOTARIZATION_PASSWORD"))
                    teamID.set(providers.environmentVariable("NOTARIZATION_TEAM_ID"))
                }
                // The hardened runtime blocks a JIT outright without these; see the file itself.
                entitlementsFile.set(project.file("entitlements.plist"))
                runtimeEntitlementsFile.set(project.file("runtime-entitlements.plist"))

                // The dial, from internal/design/icon. LSUIElement hides the Dock icon, but the DMG,
                // the installer and Finder still show this one.
                iconFile.set(project.file("icons/abit.icns"))

                infoPlist {
                    // What makes this a menu-bar app: no Dock icon, no app menu bar, nothing in
                    // the app switcher. The tray icon is the only entry point.
                    extraKeysRawXml = "<key>LSUIElement</key><string>1</string>"
                }
            }
        }
    }
}
