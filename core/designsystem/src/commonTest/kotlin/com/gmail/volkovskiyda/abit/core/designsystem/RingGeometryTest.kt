package com.gmail.volkovskiyda.abit.core.designsystem

import com.gmail.volkovskiyda.abit.core.domain.Block
import com.gmail.volkovskiyda.abit.core.domain.BlockKind
import com.gmail.volkovskiyda.abit.core.domain.Session
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TOLERANCE = 0.01f

private fun session(
    focusFrom: String,
    focusTo: String,
    restTo: String? = null,
) = Session(
    index = 0,
    focus = Block(BlockKind.Focus, LocalTime.parse(focusFrom), LocalTime.parse(focusTo)),
    rest = restTo?.let { Block(BlockKind.Break, LocalTime.parse(focusTo), LocalTime.parse(it)) },
)

class RingGeometryTest {
    /** Workdays 45/15: session 1 runs 09:00–10:00, so a full session is 360° and the break is 90°. */
    private val fortyFiveFifteen = session("09:00", "09:45", "10:00")

    @Test
    fun `the design's 09-22 sample is a 228 degree total with a 90 degree break share`() {
        val arcs = ringArcs(fortyFiveFifteen, LocalTime(9, 22))

        // 38 of 60 minutes left.
        assertEquals(228f, arcs.totalSweep, TOLERANCE)
        assertEquals(90f, arcs.breakSweep, TOLERANCE)
        assertEquals(138f, arcs.focusSweep, TOLERANCE)
        assertEquals(RING_GAP_DEGREES, arcs.gapDegrees)
    }

    @Test
    fun `at the boundary exactly, the whole remaining ring is the break`() {
        val arcs = ringArcs(fortyFiveFifteen, LocalTime(9, 45))

        assertEquals(90f, arcs.totalSweep, TOLERANCE)
        assertEquals(90f, arcs.breakSweep, TOLERANCE)
        assertEquals(0f, arcs.focusSweep, TOLERANCE)
        assertEquals(0f, arcs.gapDegrees, "no gap with only one arc")
    }

    @Test
    fun `half a minute before the end there is only a sliver`() {
        val arcs = ringArcs(fortyFiveFifteen, LocalTime(9, 59, 30))

        assertEquals(3f, arcs.totalSweep, TOLERANCE)
        assertEquals(3f, arcs.breakSweep, TOLERANCE)
    }

    @Test
    fun `the break share is not a constant 90 degrees`() {
        // 50/10 is a 60-minute session too, but only a sixth of it is break.
        val arcs = ringArcs(session("17:00", "17:50", "18:00"), LocalTime(17, 0))

        assertEquals(360f, arcs.totalSweep, TOLERANCE)
        assertEquals(60f, arcs.breakSweep, TOLERANCE)
        assertEquals(300f, arcs.focusSweep, TOLERANCE)
    }

    @Test
    fun `the last session of the day has no break share and no gap`() {
        val arcs = ringArcs(session("17:00", "17:45"), LocalTime(17, 0))

        assertEquals(360f, arcs.focusSweep, TOLERANCE)
        assertEquals(0f, arcs.breakSweep, TOLERANCE)
        assertEquals(0f, arcs.gapDegrees)
    }

    @Test
    fun `a time past the session end reads as an empty ring rather than a negative one`() {
        val arcs = ringArcs(fortyFiveFifteen, LocalTime(11, 0))

        assertEquals(0f, arcs.totalSweep, TOLERANCE)
        assertTrue(arcs.breakSweep >= 0f && arcs.focusSweep >= 0f)
    }

    @Test
    fun `minutes left counts the current block, rounds down and never goes negative`() {
        // 09:22:22 is 22 minutes and 38 seconds from the end of the focus, not 37 from the session.
        assertEquals(22, minutesLeft(fortyFiveFifteen, LocalTime(9, 22, 22)))
        assertEquals(10, minutesLeft(fortyFiveFifteen, LocalTime(9, 50)))
        assertEquals(0, minutesLeft(fortyFiveFifteen, LocalTime(11, 0)))
    }
}
