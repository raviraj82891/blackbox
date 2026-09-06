package com.example.blackbox.quicksettings

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.blackbox.service.BlackboxForegroundService
import dagger.hilt.android.AndroidEntryPoint

/**
 * Quick Settings Tile Service — Allows instant Manual SOS trigger from the Android System Quick Settings shade.
 */
@AndroidEntryPoint
class SosTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, BlackboxForegroundService::class.java).apply {
            action = BlackboxForegroundService.ACTION_MANUAL_SOS
        }
        startService(intent)

        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            label = "TRACE SOS"
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }
}
