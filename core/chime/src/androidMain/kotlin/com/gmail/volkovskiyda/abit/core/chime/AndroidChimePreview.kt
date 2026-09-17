package com.gmail.volkovskiyda.abit.core.chime

import android.content.Context
import android.media.RingtoneManager
import com.gmail.volkovskiyda.abit.core.datastore.ChimeSound
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    override suspend fun countdown() = notifications.postSampleCountdown()
}
