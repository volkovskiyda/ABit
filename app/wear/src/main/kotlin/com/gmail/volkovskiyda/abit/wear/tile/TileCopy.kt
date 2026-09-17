// Every word the tile puts on screen, and the one colour the day's mode is allowed to tint. The
// wording tracks `WearTodayScreen`, because a tile that phrases the same state differently from the
// app one swipe away is a tile that looks wrong rather than one that looks its own.
package com.gmail.volkovskiyda.abit.wear.tile

import androidx.wear.protolayout.types.LayoutColor
import com.gmail.volkovskiyda.abit.core.designsystem.AbitTokens
import com.gmail.volkovskiyda.abit.core.designsystem.dayLabel
import com.gmail.volkovskiyda.abit.core.designsystem.hhmm
import com.gmail.volkovskiyda.abit.core.designsystem.sessionCaption
import com.gmail.volkovskiyda.abit.core.domain.BlockKind
import com.gmail.volkovskiyda.abit.core.domain.NextSession
import com.gmail.volkovskiyda.abit.core.domain.TodayState
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

/** The label above the digits, shouted the way the app's Today screen shouts it. */
internal fun TodayState.mode(): String =
    when (this) {
        is TodayState.Running -> if (stage == BlockKind.Focus) "FOCUS" else "BREAK"
        is TodayState.Skipped -> "SKIPPED"
        is TodayState.OffHours -> if (next == null) "NO SCHEDULE" else "OFF HOURS"
    }

internal fun TodayState.modeColor(): LayoutColor =
    when {
        this !is TodayState.Running -> AbitTokens.Dark.ON_SURFACE_VARIANT.layoutColor()
        stage == BlockKind.Focus -> AbitTokens.Dark.PRIMARY.layoutColor()
        else -> AbitTokens.Dark.TERTIARY.layoutColor()
    }

internal fun TodayState.caption(now: LocalDateTime): String =
    when (this) {
        is TodayState.Running -> sessionCaption(nextBoundary, session.end)
        is TodayState.Skipped -> "until ${dayLabel(resumesOn)}"
        is TodayState.OffHours -> next.caption(now)
    }

/**
 * Which schedule is next, and — when it is not today — which day. The digits above say `09:00` and
 * nothing else; without the date that is a promise the tile has not actually made.
 */
private fun NextSession?.caption(now: LocalDateTime): String =
    when {
        this == null -> "add one on your phone"
        date == now.date -> scheduleName
        else -> "${dayLabel(date)} · $scheduleName"
    }

/** What a screen reader says instead of reading three fragments in a row. */
internal fun TodayState.spoken(now: LocalDateTime): String =
    when (this) {
        is TodayState.Running -> "${stage.spokenName()} until ${hhmm(nextBoundary)}, ${stageRemaining.spokenMinutes()} left"
        is TodayState.Skipped -> "Today is skipped, resuming ${dayLabel(resumesOn)}"
        is TodayState.OffHours -> next.spoken(now)
    }

private fun NextSession?.spoken(now: LocalDateTime): String =
    when {
        this == null -> "No schedule yet"
        date == now.date -> "Next session at ${hhmm(at)}"
        else -> "Next session ${dayLabel(date)} at ${hhmm(at)}"
    }

private fun BlockKind.spokenName(): String = if (this == BlockKind.Focus) "Focus" else "Break"

private fun Duration.spokenMinutes(): String = inWholeMinutes.let { if (it == 1L) "1 minute" else "$it minutes" }
