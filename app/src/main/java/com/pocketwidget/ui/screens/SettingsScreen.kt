package com.pocketwidget.ui.screens

import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.pocketwidget.BuildConfig
import com.pocketwidget.data.Currencies
import com.pocketwidget.data.Money
import com.pocketwidget.data.Settings
import com.pocketwidget.data.WidgetAccent
import com.pocketwidget.data.WidgetSnapshot
import com.pocketwidget.data.WidgetTheme
import com.pocketwidget.ui.AppViewModel
import com.pocketwidget.ui.Route
import com.pocketwidget.ui.components.IconAction
import com.pocketwidget.ui.components.SettingRow
import com.pocketwidget.ui.components.SwitchRow
import com.pocketwidget.widget.WidgetRenderer
import java.time.LocalDate

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val settings by vm.settings.collectAsState()
    val summary by vm.summary.collectAsState()
    val month by vm.month.collectAsState()
    val categories by vm.categories.collectAsState()
    val transactionCount by vm.transactionCount.collectAsState()
    val context = LocalContext.current

    var showLimitDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showCurrencyLockedDialog by remember { mutableStateOf(false) }
    var showDeleteDataDialog by remember { mutableStateOf(false) }
    var limitText by remember { mutableStateOf("") }

    val currencyLocked = transactionCount > 0

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            vm.exportCsv(uri) { ok ->
                Toast.makeText(
                    context,
                    if (ok) "CSV exported" else "Export failed",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val snapshot = WidgetSnapshot(
        month = month,
        totalCents = summary.totalCents,
        limitCents = summary.limitCents,
        currencyCode = settings.currencyCode,
        topCategory = summary.byCategory.firstOrNull(),
        otherCategoryCount = (summary.byCategory.size - 1).coerceAtLeast(0),
        status = summary.status,
        progress = summary.progress,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAction(Icons.Filled.ArrowBack, "Back") { vm.pop() }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("BUDGET")
        SettingRow(
            title = "Monthly limit",
            value = if (settings.monthlyLimitCents > 0) {
                Money.format(settings.monthlyLimitCents, settings.currencyCode, showDecimalsWhenWhole = false)
            } else {
                "Not set"
            },
            onClick = {
                limitText = if (settings.monthlyLimitCents > 0) {
                    Money.toKeypad(settings.monthlyLimitCents, settings.currencyCode)
                } else {
                    ""
                }
                showLimitDialog = true
            },
        )
        SettingRow(
            title = "Currency",
            value = if (currencyLocked) {
                "${settings.currencyCode} · locked"
            } else {
                "${settings.currencyCode} · ${Money.symbol(settings.currencyCode)}"
            },
            onClick = {
                if (currencyLocked) showCurrencyLockedDialog = true else showCurrencyDialog = true
            },
        )
        if (currencyLocked) {
            Text(
                text = "Currency is fixed once you have expenses, so history is never relabelled.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("WIDGET")
        Text(text = "Theme", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        ThemeSelector(selected = settings.widgetTheme) { vm.setWidgetTheme(it) }
        Spacer(Modifier.height(16.dp))
        Text(text = "Accent", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        AccentSelector(selected = settings.widgetAccent) { vm.setWidgetAccent(it) }

        SwitchRow(
            title = "Show amounts",
            subtitle = "Hide the numbers when someone is looking over your shoulder",
            checked = settings.showWidgetAmounts,
            onCheckedChange = { vm.setShowWidgetAmounts(it) },
        )
        SwitchRow(
            title = "Limit meter",
            subtitle = "Draw the budget bar under the total",
            checked = settings.showWidgetMeter,
            onCheckedChange = { vm.setShowWidgetMeter(it) },
        )

        Spacer(Modifier.height(10.dp))
        Text(
            text = "Live preview",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        WidgetPreviewCard(snapshot = snapshot, settings = settings)

        Spacer(Modifier.height(22.dp))
        SectionLabel("DATA")
        SettingRow(
            title = "Categories",
            value = "${categories.count { !it.archived }} active",
            onClick = { vm.navigate(Route.Categories) },
        )
        SettingRow(
            title = "Export CSV",
            value = "All transactions",
            onClick = {
                exportLauncher.launch("pocketwidget-export-${LocalDate.now()}.csv")
            },
        )
        SettingRow(
            title = "Delete all data",
            value = "Export first",
            onClick = { showDeleteDataDialog = true },
        )

        Spacer(Modifier.height(22.dp))
        SectionLabel("PRIVACY")
        PrivacyCard()

        if (vm.isDebuggable()) {
            Spacer(Modifier.height(22.dp))
            SectionLabel("DEVELOPER")
            SettingRow(
                title = "Seed sample month",
                value = "Debug builds only",
                onClick = { vm.seedDemoData() },
            )
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = "PocketWidget ${BuildConfig.VERSION_NAME} · A private spending widget",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
    }

    if (showLimitDialog) {
        val parsed = Money.parseMinor(limitText, settings.currencyCode)
        val valid = limitText.isBlank() || parsed != null
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text("Monthly limit") },
            text = {
                Column {
                    Text(
                        text = "The widget turns the limit into a gauge. Leave it empty to remove the limit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { input ->
                            limitText = input.filter { it.isDigit() || it == '.' }
                                .take(if (Money.minorDigits(settings.currencyCode) > 0) 14 else 9)
                        },
                        singleLine = true,
                        isError = !valid,
                        prefix = { Text(Money.symbol(settings.currencyCode)) },
                        supportingText = {
                            if (!valid) {
                                Text("Enter a valid amount, for example 1500")
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    if (settings.monthlyLimitCents > 0) {
                        TextButton(onClick = {
                            vm.setMonthlyLimit(0L)
                            showLimitDialog = false
                        }) { Text("Remove limit") }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = valid,
                    onClick = {
                        vm.setMonthlyLimit(if (limitText.isBlank()) 0L else parsed ?: 0L)
                        showLimitDialog = false
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Currency") },
            text = {
                Column {
                    Text(
                        text = "Changing the currency before your first expense also resets the monthly limit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                        Currencies.COMMON.forEach { (code, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        vm.setCurrency(code)
                                        showCurrencyDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "$code  $label",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                if (code == settings.currencyCode) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) { Text("Close") }
            },
        )
    }

    if (showCurrencyLockedDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyLockedDialog = false },
            title = { Text("Currency is locked") },
            text = {
                Text(
                    text = "You already have expenses in ${settings.currencyCode}. Changing the currency now " +
                        "would relabel them. Export your data and delete it if you need to switch.",
                )
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyLockedDialog = false }) { Text("Got it") }
            },
        )
    }

    if (showDeleteDataDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDataDialog = false },
            title = { Text("Delete all data?") },
            text = {
                Text(
                    text = "This permanently removes every expense from this device and resets the monthly " +
                        "limit. Categories are kept. Export a CSV first if you want a copy.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDataDialog = false
                    vm.clearAllData()
                    Toast.makeText(context, "All expenses deleted", Toast.LENGTH_SHORT).show()
                }) { Text("Delete everything") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDataDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun ThemeSelector(selected: WidgetTheme, onSelect: (WidgetTheme) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        WidgetTheme.entries.forEach { theme ->
            val active = theme == selected
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelect(theme) },
            ) {
                Text(
                    text = theme.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 9.dp),
                )
            }
        }
    }
}

@Composable
private fun AccentSelector(selected: WidgetAccent, onSelect: (WidgetAccent) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WidgetAccent.entries.forEach { accent ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(accent.argb))
                    .clickable { onSelect(accent) },
                contentAlignment = Alignment.Center,
            ) {
                if (accent == selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = accent.label,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetPreviewCard(snapshot: WidgetSnapshot, settings: Settings) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        AndroidView(
            factory = { context -> FrameLayout(context) },
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp)
                .padding(10.dp),
            update = { frame ->
                frame.removeAllViews()
                val views = WidgetRenderer.monthRemoteViews(
                    context = frame.context,
                    snapshot = snapshot,
                    settings = settings,
                    widthDp = 300,
                )
                frame.addView(views.apply(frame.context, frame))
            },
        )
    }
}

@Composable
private fun PrivacyCard() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = "Nothing leaves this device",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            listOf(
                "No internet permission, no account, no ads",
                "Android cloud backup and device transfer are disabled",
                "Transactions live in a local database",
                "CSV export is the only way data leaves",
            ).forEach { line ->
                Row(Modifier.padding(vertical = 2.dp)) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(12.dp),
                    )
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
