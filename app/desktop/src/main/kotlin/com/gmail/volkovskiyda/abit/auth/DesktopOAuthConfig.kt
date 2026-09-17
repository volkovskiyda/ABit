package com.gmail.volkovskiyda.abit.auth

import java.util.Properties

/**
 * The Google OAuth client the desktop app signs in with, read from a resource the build puts there.
 *
 * `oauth.properties` sits at the repository root, is git-ignored, and has a committed
 * `.example.oauth.properties` twin — the same shape as `keystore.properties` and `kotzilla.json`.
 * `app/desktop/build.gradle.kts` copies it into the app's resources when it exists, so a checkout
 * without it compiles, packages and runs; [fromResources] simply returns null and Google sign-in
 * reports itself unavailable, exactly as it does on an Android build with no web OAuth client.
 *
 * It is a **Desktop app** client rather than a web one. Google issues those a "client secret" that
 * its own documentation says is not treated as confidential — an installed app cannot keep one — and
 * the flow below pairs it with PKCE, which is what actually secures the exchange. Using the web
 * client instead would put a genuinely confidential secret inside a DMG anyone can download.
 */
internal data class DesktopOAuthConfig(
    val clientId: String,
    val clientSecret: String,
) {
    internal companion object {
        private const val RESOURCE = "/oauth.properties"

        fun fromResources(): DesktopOAuthConfig? {
            val stream = DesktopOAuthConfig::class.java.getResourceAsStream(RESOURCE) ?: return null
            val properties = stream.use { Properties().apply { load(it) } }
            val clientId = properties.getProperty("DESKTOP_CLIENT_ID")?.takeIf { it.isNotBlank() }
            val clientSecret = properties.getProperty("DESKTOP_CLIENT_SECRET")?.takeIf { it.isNotBlank() }
            return if (clientId == null || clientSecret == null) {
                null
            } else {
                DesktopOAuthConfig(clientId, clientSecret)
            }
        }
    }
}
