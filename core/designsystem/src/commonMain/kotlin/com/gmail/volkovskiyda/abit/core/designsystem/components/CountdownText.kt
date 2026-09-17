package com.gmail.volkovskiyda.abit.core.designsystem.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.gmail.volkovskiyda.abit.core.designsystem.countdown
import com.gmail.volkovskiyda.abit.core.designsystem.tabular
import kotlin.time.Duration

/** The one loud element on any screen. Tabular by construction, so the digits do not jitter. */
@Composable
fun CountdownText(
    remaining: Duration,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayLarge,
) {
    Text(
        text = countdown(remaining),
        style = style.tabular,
        modifier = modifier,
    )
}

/** A time rendered with the same tabular treatment, for captions like "09:45 · ends 10:00". */
@Composable
fun TabularText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
) {
    Text(text = text, style = style.tabular, modifier = modifier)
}
