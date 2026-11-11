package com.pocketwidget.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.pocketwidget.data.BudgetStatus
import com.pocketwidget.data.Money
import com.pocketwidget.data.Transaction
import com.pocketwidget.ui.AppViewModel
import com.pocketwidget.ui.Route
import com.pocketwidget.ui.components.AmountText
import com.pocketwidget.ui.components.CategoryDot
import com.pocketwidget.ui.components.EmptyState
import com.pocketwidget.ui.components.IconAction
import com.pocketwidget.ui.components.LimitMeter
import com.pocketwidget.ui.components.PrimaryPill
import com.pocketwidget.ui.components.SectionHeader
import com.pocketwidget.ui.components.StatusPill
import com.pocketwidget.ui.components.statusColor
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(vm: AppViewModel) {
    val summary by vm.summary.collectAsState()
    val settings by vm.settings.collectAsState()
    val month by vm.month.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val categories by vm.categories.collectAsState()

    val currency = settings.currencyCode
    val categoryById = categories.associateBy { it.id }
    val monthName = month.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())).uppercase(Locale.getDefault())

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = month.year.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconAction(Icons.Filled.KeyboardArrowLeft, "Previous month") { vm.previousMonth() }
                IconAction(Icons.Filled.KeyboardArrowRight, "Next month") { vm.nextMonth() }
                IconAction(Icons.Filled.Settings, "Settings") { vm.navigate(Route.Settings) }
            }

            Spacer(Modifier.height(18.dp))

            SummaryCard(
                totalCents = summary.totalCents,
                limitCents = summary.limitCents,
                progress = summary.progress,
                status = summary.status,
                remainingCents = summary.remainingCents,
                overByCents = summary.overByCents,
                projectedCents = summary.projectedCents,
                dailyAverageCents = summary.dailyAverageCents,
                currency = currency,
                isCurrentMonth = month == YearMonth.now(),
                periodLabel = if (month == YearMonth.now()) "SPENT THIS MONTH" else "SPENT IN ${monthName}",
                onSetLimit = { vm.navigate(Route.Settings) },
            )

            if (summary.byCategory.isNotEmpty()) {
                Spacer(Modifier.height(26.dp))
                SectionHeader(title = "Where it went")
                Spacer(Modifier.height(6.dp))
                summary.byCategory.take(4).forEach { spend ->
                    CategoryRow(name = spend.category.name, emoji = spend.category.emoji, color = spend.category.color, cents = spend.cents, share = spend.share, currency = currency)
                }
                if (summary.byCategory.size > 4) {
                    val restCents = summary.byCategory.drop(4).sumOf { it.cents }
                    Text(
                        text = "+ ${summary.byCategory.size - 4} more · ${Money.format(restCents, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader(
                title = "Recent",
                actionLabel = if (transactions.isNotEmpty()) "See all" else null,
                onAction = { vm.navigate(Route.History) },
            )
            Spacer(Modifier.height(4.dp))

            if (transactions.isEmpty()) {
                EmptyState(
                    emoji = "🧾",
                    title = "Nothing logged this month",
                    body = "Tap Log expense and it is saved on this device. No bank, no account.",
                )
            } else {
                transactions.take(4).forEach { transaction ->
                    val category = categoryById[transaction.categoryId]
                    RecentRow(
                        transaction = transaction,
                        categoryName = category?.name ?: "Unknown",
                        emoji = category?.emoji ?: "•",
                        color = category?.color ?: 0xFF9A8C98,
                        currency = currency,
                        dayLabel = vm.dayLabel(transaction.occurredAt),
                        onClick = { vm.navigate(Route.AddExpense(transactionId = transaction.id)) },
                    )
                }
            }

            Spacer(Modifier.height(108.dp))
        }

        PrimaryPill(
            text = "Log expense",
            icon = "+",
            onClick = { vm.navigate(Route.AddExpense()) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun SummaryCard(
    totalCents: Long,
    limitCents: Long,
    progress: Float,
    status: BudgetStatus,
    remainingCents: Long,
    overByCents: Long,
    projectedCents: Long,
    dailyAverageCents: Long,
    currency: String,
    isCurrentMonth: Boolean,
    periodLabel: String,
    onSetLimit: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = periodLabel,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (limitCents > 0) StatusPill(status)
            }
            Spacer(Modifier.height(10.dp))
            AmountText(
                cents = totalCents,
                currencyCode = currency,
                style = MaterialTheme.typography.displayMedium,
                fractionStyle = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(2.dp))

            if (limitCents <= 0) {
                Text(
                    text = "No limit set — add one for a glanceable gauge.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Set a monthly limit",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSetLimit)
                        .padding(vertical = 4.dp),
                )
            } else {
                Text(
                    text = "of ${Money.format(limitCents, currency)} limit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(18.dp))
                LimitMeter(progress = progress, status = status)
                Spacer(Modifier.height(10.dp))
                Row {
                    Text(
                        text = if (overByCents > 0) "${Money.format(overByCents, currency)} over" else "${Money.format(remainingCents, currency)} left",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (overByCents > 0) statusColor(status) else MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = if (isCurrentMonth) "~${Money.format(projectedCents, currency)} projected" else "avg ${Money.format(dailyAverageCents, currency)}/day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    name: String,
    emoji: String,
    color: Long,
    cents: Long,
    share: Float,
    currency: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryDot(com.pocketwidget.data.Category("", name, emoji, color, 0), size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = Money.format(cents, currency), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(share.coerceIn(0.02f, 1f))
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(color)),
                )
            }
        }
    }
}

@Composable
private fun RecentRow(
    transaction: Transaction,
    categoryName: String,
    emoji: String,
    color: Long,
    currency: String,
    dayLabel: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryDot(com.pocketwidget.data.Category("", categoryName, emoji, color, 0), size = 38.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = transaction.note.ifBlank { categoryName },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$dayLabel · $categoryName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AmountText(cents = transaction.amountMinor, currencyCode = currency, style = MaterialTheme.typography.titleMedium)
    }
}
