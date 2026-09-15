package com.gmail.volkovskiyda.abit.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Offered, never imposed: this appears at the top of Settings and as a sheet from the sync badge, and
 * **never as a modal on launch**. Without an account the app is fully usable — it simply keeps
 * everything on the device, which is what the outline cloud says.
 */
@Composable
fun SignInCard(
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Sync across your devices",
    body: String = "Sign in with Google to keep the same schedules on your phone, watch, Mac and browser.",
    buttonLabel: String = "Sign in with Google",
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onSignIn) { Text(buttonLabel) }
    }
}
