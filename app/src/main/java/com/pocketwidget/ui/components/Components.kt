package com.pocketwidget.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketwidget.data.BudgetStatus
import com.pocketwidget.data.Category
import com.pocketwidget.data.Money
import java.util.Locale

@Composable
fun statusColor(status: BudgetStatus): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (status) {
        BudgetStatus.NO_LIMIT -> MaterialTheme.colorScheme.onSurfaceVariant
        BudgetStatus.ON_TRACK -> if (dark) Color(0xFF9FD3B4) else Color(0xFF2F6F4E)
        BudgetStatus.CLOSE_TO_LIMIT -> if (dark) Color(0xFFE9C46A) else Color(0xFFA66A12)
        BudgetStatus.OVER -> if (dark) Color(0xFFF2B8B5) else Color(0xFFB3261E)
    }
}

private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

@Composable
fun StatusPill(status: BudgetStatus, modifier: Modifier = Modifier) {
    val color = statusColor(status)
    val label = when (status) {
        BudgetStatus.NO_LIMIT -> "No limit"
        BudgetStatus.ON_TRACK -> "On track"
        BudgetStatus.CLOSE_TO_LIMIT -> "Close"
        BudgetStatus.OVER -> "Over"
    }
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color.copy(alpha = 0.14f),
    ) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp, fontWeight = FontWeight.Bold),
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
fun AmountText(
    cents: Long,
    currencyCode: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fractionStyle: TextStyle? = null,
) {
    val (whole, fraction) = Money.parts(cents, currencyCode)
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text = whole,
            style = style.copy(color = color, fontFeatureSettings = "tnum"),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (fraction.isNotEmpty()) {
            Text(
                text = fraction,
                style = (fractionStyle ?: style).copy(
                    color = if (color == Color.Unspecified) color else color.copy(alpha = 0.72f),
                    fontFeatureSettings = "tnum",
                ),
            )
        }
    }
}

@Composable
fun LimitMeter(
    progress: Float,
    status: BudgetStatus,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), animationSpec = tween(650), label = "meter")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = statusColor(status)
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = track, cornerRadius = radius, size = size)
        if (animated > 0.001f) {
            drawRoundRect(
                color = fill,
                cornerRadius = radius,
                size = Size((size.width * animated).coerceAtLeast(size.height), size.height),
            )
        }
        // Quarter ticks give the bar a scale without adding clutter.
        val tick = Color.Black.copy(alpha = 0.10f)
        listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
            val x = size.width * fraction
            drawLine(tick, Offset(x, 1.5f), Offset(x, size.height - 1.5f), strokeWidth = 1f)
        }
    }
}

@Composable
fun CategoryDot(category: Category, size: Dp = 40.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(category.color).copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = category.emoji, fontSize = (size.value * 0.44f).sp)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun IconAction(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
    }
}

@Composable
fun EmptyState(emoji: String, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = emoji, fontSize = 30.sp)
        Spacer(Modifier.height(10.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PrimaryPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: String? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        onClick = { if (enabled) onClick() },
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
