package com.example.blackbox.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.blackbox.R
import com.example.blackbox.data.db.EmergencyContact
import com.example.blackbox.data.repository.BlackboxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    application: Application,
    private val repository: BlackboxRepository
) : AndroidViewModel(application) {

    val contacts: StateFlow<List<EmergencyContact>> = repository.getAllEmergencyContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        createTestNotificationChannel()
    }

    fun addContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.insertEmergencyContact(contact)
        }
    }

    fun removeContact(id: String) {
        viewModelScope.launch {
            repository.deleteEmergencyContact(id)
        }
    }

    /**
     * Send Test Alert Action — Fires a local notification previewing what that contact would receive
     * in a real emergency, verifying contact setup without requiring a backend.
     */
    fun sendTestAlert(contact: EmergencyContact) {
        val manager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(getApplication(), TEST_CHANNEL_ID)
            .setContentTitle("TEST ALERT Preview for ${contact.name}")
            .setContentText("Emergency alert preview sent to ${contact.phone} / ${contact.email}. Encrypted incident report link ready.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(contact.id.hashCode(), notification)
    }

    private fun createTestNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TEST_CHANNEL_ID,
                "Emergency Test Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Previews emergency alerts dispatched to contacts"
            }
            val manager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val TEST_CHANNEL_ID = "blackbox_test_alerts"
    }
}
