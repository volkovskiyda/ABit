package com.gmail.volkovskiyda.abit.core.datastore

import kotlinx.coroutines.test.runTest
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class UserPreferencesSerializerTest {
    @Test
    fun `round trips every field`() =
        runTest {
            val original =
                UserPreferences(
                    themeMode = ThemeMode.Dark,
                    hasSeenOnboarding = true,
                    lastSyncedAtMillis = 1_700_000_000_000,
                    chimeOnThisDevice = false,
                    chimeSound = ChimeSound.SoftBell,
                    vibrate = false,
                    showCountdownNotification = false,
                )

            val buffer = Buffer()
            UserPreferencesSerializer.writeTo(original, buffer)

            assertEquals(original, UserPreferencesSerializer.readFrom(buffer))
        }

    @Test
    fun `reads a file written before a field existed`() =
        runTest {
            // What an older build would have left behind. Every field defaults, so this must not throw
            // and must not lose the values that are present.
            val buffer = Buffer().writeUtf8("""{"themeMode":"Light","retiredFlag":true}""")

            val read = UserPreferencesSerializer.readFrom(buffer)

            assertEquals(ThemeMode.Light, read.themeMode)
            assertEquals(false, read.hasSeenOnboarding)
            assertEquals(null, read.lastSyncedAtMillis)
        }

    @Test
    fun `reads a file written before the chime settings existed, and every device chimes`() =
        runTest {
            // What a build from before item 07 would have left behind. The defaults matter: a device
            // that has been running since then must start chiming, not stay silent.
            val buffer = Buffer().writeUtf8("""{"themeMode":"Dark","hasSeenOnboarding":true}""")

            val read = UserPreferencesSerializer.readFrom(buffer)

            assertEquals(true, read.chimeOnThisDevice)
            assertEquals(ChimeSound.Platform, read.chimeSound)
            assertEquals(true, read.vibrate)
            // The one that does not default on: it needs a permission, and a switch that reads on
            // while Android refuses to post is a switch that lies.
            assertEquals(false, read.showCountdownNotification)
        }
}
