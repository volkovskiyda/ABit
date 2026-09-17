package com.gmail.volkovskiyda.abit.core.chime

import android.content.Context
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import com.gmail.volkovskiyda.abit.core.datastore.ChimeSound
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Long enough to feel deliberate, short enough not to outlast the tap that asked for it. */
private const val BUZZ_MILLIS = 120L

/**
 * Plays the sound the chime channel would play, which on Android and Wear is the platform's own
 * notification sound — [ChimeNotifications] leaves the channel alone on purpose, so borrowing the
 * same default here is what makes the preview honest.
 */
class AndroidChimePreview(
    private val context: Context,
    private val notifications: ChimeNotifications,
) : ChimePreview {
    override suspend fun chime(sound: ChimeSound) {
        if (sound == ChimeSound.Silent) return
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return
        // Loading a ringtone touches the media store, and a device with none configured throws
        // rather than returning null.
        withContext(Dispatchers.IO) {
            runCatching { RingtoneManager.getRingtone(context, uri)?.play() }
        }
    }

    override suspend fun vibrate() {
        val vibrator = context.getSystemService<Vibrator>() ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createOneShot(BUZZ_MILLIS, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override suspend fun countdown() = notifications.postSampleCountdown()
}
