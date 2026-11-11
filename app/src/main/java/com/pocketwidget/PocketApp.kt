package com.pocketwidget

import android.app.Application
import android.content.Context
import com.pocketwidget.data.PocketDatabase
import com.pocketwidget.data.SettingsStore
import com.pocketwidget.data.SpendingRepository
import com.pocketwidget.widget.WidgetRefresh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class AppContainer(context: Context) {
    val settingsStore = SettingsStore(context)
    private val database = PocketDatabase.build(context)
    val repository = SpendingRepository(database, settingsStore)
}

class PocketApp : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationJob = SupervisorJob()
    val appScope: CoroutineScope = CoroutineScope(Dispatchers.Default) + applicationJob

    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 16)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            refreshRequests.debounce(350L).collect {
                WidgetRefresh.updateAll(this@PocketApp)
            }
        }
        // Refresh widgets independently of any screen/ViewModel lifecycle.
        appScope.launch {
            container.repository.dataChanges.collect { requestWidgetRefresh() }
        }
        appScope.launch {
            container.settingsStore.settings.drop(1).collect { requestWidgetRefresh() }
        }
    }

    /** Coalesces rapid writes into a single widget refresh. */
    fun requestWidgetRefresh() {
        refreshRequests.tryEmit(Unit)
    }
}
