package com.pocketwidget.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketwidget.data.Category
import com.pocketwidget.data.Money
import com.pocketwidget.ui.AppViewModel
import com.pocketwidget.ui.Route
import com.pocketwidget.ui.components.IconAction
import com.pocketwidget.ui.components.PrimaryPill
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AddScreen(vm: AppViewModel, route: Route.AddExpense) {
    val categories by vm.categories.collectAsState()
    val settings by vm.settings.collectAsState()
    val isSaving by vm.isSaving.collectAsState()
    val saveError by vm.saveError.collectAsState()
    val currency = settings.currencyCode
    val digits = Money.minorDigits(currency)
    val context = LocalContext.current

    var amountText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf(route.categoryId.orEmpty()) }
    var occurredAt by rememberSaveable { mutableLongStateOf(route.occurredAt ?: System.currentTimeMillis()) }
    var suggestion by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(route.transactionId) {
        val id = route.transactionId ?: return@LaunchedEffect
        val transaction = vm.loadTransaction(id) ?: return@LaunchedEffect
        amountText = Money.toKeypad(transaction.amountMinor, transaction.currencyCode)
        note = transaction.note
        categoryId = transaction.categoryId
        occurredAt = transaction.occurredAt
    }

    LaunchedEffect(Unit) { vm.clearSaveError() }

    val activeCategories = categories.filter { !it.archived }
    val selected = categories.firstOrNull { it.id == categoryId } ?: activeCategories.firstOrNull()
    val selectedId = selected?.id.orEmpty()
    // Keep an archived category visible when editing an old transaction.
    val chipCategories = categories.filter { !it.archived || it.id == selectedId }

    LaunchedEffect(note) {
        suggestion = null
        if (note.length >= 3) {
            delay(280)
            suggestion = vm.categorizer.suggest(note, 0L)
        }
    }

    if (showDatePicker) {
        val date = Instant.ofEpochMilli(occurredAt).atZone(vm.zone()).toLocalDate()
        LaunchedEffect(Unit) {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    occurredAt = LocalDate.of(year, month + 1, day)
                        .atTime(12, 0)
                        .atZone(vm.zone())
                        .toInstant()
                        .toEpochMilli()
                    showDatePicker = false
                },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth,
            ).apply {
                setOnCancelListener { showDatePicker = false }
                show()
            }
        }
    }

    val amountMinor = Money.parseMinor(amountText, currency) ?: 0L
    val canSave = amountMinor > 0L && selected != null && !isSaving

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAction(Icons.Filled.Close, "Close") { if (!isSaving) vm.pop() }
            Text(
                text = if (route.transactionId == null) "New expense" else "Edit expense",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            if (route.transactionId != null) {
                IconAction(Icons.Filled.Delete, "Delete") { showDeleteDialog = true }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = Money.symbol(currency),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = amountText.ifBlank { "0" },
                style = MaterialTheme.typography.displayLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(14.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(chipCategories, key = { it.id }) { category ->
                CategoryChip(
                    category = category,
                    selected = category.id == selectedId,
                    onClick = { categoryId = category.id },
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                if (note.isEmpty()) {
                    Text(
                        text = "What was it? (optional)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = note,
                    onValueChange = { if (it.length <= 80) note = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PillButton(text = vm.dayLabel(occurredAt), onClick = { showDatePicker = true })
            val suggestionId = suggestion
            val suggestedCategory = categories.firstOrNull { it.id == suggestionId }
            if (suggestedCategory != null && suggestedCategory.id != selectedId) {
                PillButton(
                    text = "Suggest: ${suggestedCategory.emoji} ${suggestedCategory.name}",
                    onClick = { categoryId = suggestedCategory.id; suggestion = null },
                    accent = true,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        if (saveError != null) {
            Text(
                text = saveError.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Keypad(digits = digits, onKey = { key -> amountText = applyKey(amountText, key, digits) })
        Spacer(Modifier.height(12.dp))
        PrimaryPill(
            text = when {
                isSaving -> "Saving…"
                canSave -> "Log ${Money.format(amountMinor, currency)}"
                else -> "Log expense"
            },
            onClick = {
                if (canSave && selected != null) {
                    vm.saveTransaction(
                        id = route.transactionId,
                        amountMinor = amountMinor,
                        categoryId = selected.id,
                        note = note,
                        occurredAt = occurredAt,
                    )
                }
            },
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        )
        Spacer(Modifier.height(12.dp))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this expense?") },
            text = { Text("This removes it from your device. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    route.transactionId?.let(vm::deleteTransaction)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Keep") }
            },
        )
    }
}

@Composable
private fun CategoryChip(category: Category, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (selected) Color(category.color).copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) Color(category.color) else MaterialTheme.colorScheme.outlineVariant,
        ),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = category.emoji, fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PillButton(text: String, onClick: () -> Unit, accent: Boolean = false) {
    Surface(
        shape = CircleShape,
        color = if (accent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onClick,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (accent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun Keypad(digits: Int, onKey: (String) -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", if (digits > 0) "." else "", "0", "⌫")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(Modifier.weight(1f).height(56.dp))
                    } else {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            onClick = { onKey(key) },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Digits form the major unit until a decimal point is typed. */
internal fun applyKey(current: String, key: String, digits: Int): String = when {
    key == "⌫" -> current.dropLast(1)
    key == "." -> when {
        digits == 0 -> current
        current.contains('.') -> current
        current.isEmpty() -> "0."
        else -> "$current."
    }
    current.length >= 11 -> current
    digits > 0 && current.contains('.') && current.substringAfter('.').length >= digits -> current
    current == "0" -> key
    else -> current + key
}
