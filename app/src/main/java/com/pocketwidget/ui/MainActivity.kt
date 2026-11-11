package com.pocketwidget.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.pocketwidget.ui.theme.PocketWidgetTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: AppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[AppViewModel::class.java]
        if (savedInstanceState == null) {
            handleIntent(intent)
        }
        setContent {
            PocketWidgetTheme {
                AppRoot(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "pocketwidget") return
        val route = when (data.host) {
            "add" -> Route.AddExpense(categoryId = data.getQueryParameter("category"))
            "history" -> Route.History
            "settings" -> Route.Settings
            else -> Route.Home
        }
        viewModel.openFromIntent(route)
    }
}
