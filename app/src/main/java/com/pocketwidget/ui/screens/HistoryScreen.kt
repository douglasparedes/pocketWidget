package com.pocketwidget.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketwidget.data.Money
import com.pocketwidget.data.Transaction
import com.pocketwidget.ui.AppViewModel
import com.pocketwidget.ui.Route
import com.pocketwidget.ui.components.AmountText
import com.pocketwidget.ui.components.CategoryDot
import com.pocketwidget.ui.components.EmptyState
import com.pocketwidget.ui.components.IconAction
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(vm: AppViewModel) {
    val transactions by vm.transactions.collectAsState()
    val categories by vm.categories.collectAsState()
    val month by vm.month.collectAsState()
    val settings by vm.settings.collectAsState()
    val currency = settings.currencyCode
    val byId = categories.associateBy { it.id }

    val grouped = remember(transactions) {
        transactions.groupBy { vm.localDate(it.occurredAt) }.toSortedMap(compareByDescending { it })
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconAction(Icons.Filled.ArrowBack, "Back") { vm.pop() }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(text = "History", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconAction(Icons.Filled.KeyboardArrowLeft, "Previous month") { vm.previousMonth() }
            IconAction(Icons.Filled.KeyboardArrowRight, "Next month") { vm.nextMonth() }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        ) {
            if (transactions.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🗓",
                        title = "No expenses in this month",
                        body = "Switch months above, or log an expense to fill this in.",
                    )
                }
            }
            grouped.forEach { (date, dayTransactions) ->
                item(key = "header-$date") {
                    DayHeader(
                        totalCents = dayTransactions.sumOf { it.amountMinor },
                        currency = currency,
                        label = vm.dayLabel(dayTransactions.first().occurredAt),
                    )
                }
                items(dayTransactions, key = { it.id }) { transaction ->
                    val category = byId[transaction.categoryId]
                    HistoryRow(
                        transaction = transaction,
                        categoryName = category?.name ?: "Unknown",
                        emoji = category?.emoji ?: "•",
                        color = category?.color ?: 0xFF9A8C98,
                        currency = currency,
                        onClick = { vm.navigate(Route.AddExpense(transactionId = transaction.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayHeader(totalCents: Long, currency: String, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = Money.format(totalCents, currency, showDecimalsWhenWhole = false),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistoryRow(
    transaction: Transaction,
    categoryName: String,
    emoji: String,
    color: Long,
    currency: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryDot(com.pocketwidget.data.Category("", categoryName, emoji, color, 0), size = 38.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = transaction.note.ifBlank { categoryName },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = categoryName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AmountText(
            cents = transaction.amountMinor,
            currencyCode = currency,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
