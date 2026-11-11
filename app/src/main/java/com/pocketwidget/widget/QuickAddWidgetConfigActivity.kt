package com.pocketwidget.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketwidget.PocketApp
import com.pocketwidget.data.Category
import com.pocketwidget.ui.components.PrimaryPill
import com.pocketwidget.ui.theme.PocketWidgetTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickAddWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val app = application as PocketApp
        setContent {
            PocketWidgetTheme {
                ConfigScreen(
                    app = app,
                    appWidgetId = appWidgetId,
                    onCancel = { finish() },
                    onSave = { ids ->
                        app.container.settingsStore.setQuickCategoryIds(appWidgetId, ids)
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                WidgetRefresh.updateQuickAdd(
                                    this@QuickAddWidgetConfigActivity,
                                    intArrayOf(appWidgetId),
                                )
                            } finally {
                                withContext(Dispatchers.Main) {
                                    setResult(
                                        RESULT_OK,
                                        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                                    )
                                    finish()
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ConfigScreen(
    app: PocketApp,
    appWidgetId: Int,
    onCancel: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    val categories by produceState(initialValue = emptyList<Category>()) {
        value = app.container.repository.categories.first().filterNot { it.archived }
    }
    var selected by remember { mutableStateOf(app.container.settingsStore.quickCategoryIds(appWidgetId)) }

    LaunchedEffect(categories) {
        if (selected.isEmpty() && categories.isNotEmpty()) {
            selected = categories.take(4).map { it.id }
        }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(text = "Quick add buttons", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Choose up to four categories. Each button opens a pre-filled expense.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))

            LazyColumn(Modifier.weight(1f)) {
                items(categories, key = { it.id }) { category ->
                    val checked = selected.contains(category.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = category.emoji, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                selected = when {
                                    isChecked && selected.size < 4 -> selected + category.id
                                    !isChecked -> selected - category.id
                                    else -> selected
                                }
                            },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                PrimaryPill(
                    text = if (selected.isEmpty()) "Use defaults" else "Add widget",
                    onClick = { onSave(selected) },
                    modifier = Modifier.weight(1.6f),
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
