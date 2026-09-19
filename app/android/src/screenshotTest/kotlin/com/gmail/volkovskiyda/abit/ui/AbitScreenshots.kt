package com.gmail.volkovskiyda.abit.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.gmail.volkovskiyda.abit.core.datastore.ThemeMode
import com.gmail.volkovskiyda.abit.core.datastore.UserPreferences
import com.gmail.volkovskiyda.abit.core.domain.AuthUser
import com.gmail.volkovskiyda.abit.core.domain.BlockKind
import com.gmail.volkovskiyda.abit.core.domain.Conflict
import com.gmail.volkovskiyda.abit.core.domain.DayPlan
import com.gmail.volkovskiyda.abit.core.domain.NextSession
import com.gmail.volkovskiyda.abit.core.domain.SyncState
import com.gmail.volkovskiyda.abit.core.domain.TodayState
import com.gmail.volkovskiyda.abit.core.domain.planFor
import com.gmail.volkovskiyda.abit.core.model.Schedule
import com.gmail.volkovskiyda.abit.core.model.ScheduleId
import com.gmail.volkovskiyda.abit.core.model.UserId
import com.gmail.volkovskiyda.abit.feature.schedules.impl.SchedulesUiState
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionId
import com.gmail.volkovskiyda.abit.feature.settings.api.PermissionState
import com.gmail.volkovskiyda.abit.feature.settings.impl.SettingsUiState
import com.gmail.volkovskiyda.abit.feature.today.impl.TodayUiState
import com.gmail.volkovskiyda.abit.ui.schedules.SchedulesContent
import com.gmail.volkovskiyda.abit.ui.settings.SettingsContent
import com.gmail.volkovskiyda.abit.ui.theme.AbitTheme
import com.gmail.volkovskiyda.abit.ui.today.TodayContent
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Golden images of the phone screens. Each `@PreviewTest` is rendered by LayoutLib at build time and
 * diffed against a committed PNG, which catches the kind of regression a semantics assertion cannot
 * see: a clipped label, a broken dark palette, text that stops fitting at a larger font.
 *
 * **Every state here is frozen.** A golden rendering a live countdown would re-bake itself on every
 * run and diff against yesterday's, so the states below carry a fixed `remaining` and a fixed date
 * rather than reading a clock.
 *
 * Re-bake with `./gradlew :app:android:updateDebugScreenshotTest` after a deliberate change, then
 * look at the diff before committing it. The goldens are LFS objects — see `.gitattributes`.
 */
private const val YEAR = 2026
private const val SEPTEMBER = 9
private const val NINE_AM = 9
private const val FIVE_PM = 17
private const val SIX_PM = 18
private const val HALF_PAST_NINE_THIRTY = 30
private const val FOCUS_MINUTES = 45
private const val BREAK_MINUTES = 15
private const val FOCUS_REMAINING_MINUTES = 22
private const val SESSION_REMAINING_MINUTES = 37
private const val REMAINING_SECONDS = 38
private const val BREAK_REMAINING_MINUTES = 10
private const val TWENTY_TWO = 22
private const val FIFTY = 50
private const val FORTY_FIVE = 45
private const val LARGEST_FONT_SCALE = 2.0f

/**
 * The room a 360x640 dp phone — the shortest screen the app supports, and the SmallPhone.arm the
 * Test Lab matrix runs — actually gives `SchedulesContent`, once the status bar and the
 * NavigationSuiteScaffold band outside this composable have taken theirs. Measured off a Test Lab
 * frame rather than derived, because the band's height is the adaptive library's to choose.
 *
 * The default preview device is tall enough to hide a layout that overflows here, which is how an
 * empty state whose only call to action sat below the fold reached a release.
 */
private const val SHORT_PHONE = "spec:width=360dp,height=480dp,dpi=320"

/** A stand-in for the git-derived version, so the goldens do not move with every commit. */
private const val SCREENSHOT_VERSION = "1.0.0"

/** Inside the first focus block, and inside the break that follows it: the two `running` states. */
private val FOCUS_NOW = LocalTime(NINE_AM, TWENTY_TWO)
private val BREAK_NOW = LocalTime(NINE_AM, FIFTY)

private val MONDAY = LocalDate(YEAR, SEPTEMBER, 14)
private val TUESDAY = LocalDate(YEAR, SEPTEMBER, 15)
private val TEST_INSTANT = Instant.fromEpochSeconds(1_700_000_000)

/** The two accounts the empty state tells apart: one that syncs, and one that does not yet. */
private val ANONYMOUS = AuthUser(UserId("anonymous"), isAnonymous = true)
private val SIGNED_IN = AuthUser(UserId("google"), isAnonymous = false, email = "user@example.com")

private val WORKDAY_SET =
    setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)

@Suppress("LongParameterList")
private fun workdays(
    id: String = "workdays",
    name: String = "Workdays",
    enabled: Boolean = true,
    days: Set<DayOfWeek> = WORKDAY_SET,
    start: LocalTime = LocalTime(NINE_AM, 0),
    end: LocalTime = LocalTime(SIX_PM, 0),
) = Schedule(
    id = ScheduleId(id),
    name = name,
    enabled = enabled,
    days = days,
    start = start,
    end = end,
    focusMinutes = FOCUS_MINUTES,
    breakMinutes = BREAK_MINUTES,
    updatedAt = TEST_INSTANT,
)

private fun running(stage: BlockKind): TodayState.Running {
    val plan = workdays().planFor(MONDAY)
    val session = plan.sessions.first()
    return TodayState.Running(
        plan = plan,
        session = session,
        sessionNumber = 1,
        sessionCount = plan.sessions.size,
        stage = stage,
        // 09:22:38 into the first session: 22:38 left of the focus, 37:38 left of the session itself.
        stageRemaining =
            if (stage == BlockKind.Focus) {
                FOCUS_REMAINING_MINUTES.minutes + REMAINING_SECONDS.seconds
            } else {
                BREAK_REMAINING_MINUTES.minutes
            },
        sessionRemaining =
            if (stage == BlockKind.Focus) {
                SESSION_REMAINING_MINUTES.minutes + REMAINING_SECONDS.seconds
            } else {
                BREAK_REMAINING_MINUTES.minutes
            },
        nextBoundary = if (stage == BlockKind.Focus) LocalTime(NINE_AM, FORTY_FIVE) else LocalTime(NINE_AM + 1, 0),
    )
}

private fun offHours() =
    TodayState.OffHours(
        today = DayPlan(MONDAY, schedule = null, sessions = emptyList()),
        next = NextSession(TUESDAY, LocalTime(NINE_AM, 0), "Workdays"),
        canSkipTomorrow = true,
    )

@Composable
private fun Today(
    themeMode: ThemeMode,
    state: TodayUiState,
) {
    AbitTheme(themeMode = themeMode) {
        TodayContent(
            state = state,
            onSkipToday = {},
            onSkipTomorrow = {},
            onOpenSignIn = {},
            onOpenConflict = { _, _ -> },
        )
    }
}

@Composable
private fun Settings(
    themeMode: ThemeMode,
    state: SettingsUiState,
) {
    AbitTheme(themeMode = themeMode) {
        SettingsContent(
            state = state,
            onThemeMode = {},
            onShowCountdown = {},
            onSignIn = {},
            onSignOut = {},
            onRequestNotifications = {},
            onRequestExactAlarms = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
private fun TodayFocusLight() = Today(ThemeMode.Light, TodayUiState(today = running(BlockKind.Focus), now = FOCUS_NOW))

@PreviewTest
@Preview(showBackground = true)
@Composable
private fun TodayFocusDark() = Today(ThemeMode.Dark, TodayUiState(today = running(BlockKind.Focus), now = FOCUS_NOW))

@PreviewTest
@Preview(showBackground = true)
@Composable
private fun TodayBreakLight() = Today(ThemeMode.Light, TodayUiState(today = running(BlockKind.Break), now = BREAK_NOW))

/** Mid-afternoon, where the morning's blocks are folded into one row rather than scrolled past. */
@PreviewTest
@Preview(showBackground = true)
@Composable
private fun TodayAfternoonLight() = Today(ThemeMode.Light, TodayUiState(today = running(BlockKind.Focus), now = LocalTime(FIVE_PM, 0)))

@PreviewTest
@Preview(showBackground = true)
@Composable
private fun TodayOffHoursLight() = Today(ThemeMode.Light, TodayUiState(today = offHours()))

/** The largest font scale the system offers, where a cramped layout gives way first. */
@PreviewTest
@Preview(showBackground = true, fontScale = LARGEST_FONT_SCALE)
@Composable
private fun TodayLargestFont() =
    Today(ThemeMode.Light, TodayUiState(today = running(BlockKind.Focus), now = FOCUS_NOW, syncState = SyncState.Syncing))

/**
 * A fresh install, which is the one state where the screen has nothing of its own to show. Both
 * accounts are covered because they are different screens: an anonymous one is offered sync, and a
 * signed-in one has the empty state to itself.
 */
@Composable
private fun SchedulesEmpty(
    themeMode: ThemeMode,
    user: AuthUser?,
) {
    AbitTheme(themeMode = themeMode) {
        SchedulesContent(
            state = SchedulesUiState(loaded = true, user = user),
            onToggle = { _, _ -> },
            onOpenEditor = {},
            onOpenConflict = { _, _ -> },
            onOpenSignIn = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
private fun SchedulesEmptyAnonymousLight() = SchedulesEmpty(ThemeMode.Light, ANONYMOUS)

@PreviewTest
@Preview(showBackground = true)
@Composable
private fun SchedulesEmptyAnonymousDark() = SchedulesEmpty(ThemeMode.Dark, ANONYMOUS)

@PreviewTest
@Preview(showBackground = true)
@Composable
private fun SchedulesEmptySignedInLight() = SchedulesEmpty(ThemeMode.Light, SIGNED_IN)

@PreviewTest
@Preview(showBackground = true)
@Composable
private fun SchedulesEmptySignedInDark() = SchedulesEmpty(ThemeMode.Dark, SIGNED_IN)

/** The empty state at the largest font scale, where a centred column runs out of room first. */
@PreviewTest
@Preview(showBackground = true, fontScale = LARGEST_FONT_SCALE)
@Composable
private fun SchedulesEmptyLargestFont() = SchedulesEmpty(ThemeMode.Light, ANONYMOUS)

/**
 * The empty state on the shortest screen the app supports, anonymous — the tallest of the empty
 * states, because it is the one carrying the sign-in card. "New schedule" has to be on screen here
 * without scrolling: the FAB is withheld while the list is empty, so it is the only way in.
 */
@PreviewTest
@Preview(showBackground = true, device = SHORT_PHONE)
@Composable
private fun SchedulesEmptyShortPhone() = SchedulesEmpty(ThemeMode.Light, ANONYMOUS)

@PreviewTest
@Preview(showBackground = true)
@Composable
private fun SchedulesLight() {
    AbitTheme(themeMode = ThemeMode.Light) {
        SchedulesContent(
            state =
                SchedulesUiState(
                    schedules = listOf(workdays(), workdays(id = "light", name = "Saturday light", enabled = false)),
                ),
            onToggle = { _, _ -> },
            onOpenEditor = {},
            onOpenConflict = { _, _ -> },
            onOpenSignIn = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
private fun SchedulesConflictLight() {
    val evening =
        workdays(
            id = "evening",
            name = "Evening study",
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            start = LocalTime(FIVE_PM, 0),
            end = LocalTime(FIVE_PM + 4, HALF_PAST_NINE_THIRTY),
        )
    AbitTheme(themeMode = ThemeMode.Light) {
        SchedulesContent(
            state =
                SchedulesUiState(
                    schedules = listOf(workdays(), evening),
                    conflicts =
                        listOf(
                            Conflict(
                                first = ScheduleId("evening"),
                                second = ScheduleId("workdays"),
                                days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                                from = LocalTime(FIVE_PM, 0),
                                to = LocalTime(SIX_PM, 0),
                            ),
                        ),
                ),
            onToggle = { _, _ -> },
            onOpenEditor = {},
            onOpenConflict = { _, _ -> },
            onOpenSignIn = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
private fun SettingsLight() =
    Settings(
        ThemeMode.Light,
        SettingsUiState(
            preferences = UserPreferences(),
            permissions =
                listOf(
                    PermissionState(PermissionId.Notifications, granted = true),
                    PermissionState(PermissionId.ExactAlarms, granted = false),
                ),
            // Fixed, not the real one: the version is git-derived, so a golden holding the actual
            // number would go red on the next commit.
            appVersion = SCREENSHOT_VERSION,
        ),
    )

@PreviewTest
@Preview(showBackground = true)
@Composable
private fun SettingsDark() =
    Settings(
        ThemeMode.Dark,
        SettingsUiState(preferences = UserPreferences(), appVersion = SCREENSHOT_VERSION),
    )
