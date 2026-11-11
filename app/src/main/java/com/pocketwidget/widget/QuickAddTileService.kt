package com.pocketwidget.widget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.pocketwidget.R
import com.pocketwidget.ui.MainActivity

/**
 * A Quick Settings tile is the universal "lock screen speed" path: it is reachable
 * from the lock screen shade on every Android 7+ device, unlike third-party
 * lock-screen widgets, which stock Android does not host.
 */
class QuickAddTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            label = "Log expense"
            icon = Icon.createWithResource(this@QuickAddTileService, R.drawable.ic_tile)
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("pocketwidget://add")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
