package com.example.blackbox.quicksettings

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.blackbox.service.BlackboxForegroundService

/**
 * Quick Settings Tile Service — Allows instant Manual SOS trigger from the Android System Quick Settings shade.
 * Satisfies the requirement:
 * "Manual SOS: dedicated in-app button + a Quick Settings tile, since Android does not
 * allow reliably intercepting hardware power-button chords without an accessibility service."
 */
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
            label = "Blackbox SOS"
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }
}
