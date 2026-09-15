package com.gmail.volkovskiyda.abit.core.chime

import com.gmail.volkovskiyda.abit.core.datastore.ChimeSound

/**
 * Web Audio, not an audio file: nothing to download, nothing to license, and the same two partials
 * the desktop synthesizes.
 *
 * The envelope matches `DesktopBell` — 880 Hz fundamental, a 2640 Hz partial at 0.35 gain, a 5 ms
 * fade-in and a 600 ms exponential decay — so the Mac and the browser sound alike. The numbers live
 * inside the `js(…)` block rather than as Kotlin constants because Web Audio is scheduled against
 * `context.currentTime` and interpolating them in would buy nothing.
 *
 * An `AudioContext` created before any user gesture starts **suspended** — every browser requires one
 * interaction with the page before it will make a noise. [resumeOnGesture] is what the page calls on
 * the first click; until then a chime is silent, which is part of what [webChimeLimitation] says out
 * loud.
 */
class WebBell : Bell {
    private var context: JsAny? = null

    override suspend fun ring(sound: ChimeSound) {
        when (sound) {
            ChimeSound.Silent -> Unit

            // The browser has no notification sound to borrow either, so both audible modes ring the
            // same bell. The setting still matters: it is how a user turns it off.
            ChimeSound.Platform, ChimeSound.SoftBell -> playBell(audioContext())
        }
    }

    /** Called from the first user gesture on the page. Safe to call more than once. */
    fun resumeOnGesture() {
        resumeContext(audioContext())
    }

    private fun audioContext(): JsAny = context ?: createAudioContext().also { context = it }
}

private fun createAudioContext(): JsAny = js("new (window.AudioContext || window.webkitAudioContext)()")

// The parameter is read inside the `js(…)` body, which detekt cannot see.
@Suppress("UnusedParameter")
private fun resumeContext(context: JsAny): Unit = js("{ if (context.state === 'suspended') { context.resume(); } }")

@Suppress("UnusedParameter")
private fun playBell(context: JsAny): Unit =
    js(
        """{
            var now = context.currentTime;
            var gain = context.createGain();
            gain.gain.setValueAtTime(0.0001, now);
            gain.gain.linearRampToValueAtTime(0.35, now + 0.005);
            gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.6);
            gain.connect(context.destination);

            var partialGain = context.createGain();
            partialGain.gain.setValueAtTime(0.35, now);
            partialGain.connect(gain);

            var fundamental = context.createOscillator();
            fundamental.type = 'sine';
            fundamental.frequency.setValueAtTime(880, now);
            fundamental.connect(gain);

            var partial = context.createOscillator();
            partial.type = 'sine';
            partial.frequency.setValueAtTime(2640, now);
            partial.connect(partialGain);

            fundamental.start(now); fundamental.stop(now + 0.6);
            partial.start(now); partial.stop(now + 0.6);
        }""",
    )
