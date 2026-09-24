package app.elevon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.elevon.hid.HidConnectionState
import app.elevon.hid.HidUiState

/** The connection pill in the header: dot + label. */
@Composable
fun ConnectionPill(state: HidUiState, modifier: Modifier = Modifier) {
    val (label, color) = when (state.connectionState) {
        HidConnectionState.CONNECTED -> "Connected" to MaterialTheme.colorScheme.tertiary
        HidConnectionState.CONNECTING -> "Connecting…" to MaterialTheme.colorScheme.secondary
        HidConnectionState.READY -> "Available" to MaterialTheme.colorScheme.secondary
        HidConnectionState.OFFLINE -> "Bluetooth off" to MaterialTheme.colorScheme.secondary
        HidConnectionState.DISCONNECTED -> "Disconnected" to MaterialTheme.colorScheme.secondary
        HidConnectionState.UNSUPPORTED -> "Not supported" to MaterialTheme.colorScheme.error
    }
    Row(
        modifier = modifier
            .semantics { contentDescription = "Connection state: $label" }
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Honesty chip: Core / Experimental / Limited. A visible part of the product,
 * not small print (docs/research.md, "Feature labels carry an honesty chip").
 */
enum class Honesty { CORE, EXPERIMENTAL, LIMITED }

@Composable
fun HonestyChip(kind: Honesty, modifier: Modifier = Modifier) {
    val (label, color) = when (kind) {
        Honesty.CORE -> "Core" to MaterialTheme.colorScheme.tertiary
        Honesty.EXPERIMENTAL -> "Experimental" to MaterialTheme.colorScheme.secondary
        Honesty.LIMITED -> "Limited" to MaterialTheme.colorScheme.error
    }
    Box(
        modifier = modifier
            .border(1.dp, color, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, color = color, fontSize = 11.sp, letterSpacing = 0.4.sp)
    }
}

/** Big tactile keycap used across keyboard mode. */
@Composable
fun Keycap(
    label: String,
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    locked: Boolean = false,
    sticky: Boolean = false,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    val border = when {
        locked -> MaterialTheme.colorScheme.primary
        sticky -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        pressed -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val background = when {
        locked -> MaterialTheme.colorScheme.primaryContainer
        pressed -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Box(
        modifier = modifier
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .background(background, RoundedCornerShape(14.dp))
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Surface(
            onClick = onClick,
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth().height(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (locked) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(5.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                }
                Text(
                    label,
                    color = tint,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Empty / explanation card used for states across the app. */
@Composable
fun StateCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
