package com.gmail.volkovskiyda.abit.wear.tile

import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.modifiers.toProtoLayoutModifiers
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.gmail.volkovskiyda.abit.core.common.LocalClock
import com.gmail.volkovskiyda.abit.core.designsystem.AbitTokens
import com.gmail.volkovskiyda.abit.core.designsystem.RingArcs
import com.gmail.volkovskiyda.abit.core.designsystem.ringArcs
import com.gmail.volkovskiyda.abit.core.domain.DayOverrideRepository
import com.gmail.volkovskiyda.abit.core.domain.ScheduleRepository
import com.gmail.volkovskiyda.abit.core.domain.TodayState
import com.gmail.volkovskiyda.abit.core.domain.todayState
import com.gmail.volkovskiyda.abit.wear.MainActivity
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.minutes

private const val RESOURCES_VERSION = "1"

/**
 * How often the *service* is woken. Not how often the tile changes: the countdown and the ring are
 * bound to the renderer's own clock, so between rebuilds they stay true on their own. What a rebuild
 * is for is the things an expression cannot carry — which arc is the saturated one, which session is
 * being counted, whether a schedule was edited on the phone. Those change at a boundary, so the
 * interval is the time to the next one, floored so a flurry of boundaries cannot busy-wake the watch
 * and capped so an idle evening still picks up an edit within the hour.
 */
private val MIN_FRESHNESS = 1.minutes
private val MAX_FRESHNESS = 20.minutes

/**
 * The ring hugs the bezel inside an 8 % inset at a 6 dp stroke — the brief's numbers, and the ones
 * `WearTodayScreen` draws to, so the tile and the app's own Today screen are the same dial.
 *
 * It is given a diameter rather than left to expand into the tile. An arc that fills a container it
 * does not know to be square is drawn as an ellipse; that is the trap `WearTodayScreen` documents on
 * `RING_MAX_DIAMETER`, and the answer in both places is to hand the ring one number.
 */
private const val RING_INSET_FRACTION = 0.08f

private const val RING_STROKE_DP = 6f

/** Between the wall clock and the mode label, so the two read as separate lines rather than a block. */
private const val CLOCK_GAP_DP = 4f

/**
 * The next boundary, one swipe from the watch face — which is the point of a watch app like this one.
 *
 * It renders the same [TodayState] as every other surface, which is what stops the tile and the app
 * disagreeing, and draws the same session ring around the same countdown. What it does *not* share is
 * the drawing: ProtoLayout is a serialized layout tree rendered by the system launcher in another
 * process, so nothing from `core:designsystem`'s composables can be reused here. The tokens,
 * `AbitFormat` and `RingGeometry` can, and that is the second reason item 06 keeps them Compose-free.
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
                        .onFailure { completer.set(emptyTile(requestParams)) }
                }
            job.invokeOnCompletion { cause -> if (cause != null) completer.setCancelled() }
            "AbitTileService.onTileRequest"
        }

    private suspend fun buildTile(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        val now = clock.now()
        val schedules = scheduleRepository.observeSchedules().first()
        val overrides = dayOverrideRepository.observeFrom(now.date).first()
        return tile(requestParams, todayState(schedules, overrides, now), now)
    }

    /** What the tile shows when the plan could not be read: the same thing an empty plan shows. */
    private fun emptyTile(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        val now = clock.now()
        return tile(requestParams, todayState(emptyList(), emptyMap(), now), now)
    }

    /** The layout alone: no I/O, so this is the half that cannot fail. */
    private fun tile(
        requestParams: RequestBuilders.TileRequest,
        state: TodayState,
        now: LocalDateTime,
    ): TileBuilders.Tile {
        val live = TileCountdown(zone = clock.zone(), today = now.date)
        val layout =
            materialScope(
                context = this,
                deviceConfiguration = requestParams.deviceConfiguration,
                // Tangerine means Focus and mint means Break. A device-tinted scheme would repaint
                // that at random, which is why the app has no dynamic colour anywhere.
                allowDynamicTheme = false,
                defaultColorScheme = AbitTileColorScheme,
            ) {
                dial(state, live, now, requestParams.deviceConfiguration)
            }

        return TileBuilders.Tile
            .Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(freshnessMillis(state, live, now))
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

    /**
     * The whole tile: the ring, the mode label, the countdown and the caption, on the watch's own
     * black. No `primaryLayout` and no edge button — a tile with a button in it is a tile with a
     * smaller dial, and there is only ever one thing to do here, so the dial itself is the target.
     * Tapping anywhere opens the app.
     */
    private fun MaterialScope.dial(
        state: TodayState,
        live: TileCountdown,
        now: LocalDateTime,
        device: DeviceParameters,
    ): LayoutElementBuilders.LayoutElement {
        // A round screen is reported square-bounded; taking the smaller side is what keeps the ring
        // a circle on a watch that is not.
        val screen = minOf(device.screenWidthDp, device.screenHeightDp)
        val diameter = DimensionBuilders.dp(screen * (1f - 2 * RING_INSET_FRACTION))
        val running = state as? TodayState.Running
        val arcs = running?.let { ringArcs(it.session, it.sessionRemaining) } ?: RingArcs.Empty

        val ring =
            LayoutElementBuilders.Box
                .Builder()
                .setWidth(diameter)
                .setHeight(diameter)
                .addContent(ringTrack(RING_STROKE_DP))
        if (running != null) {
            ring.addContent(sessionRing(running.session, running.stage, arcs, live, RING_STROKE_DP))
        }
        val headline = state.headline(live, now)
        ring.addContent(
            LayoutElementBuilders.Column
                .Builder()
                // The wall clock, which a tile otherwise has none of: the launcher draws one over a
                // watch face but not over a tile, and the dial is where a glance expects to find it.
                // Dimmer than the mode label, because it is the one line here that is not about ABit.
                .addContent(
                    text(
                        live.clockText(now),
                        typography = Typography.BODY_EXTRA_SMALL,
                        color = AbitTokens.Dark.OUTLINE.layoutColor(),
                    ),
                ).addContent(
                    LayoutElementBuilders.Spacer
                        .Builder()
                        .setHeight(DimensionBuilders.dp(CLOCK_GAP_DP))
                        .build(),
                ).addContent(text(state.mode().layoutString, typography = Typography.LABEL_SMALL, color = state.modeColor()))
                .addContent(text(headline.text, typography = headline.typography))
                .addContent(
                    text(
                        state.caption(now).layoutString,
                        typography = Typography.BODY_EXTRA_SMALL,
                        color = AbitTokens.Dark.ON_SURFACE_VARIANT.layoutColor(),
                    ),
                ).build(),
        )

        return LayoutElementBuilders.Box
            .Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .setModifiers(
                LayoutModifier
                    .contentDescription(state.spoken(now))
                    .clickable(openAbit())
                    .toProtoLayoutModifiers(),
            ).addContent(ring.build())
            .build()
    }

    private fun openAbit(): ModifiersBuilders.Clickable =
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
            ).build()

    /** The next moment the layout itself has to change, clamped. See [MIN_FRESHNESS]. */
    private fun freshnessMillis(
        state: TodayState,
        live: TileCountdown,
        now: LocalDateTime,
    ): Long {
        val until =
            when (state) {
                is TodayState.Running -> live.remainingAt(LocalDateTime(now.date, state.nextBoundary), now)
                is TodayState.OffHours -> state.next?.let { live.remainingAt(LocalDateTime(it.date, it.at), now) }
                is TodayState.Skipped -> null
            } ?: MAX_FRESHNESS
        return until.coerceIn(MIN_FRESHNESS, MAX_FRESHNESS).inWholeMilliseconds
    }
}
