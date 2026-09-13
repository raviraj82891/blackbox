package com.example.blackbox.quicksettings

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.blackbox.service.BlackboxForegroundService
import com.example.blackbox.service.ProtectionState
import com.example.blackbox.service.ProtectionStateManager
import com.example.blackbox.util.PermissionValidator
import dagger.hilt.android.AndroidEntryPoint

/**
 * Quick Settings Tile Service — Reflects real protection state and allows instant Manual SOS trigger.
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
            label = "TRACE SOS Triggered"
            updateTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val currentState = ProtectionStateManager.state.value
        val hasPermissions = PermissionValidator.isAllRequiredGranted(this)

        qsTile?.apply {
            when {
                !hasPermissions || currentState == ProtectionState.PERMISSION_LIMITED -> {
                    label = "TRACE: Limited"
                    state = Tile.STATE_INACTIVE
                }
                currentState == ProtectionState.ACTIVE -> {
                    label = "TRACE: Protected"
                    state = Tile.STATE_ACTIVE
                }
                currentState == ProtectionState.PAUSED -> {
                    label = "TRACE: Paused"
                    state = Tile.STATE_INACTIVE
                }
                else -> {
                    label = "TRACE: Attention"
                    state = Tile.STATE_UNAVAILABLE
                }
            }
            updateTile()
        }
    }
}
