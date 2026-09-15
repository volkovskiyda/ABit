package com.gmail.volkovskiyda.abit.wear.tile

import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.gmail.volkovskiyda.abit.core.common.LocalClock
import com.gmail.volkovskiyda.abit.core.designsystem.dayLabel
import com.gmail.volkovskiyda.abit.core.designsystem.hhmm
import com.gmail.volkovskiyda.abit.core.domain.Chime
import com.gmail.volkovskiyda.abit.core.domain.ChimeKind
import com.gmail.volkovskiyda.abit.core.domain.DayOverrideRepository
import com.gmail.volkovskiyda.abit.core.domain.ScheduleRepository
import com.gmail.volkovskiyda.abit.core.domain.chimesFrom
import com.gmail.volkovskiyda.abit.wear.MainActivity
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.minutes

private const val RESOURCES_VERSION = "1"

/**
 * Refreshed often enough to be honest and rarely enough not to drain the watch. The lower bound
 * matters because the copy says "in 23 min"; the upper bound because an idle evening should not wake
 * the watch every minute.
 */
private val MIN_FRESHNESS = 1.minutes
private val MAX_FRESHNESS = 20.minutes

/**
 * The next chime, one swipe from the watch face — which is the point of a watch app like this one.
 *
 * ProtoLayout, not Compose: a tile is rendered by the system launcher in another process, so it is a
 * serialized layout tree rather than a composition. Nothing from `core:designsystem`'s composables
 * can be reused here; the tokens and `AbitFormat` can, and that is the second reason item 06 keeps
 * them Compose-free.
 */
class AbitTileService :
    TileService(),
    KoinComponent {
    private val scheduleRepository: ScheduleRepository by inject()
    private val dayOverrideRepository: DayOverrideRepository by inject()
    private val clock: LocalClock by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        CallbackToFutureAdapter.getFuture { completer ->
            completer.set(
                ResourceBuilders.Resources
                    .Builder()
                    .setVersion(RESOURCES_VERSION)
                    .build(),
            )
            "AbitTileService.onTileResourcesRequest"
        }

    /**
     * The completer is always answered, whatever [buildTile] does.
     *
     * It reads the database, so it can throw — and an exception escaping this coroutine reached the
     * default handler and took the process with it, while leaving the future unset so the tile
     * renderer waited on a request nobody was going to finish. Cancellation is the quieter half of
     * the same problem: onDestroy cancels the scope mid-request, and without the completion hook
     * below that request simply hangs.
     *
     * A failure answers with the tile a watch with nothing scheduled gets. Saying what is next is
     * the tile's whole job, and "nothing yet" is a truthful thing to put on a wrist while something
     * else is wrong; a slot that never resolves is not.
     */
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        CallbackToFutureAdapter.getFuture { completer ->
            val job =
                scope.launch {
                    runCatching { buildTile(requestParams) }
                        .onSuccess { completer.set(it) }
                        .onFailure { completer.set(tile(requestParams, next = null)) }
                }
            job.invokeOnCompletion { cause -> if (cause != null) completer.setCancelled() }
            "AbitTileService.onTileRequest"
        }

    private suspend fun buildTile(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        val now = clock.now()
        val schedules = scheduleRepository.observeSchedules().first()
        val overrides = dayOverrideRepository.observeFrom(now.date).first()
        return tile(requestParams, next = chimesFrom(schedules, overrides, now, limit = 1).firstOrNull())
    }

    /** The layout alone: no I/O, so this is the half that cannot fail. */
    private fun tile(
        requestParams: RequestBuilders.TileRequest,
        next: Chime?,
    ): TileBuilders.Tile {
        val layout =
            materialScope(
                context = this,
                deviceConfiguration = requestParams.deviceConfiguration,
            ) {
                primaryLayout(
                    titleSlot = { text(headline(next).layoutString, typography = Typography.LABEL_SMALL) },
                    mainSlot = {
                        LayoutElementBuilders.Column
                            .Builder()
                            .addContent(text(boundary(next).layoutString, typography = Typography.DISPLAY_MEDIUM))
                            .addContent(text(detail(next).layoutString, typography = Typography.BODY_MEDIUM))
                            .build()
                    },
                    bottomSlot = { openAbitButton() },
                )
            }

        return TileBuilders.Tile
            .Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(freshnessMillis(next))
            .setTileTimeline(
                TimelineBuilders.Timeline
                    .Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry
                            .Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout
                                    .Builder()
                                    .setRoot(layout)
                                    .build(),
                            ).build(),
                    ).build(),
            ).build()
    }

    private fun MaterialScope.openAbitButton(): LayoutElementBuilders.LayoutElement =
        textEdgeButton(
            onClick =
                ModifiersBuilders.Clickable
                    .Builder()
                    .setId("open")
                    .setOnClick(
                        ActionBuilders.LaunchAction
                            .Builder()
                            .setAndroidActivity(
                                ActionBuilders.AndroidActivity
                                    .Builder()
                                    .setPackageName(packageName)
                                    .setClassName(MainActivity::class.java.name)
                                    .build(),
                            ).build(),
                    ).build(),
        ) { text("Open ABit".layoutString) }

    /** The tile is accurate to the minute near a boundary and cheap when the day is over. */
    private fun freshnessMillis(next: Chime?): Long {
        if (next == null) return MAX_FRESHNESS.inWholeMilliseconds
        val until = next.at.toInstant(clock.zone()) - clock.instant()
        return until.coerceIn(MIN_FRESHNESS, MAX_FRESHNESS).inWholeMilliseconds
    }

    private fun headline(next: Chime?): String = if (next == null) "NO SCHEDULE" else "NEXT CHIME"

    private fun boundary(next: Chime?): String = if (next == null) "—" else hhmm(next.at.time)

    private fun detail(next: Chime?): String {
        if (next == null) return "Add one on your phone"
        val kind =
            when (next.kind) {
                ChimeKind.FocusStart -> "Focus"
                ChimeKind.BreakStart -> "Break"
                ChimeKind.DayEnd -> "Done"
            }
        val until = next.at.toInstant(clock.zone()) - clock.instant()
        val minutes = until.inWholeMinutes
        return if (next.at.date == clock.today()) {
            "$kind · in $minutes min"
        } else {
            "$kind · ${dayLabel(next.at.date)}"
        }
    }
}
