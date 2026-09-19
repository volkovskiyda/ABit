package com.gmail.volkovskiyda.abit.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A settings row that states a fact the user cannot act on: what it is, and underneath, what it
 * says. Deliberately the left-hand column of [PermissionRow] and nothing else, so a line the screen
 * only reports — the version — sits on the same two-line grid as the lines it can change, rather
 * than as a sentence of its own that happens to contain a number.
 *
 * Unlike [PermissionRow] it carries no vertical padding. That padding is what keeps two permission
 * rows off each other; a row that is the whole of its section has nothing to be kept off, and the
 * padding only pushed the value away from the section header that names it.
 */
@Composable
fun InfoRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
