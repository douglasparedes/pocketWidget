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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketwidget.data.Category
import com.pocketwidget.data.DefaultCategories
import com.pocketwidget.ui.AppViewModel
import com.pocketwidget.ui.components.CategoryDot
import com.pocketwidget.ui.components.IconAction

@Composable
fun CategoriesScreen(vm: AppViewModel) {
    val categories by vm.categories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconAction(Icons.Filled.ArrowBack, "Back") { vm.pop() }
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            IconAction(Icons.Filled.Add, "New category") { showAddDialog = true }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "Archived categories stay on old expenses but disappear from the quick picker.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 10.dp),
            )
            categories.forEach { category ->
                CategoryRow(
                    category = category,
                    enabled = category.id != DefaultCategories.FALLBACK_ID,
                    onArchivedChange = { archived -> vm.setCategoryArchived(category.id, archived) },
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showAddDialog) {
        AddCategoryDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, emoji, color ->
                vm.saveCategory(name, emoji, color)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun CategoryRow(category: Category, enabled: Boolean, onArchivedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryDot(category, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(text = category.name, style = MaterialTheme.typography.titleMedium)
            if (category.archived) {
                Text(
                    text = "Archived",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = !category.archived,
            onCheckedChange = { checked -> onArchivedChange(!checked) },
            enabled = enabled,
        )
    }
}

@Composable
private fun AddCategoryDialog(onDismiss: () -> Unit, onSave: (String, String, Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf(DefaultCategories.EMOJI_CHOICES.first()) }
    var color by remember { mutableStateOf(DefaultCategories.COLOR_CHOICES.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New category") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 24) name = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
                Spacer(Modifier.height(14.dp))
                Text(text = "Icon", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                DefaultCategories.EMOJI_CHOICES.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { choice ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (choice == emoji) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent,
                                    )
                                    .clickable { emoji = choice },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = choice, fontSize = 18.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(text = "Color", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DefaultCategories.COLOR_CHOICES.forEach { choice ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(choice))
                                .clickable { color = choice },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (choice == color) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, emoji, color) },
                enabled = name.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
