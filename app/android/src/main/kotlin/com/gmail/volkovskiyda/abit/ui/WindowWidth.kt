package com.gmail.volkovskiyda.abit.ui

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowWidthSizeClass

/**
 * Whether this window is wide enough for the design's two-pane layout. Medium and expanded both are:
 * the design draws one tablet layout, not one per breakpoint, and a foldable opened flat should get
 * it too.
 */
@Composable
fun isWideWindow(): Boolean = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT
