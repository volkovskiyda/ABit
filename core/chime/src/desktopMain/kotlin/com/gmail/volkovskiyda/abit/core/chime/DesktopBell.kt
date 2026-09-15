package com.gmail.volkovskiyda.abit.core.chime

import com.gmail.volkovskiyda.abit.core.datastore.ChimeSound
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

private const val SAMPLE_RATE = 44_100f
private const val BITS_PER_SAMPLE = 16
private const val CHANNELS = 1
private const val DURATION_SECONDS = 0.6f

/** A fundamental and a quieter partial an octave and a fifth above it: a bell, not a beep. */
private const val FUNDAMENTAL_HZ = 880.0
private const val PARTIAL_HZ = 2640.0
private const val PARTIAL_GAIN = 0.35
private const val DECAY = 6.0
private const val FADE_IN_SECONDS = 0.005f
private const val AMPLITUDE = 0.35
private const val SHORT_MAX = 32_767.0

/** Little-endian 16-bit PCM: low byte, then high byte. */
private const val BYTE_MASK = 0xFF
private const val HIGH_BYTE_SHIFT = 8

/**
 * The boundary sound, synthesized rather than shipped. No audio file means no asset in the DMG, no
 * licence to track and nothing to keep in step with the browser's copy — the Web Audio bell uses the
 * same numbers, so the Mac and the browser sound alike.
 */
class DesktopBell : Bell {
    override suspend fun ring(sound: ChimeSound) {
        when (sound) {
            ChimeSound.Silent -> Unit

            // No notification sound exists to borrow on a JVM desktop app; the system beep is the
            // closest thing the platform offers.
            ChimeSound.Platform -> withContext(Dispatchers.IO) { Toolkit.getDefaultToolkit().beep() }

            ChimeSound.SoftBell -> withContext(Dispatchers.IO) { play(render()) }
        }
    }

    private fun render(): ByteArray {
        val samples = (SAMPLE_RATE * DURATION_SECONDS).toInt()
        val fadeIn = (SAMPLE_RATE * FADE_IN_SECONDS).toInt().coerceAtLeast(1)
        val bytes = ByteArray(samples * 2)
        for (i in 0 until samples) {
            val t = i / SAMPLE_RATE.toDouble()
            val envelope = exp(-DECAY * t) * minOf(1.0, i.toDouble() / fadeIn)
            val value =
                (sin(2 * PI * FUNDAMENTAL_HZ * t) + PARTIAL_GAIN * sin(2 * PI * PARTIAL_HZ * t)) *
                    envelope * AMPLITUDE
            val sample = (value * SHORT_MAX).toInt().coerceIn(-Short.MAX_VALUE.toInt(), Short.MAX_VALUE.toInt())
            bytes[i * 2] = (sample and BYTE_MASK).toByte()
            bytes[i * 2 + 1] = ((sample shr HIGH_BYTE_SHIFT) and BYTE_MASK).toByte()
        }
        return bytes
    }

    private fun play(pcm: ByteArray) {
        val format = AudioFormat(SAMPLE_RATE, BITS_PER_SAMPLE, CHANNELS, true, false)
        runCatching {
            AudioSystem.getSourceDataLine(format).use { line: SourceDataLine ->
                line.open(format)
                line.start()
                line.write(pcm, 0, pcm.size)
                line.drain()
                line.stop()
            }
        }
    }
}

private inline fun <T : SourceDataLine, R> T.use(block: (T) -> R): R =
    try {
        block(this)
    } finally {
        close()
    }
