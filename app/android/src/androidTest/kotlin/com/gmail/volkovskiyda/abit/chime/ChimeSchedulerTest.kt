package com.gmail.volkovskiyda.abit.chime

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gmail.volkovskiyda.abit.core.chime.ACTION_CHIME
import com.gmail.volkovskiyda.abit.core.chime.ChimeScheduler
import com.gmail.volkovskiyda.abit.core.domain.Block
import com.gmail.volkovskiyda.abit.core.domain.BlockKind
import com.gmail.volkovskiyda.abit.core.domain.Chime
import com.gmail.volkovskiyda.abit.core.domain.ChimeKind
import com.gmail.volkovskiyda.abit.core.domain.Session
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * Exercises the real `AlarmManager` wiring on a device. The assertions go through
 * `PendingIntent.getBroadcast(..., FLAG_NO_CREATE)`, which is the only way to ask Android whether an
 * alarm's pending intent actually exists — the alarm list itself is not readable by an app.
 *
 * Runs on the Gradle-managed emulator (`./gradlew ciGroupDebugAndroidTest`).
 */
@RunWith(AndroidJUnit4::class)
class ChimeSchedulerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scheduler: ChimeScheduler get() = GlobalContext.get().get()

    @Test
    fun armingTwiceLeavesExactlyOnePendingIntent() =
        runBlocking {
            scheduler.arm(chime(LocalTime(9, 45)))
            scheduler.arm(chime(LocalTime(10, 0)))

            // One request code and FLAG_UPDATE_CURRENT: the second arm replaces the first rather than
            // adding a second alarm that would chime at the wrong minute.
            assertNotNull("no alarm was armed", existingPendingIntent())
        }

    @Test
    fun disarmRemovesThePendingIntent() =
        runBlocking {
            scheduler.arm(chime(LocalTime(9, 45)))
            scheduler.disarm()

            assertNull("the alarm survived disarm", existingPendingIntent())
        }

    /**
     * `Intent.filterEquals` compares the component too, and the scheduler arms an **explicit**
     * intent — resolved through the manifest filter — so this has to name the same receiver or
     * `FLAG_NO_CREATE` reports nothing however many alarms are set.
     */
    private fun existingPendingIntent(): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            1,
            Intent(ACTION_CHIME)
                .setPackage(context.packageName)
                .setComponent(ComponentName(context, ChimeReceiver::class.java)),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun chime(at: LocalTime) =
        Chime(
            at = LocalDateTime(LocalDate(2026, 9, 14), at),
            kind = ChimeKind.BreakStart,
            scheduleName = "Workdays",
            session =
                Session(
                    index = 0,
                    focus = Block(BlockKind.Focus, LocalTime(9, 0), LocalTime(9, 45)),
                    rest = Block(BlockKind.Break, LocalTime(9, 45), LocalTime(10, 0)),
                ),
        )
}
