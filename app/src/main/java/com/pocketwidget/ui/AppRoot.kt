package com.pocketwidget.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.pocketwidget.ui.screens.AddScreen
import com.pocketwidget.ui.screens.CategoriesScreen
import com.pocketwidget.ui.screens.HistoryScreen
import com.pocketwidget.ui.screens.HomeScreen
import com.pocketwidget.ui.screens.SettingsScreen

@Composable
fun AppRoot(vm: AppViewModel) {
    val stack by vm.backStack.collectAsState()
    val route = stack.lastOrNull() ?: Route.Home

    BackHandler(enabled = stack.size > 1) { vm.pop() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (route) {
            Route.Home -> HomeScreen(vm)
            Route.History -> HistoryScreen(vm)
            Route.Settings -> SettingsScreen(vm)
            Route.Categories -> CategoriesScreen(vm)
            is Route.AddExpense -> AddScreen(vm, route)
        }
    }
}
