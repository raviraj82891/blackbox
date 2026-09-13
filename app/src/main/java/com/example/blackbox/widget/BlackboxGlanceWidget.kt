package com.example.blackbox.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.blackbox.service.BlackboxForegroundService
import com.example.blackbox.service.ProtectionState
import com.example.blackbox.service.ProtectionStateManager
import com.example.blackbox.util.PermissionValidator

class BlackboxGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val currentState = ProtectionStateManager.state.value
        val hasPermissions = PermissionValidator.isAllRequiredGranted(context)

        val (statusLabel, statusDetail) = when {
            !hasPermissions || currentState == ProtectionState.PERMISSION_LIMITED -> Pair("LIMITED", "Missing Permissions")
            currentState == ProtectionState.ACTIVE -> Pair("PROTECTED", "60-Min Buffer Active")
            currentState == ProtectionState.PAUSED -> Pair("PAUSED", "Protection Paused")
            else -> Pair("ATTENTION REQUIRED", "Service Not Active")
        }

        provideContent {
            GlanceTheme {
                WidgetContent(context, statusLabel, statusDetail)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context, statusLabel: String, statusDetail: String) {
        val sosIntent = Intent(context, BlackboxForegroundService::class.java).apply {
            action = BlackboxForegroundService.ACTION_MANUAL_SOS
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(14.dp)
                .background(GlanceTheme.colors.surface),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "TRACE Status:",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = statusLabel,
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp)
                )
            }

            Text(
                text = statusDetail,
                style = TextStyle(fontSize = 10.sp)
            )

            Spacer(modifier = GlanceModifier.height(10.dp))

            Button(
                text = "INSTANT SOS",
                onClick = actionStartService(sosIntent),
                modifier = GlanceModifier.fillMaxWidth()
            )
        }
    }
}

class BlackboxGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BlackboxGlanceWidget()
}
