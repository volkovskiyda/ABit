package com.gmail.volkovskiyda.abit.core.designsystem

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

private const val SECONDS_IN_MINUTE = 60
private const val MINUTES_IN_HOUR = 60
private const val TWO_DIGITS_FROM = 10
private const val SHORT_NAME_LENGTH = 3

/**
 * Times are 24-hour everywhere. Locale-aware formatting is a backlog item, not an oversight: the
 * schedule editor, the timeline and the chime notification all have to agree, and one rule that is
 * the same on four platforms is worth more than the twelve-hour clock an American user expects.
 */
fun hhmm(time: LocalTime): String = "${time.hour.pad()}:${time.minute.pad()}"

/** `mm:ss` under an hour, `h:mm:ss` above. Negative durations read as zero rather than as a minus. */
fun countdown(remaining: Duration): String {
    val totalSeconds = remaining.inWholeSeconds.coerceAtLeast(0)
    val seconds = (totalSeconds % SECONDS_IN_MINUTE).toInt()
    val minutes = (totalSeconds / SECONDS_IN_MINUTE % MINUTES_IN_HOUR).toInt()
    val hours = totalSeconds / SECONDS_IN_MINUTE / MINUTES_IN_HOUR
    return if (hours > 0) "$hours:${minutes.pad()}:${seconds.pad()}" else "${minutes.pad()}:${seconds.pad()}"
}

/** `Mon 15 Sep`. */
fun dayLabel(date: LocalDate): String = "${date.dayOfWeek.shortLabel()} ${date.day} ${date.month.shortLabel()}"

/** `09:00 – 18:00`, with an en dash, which is what the design draws. */
fun timeRange(
    from: LocalTime,
    to: LocalTime,
): String = "${hhmm(from)} – ${hhmm(to)}"

private fun Int.pad(): String = if (this < TWO_DIGITS_FROM) "0$this" else toString()

private fun kotlinx.datetime.DayOfWeek.shortLabel(): String = name.take(SHORT_NAME_LENGTH).lowercase().replaceFirstChar { it.uppercase() }

private fun kotlinx.datetime.Month.shortLabel(): String = name.take(SHORT_NAME_LENGTH).lowercase().replaceFirstChar { it.uppercase() }
