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
import com.example.blackbox.data.api.AWSBackendApi
import com.example.blackbox.data.api.NotificationRequest
import com.example.blackbox.data.db.EmergencyContact
import com.example.blackbox.data.repository.BlackboxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    application: Application,
    private val repository: BlackboxRepository,
    private val apiService: AWSBackendApi
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

    fun updateContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.insertEmergencyContact(contact)
        }
    }

    fun removeContact(id: String) {
        viewModelScope.launch {
            repository.deleteEmergencyContact(id)
        }
    }

    fun setPrimaryContact(target: EmergencyContact) {
        viewModelScope.launch {
            val current = contacts.value
            for (c in current) {
                val updated = c.copy(isPrimary = c.id == target.id)
                repository.insertEmergencyContact(updated)
            }
        }
    }

    /**
     * Local Notification Preview — Generates a local preview notification on the user's phone ONLY.
     * Never claims that a real contact was notified.
     */
    fun previewAlertOnThisPhone(contact: EmergencyContact) {
        val manager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(getApplication(), TEST_CHANNEL_ID)
            .setContentTitle("TRACE Local Alert Preview (For ${contact.name})")
            .setContentText("Local preview generated on this phone. Note: This does NOT send an SMS or alert to ${contact.name}.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(contact.id.hashCode(), notification)
    }

    /**
     * Real Test Alert to Contact — Dispatches an actual test notification request to the backend server.
     * Returns true/false based on the actual backend response.
     */
    suspend fun sendRealTestAlertToContact(contact: EmergencyContact): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val req = NotificationRequest(
                incidentId = "TEST-${contact.id.take(6)}",
                message = "TRACE Safety System Test Alert: Sent by ${contact.name}'s contact settings.",
                contacts = listOf(contact.id)
            )
            val response = apiService.notifyContacts("TEST-${contact.id.take(6)}", req)
            if (response.isSuccessful) {
                Pair(true, "Real test alert successfully dispatched to ${contact.name} (${contact.phone}).")
            } else {
                Pair(false, "Backend dispatch failed: HTTP ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Pair(false, "Network error: Unable to reach backend server. ${e.localizedMessage ?: ""}")
        }
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
