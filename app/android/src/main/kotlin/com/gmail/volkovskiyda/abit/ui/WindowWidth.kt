package com.gmail.volkovskiyda.abit.ui

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

/**
 * Whether this window is wide enough for the design's two-pane layout. Medium and expanded both are:
 * the design draws one tablet layout, not one per breakpoint, and a foldable opened flat should get
 * it too.
 *
 * A breakpoint rather than a size class, because `WindowWidthSizeClass` and its `COMPACT` constant
 * are deprecated and will not be developed further; `isWidthAtLeastBreakpoint` is what androidx
 * points at instead. "At least the medium lower bound" is 600dp, which is exactly where `COMPACT`
 * used to end, so the question asked and the answer given are unchanged.
 *
 * `currentWindowAdaptiveInfoV2` rather than `currentWindowAdaptiveInfo` for the same reason: the
 * older one is deprecated for computing its size class off breakpoints that stop before large and
 * extra-large. V2 only adds buckets above 840dp, all of which this predicate already answered `true`
 * for, so nothing about the layout moves.
 */
@Composable
fun isWideWindow(): Boolean =
    currentWindowAdaptiveInfoV2()
        .windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
