package com.example.blackbox.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blackbox.data.db.EmergencyContact
import com.example.blackbox.ui.designsystem.*
import com.example.blackbox.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    contacts: List<EmergencyContact>,
    onAddContact: (EmergencyContact) -> Unit,
    onUpdateContact: (EmergencyContact) -> Unit,
    onRemoveContact: (String) -> Unit,
    onSetPrimaryContact: (EmergencyContact) -> Unit = {},
    onPreviewAlertOnThisPhone: (EmergencyContact) -> Unit,
    onSendRealTestAlert: suspend (EmergencyContact) -> Pair<Boolean, String>
) {
    val coroutineScope = rememberCoroutineScope()

    var showContactSheet by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<EmergencyContact?>(null) }
    var contactToDelete by remember { mutableStateOf<EmergencyContact?>(null) }

    var confirmContactForRealAlert by remember { mutableStateOf<EmergencyContact?>(null) }
    var realAlertDispatchResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var isSendingRealAlert by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = TraceCanvas,
        topBar = {
            TraceTopBar(
                title = "EMERGENCY CONTACTS",
                subtitle = "CONFIGURED DIRECTORY FOR EMERGENCY DISPATCHES"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingContact = null
                    showContactSheet = true
                },
                containerColor = TracePrimary,
                contentColor = Color.Black,
                shape = RectangleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Contact")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (contacts.isEmpty()) {
                TraceEmptyState(
                    title = "NO CONTACTS CONFIGURED",
                    description = "TRACE dispatches encrypted incident reports to your configured emergency contacts during a crash or manual SOS event.",
                    actionText = "ADD FIRST CONTACT",
                    onActionClick = {
                        editingContact = null
                        showContactSheet = true
                    }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(contacts) { contact ->
                        val isReady = contact.isEnabled && contact.phone.isNotBlank()
                        val isMissingDetails = contact.phone.isBlank()

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, TraceHairline, RectangleShape),
                            color = TraceSurface
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = contact.name.uppercase(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TracePrimary,
                                                letterSpacing = 1.sp
                                            )
                                            if (contact.isPrimary) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                TraceBadge(text = "PRIMARY", color = TraceMintSuccess)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (contact.phone.isNotBlank()) "${contact.phone}${if (contact.email.isNotBlank()) " • ${contact.email}" else ""}" else "NO PHONE PROVIDED",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TraceMuted
                                        )
                                        Text(
                                            text = "RELATIONSHIP: ${contact.relationship.ifBlank { "TRUSTED CONTACT" }}".uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TraceMuted
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Switch(
                                            checked = contact.isEnabled,
                                            onCheckedChange = { isChecked ->
                                                onUpdateContact(contact.copy(isEnabled = isChecked))
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.Black,
                                                checkedTrackColor = TraceMintSuccess
                                            )
                                        )
                                        TraceBadge(
                                            text = when {
                                                !contact.isEnabled -> "DISABLED"
                                                isMissingDetails -> "NO PHONE"
                                                else -> "READY"
                                            },
                                            color = when {
                                                !contact.isEnabled -> TraceMuted
                                                isMissingDetails -> TraceAmberWarning
                                                else -> TraceMintSuccess
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                TraceDivider()
                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TraceSecondaryButton(
                                        text = "EDIT",
                                        onClick = {
                                            editingContact = contact
                                            showContactSheet = true
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (!contact.isPrimary) {
                                        TraceSecondaryButton(
                                            text = "SET PRIMARY",
                                            onClick = { onSetPrimaryContact(contact) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    TraceSecondaryButton(
                                        text = "DELETE",
                                        onClick = { contactToDelete = contact },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                TraceSecondaryButton(
                                    text = "PREVIEW LOCAL ALERT ON PHONE",
                                    onClick = { onPreviewAlertOnThisPhone(contact) }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                TracePrimaryButton(
                                    text = "SEND REAL TEST ALERT TO CONTACT",
                                    onClick = { confirmContactForRealAlert = contact }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheet Form for Add / Edit Contact
    if (showContactSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showContactSheet = false
                editingContact = null
            },
            shape = RectangleShape,
            containerColor = TraceSurface
        ) {
            ContactFormSheetContent(
                initialContact = editingContact,
                onSave = { contact ->
                    if (editingContact == null) {
                        onAddContact(contact)
                    } else {
                        onUpdateContact(contact)
                    }
                    showContactSheet = false
                    editingContact = null
                },
                onCancel = {
                    showContactSheet = false
                    editingContact = null
                }
            )
        }
    }

    // Confirmation Dialog before deletion
    contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            shape = RectangleShape,
            containerColor = TraceSurface,
            title = { Text("REMOVE EMERGENCY CONTACT?", fontWeight = FontWeight.Bold, color = TracePrimary, letterSpacing = 1.sp) },
            text = { Text("Are you sure you want to remove ${contact.name}? They will no longer receive emergency alert dispatches during a crash or SOS event.", style = MaterialTheme.typography.bodyMedium, color = TraceMuted) },
            confirmButton = {
                TracePrimaryButton(
                    text = "REMOVE CONTACT",
                    onClick = {
                        onRemoveContact(contact.id)
                        contactToDelete = null
                    },
                    isCritical = true,
                    modifier = Modifier.width(160.dp)
                )
            },
            dismissButton = {
                TraceSecondaryButton(
                    text = "CANCEL",
                    onClick = { contactToDelete = null },
                    modifier = Modifier.width(100.dp)
                )
            }
        )
    }

    // Real Test Alert Confirmation Dialog
    confirmContactForRealAlert?.let { contact ->
        AlertDialog(
            onDismissRequest = { confirmContactForRealAlert = null },
            shape = RectangleShape,
            containerColor = TraceSurface,
            title = { Text("SEND REAL TEST ALERT TO CONTACT?", fontWeight = FontWeight.Bold, color = TracePrimary, letterSpacing = 1.sp) },
            text = {
                Column {
                    Text("This will transmit an actual test notification request to the backend server for this contact:", style = MaterialTheme.typography.bodyMedium, color = TraceMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, TraceHairline, RectangleShape),
                        color = TraceSoftSurface
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("NAME: ${contact.name.uppercase()}", style = MaterialTheme.typography.labelSmall, color = TracePrimary)
                            Text("PHONE: ${contact.phone}", style = MaterialTheme.typography.labelSmall, color = TraceMuted)
                            Text("EMAIL: ${contact.email.ifBlank { "NONE" }}", style = MaterialTheme.typography.labelSmall, color = TraceMuted)
                        }
                    }
                }
            },
            confirmButton = {
                TracePrimaryButton(
                    text = "DISPATCH TEST ALERT",
                    onClick = {
                        val target = contact
                        confirmContactForRealAlert = null
                        isSendingRealAlert = true
                        coroutineScope.launch {
                            val result = onSendRealTestAlert(target)
                            isSendingRealAlert = false
                            realAlertDispatchResult = result
                        }
                    },
                    modifier = Modifier.width(180.dp)
                )
            },
            dismissButton = {
                TraceSecondaryButton(
                    text = "CANCEL",
                    onClick = { confirmContactForRealAlert = null },
                    modifier = Modifier.width(100.dp)
                )
            }
        )
    }

    // Real Alert Dispatch Result Dialog
    realAlertDispatchResult?.let { (success, resultMessage) ->
        AlertDialog(
            onDismissRequest = { realAlertDispatchResult = null },
            shape = RectangleShape,
            containerColor = TraceSurface,
            title = { Text(if (success) "TEST DISPATCH SUCCESSFUL" else "TEST DISPATCH FAILED", fontWeight = FontWeight.Bold, color = if (success) TraceMintSuccess else TraceRedCritical, letterSpacing = 1.sp) },
            text = { Text(resultMessage, style = MaterialTheme.typography.bodyMedium, color = TraceMuted) },
            confirmButton = {
                TracePrimaryButton(
                    text = "OK",
                    onClick = { realAlertDispatchResult = null },
                    modifier = Modifier.width(100.dp)
                )
            }
        )
    }
}

@Composable
private fun ContactFormSheetContent(
    initialContact: EmergencyContact?,
    onSave: (EmergencyContact) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initialContact?.name ?: "") }
    var rawPhone by remember { mutableStateOf(initialContact?.phone ?: "") }
    var email by remember { mutableStateOf(initialContact?.email ?: "") }
    var relationship by remember { mutableStateOf(initialContact?.relationship ?: "Family") }
    var isEnabled by remember { mutableStateOf(initialContact?.isEnabled ?: true) }
    var isPrimary by remember { mutableStateOf(initialContact?.isPrimary ?: false) }

    var selectedCountryCode by remember { mutableStateOf("+1") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    fun validateAndSave() {
        var isValid = true
        if (name.isBlank()) {
            nameError = "Contact name is required"
            isValid = false
        } else {
            nameError = null
        }

        val cleanedDigits = rawPhone.filter { it.isDigit() || it == '+' }
        if (cleanedDigits.isBlank()) {
            phoneError = "Phone number is required"
            isValid = false
        } else {
            phoneError = null
        }

        if (isValid) {
            val normalizedPhone = if (cleanedDigits.startsWith("+")) cleanedDigits else "$selectedCountryCode $cleanedDigits"
            onSave(
                EmergencyContact(
                    id = initialContact?.id ?: UUID.randomUUID().toString(),
                    name = name.trim(),
                    phone = normalizedPhone,
                    email = email.trim(),
                    relationship = relationship.ifBlank { "Trusted Contact" },
                    isEnabled = isEnabled,
                    isPrimary = isPrimary
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TraceSectionHeader(title = if (initialContact == null) "ADD EMERGENCY CONTACT" else "EDIT EMERGENCY CONTACT")

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                if (it.isNotBlank()) nameError = null
            },
            label = { Text("FULL NAME *", style = MaterialTheme.typography.labelSmall) },
            isError = nameError != null,
            shape = RectangleShape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = TraceHairline,
                focusedBorderColor = TracePrimary,
                focusedLabelColor = TracePrimary,
                unfocusedLabelColor = TraceMuted
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (nameError != null) {
            Text(nameError!!.uppercase(), color = TraceRedCritical, style = MaterialTheme.typography.labelSmall)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = selectedCountryCode,
                onValueChange = { selectedCountryCode = it },
                label = { Text("CODE", style = MaterialTheme.typography.labelSmall) },
                shape = RectangleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = TraceHairline,
                    focusedBorderColor = TracePrimary,
                    focusedLabelColor = TracePrimary,
                    unfocusedLabelColor = TraceMuted
                ),
                modifier = Modifier.width(80.dp)
            )

            OutlinedTextField(
                value = rawPhone,
                onValueChange = {
                    rawPhone = it
                    if (it.isNotBlank()) phoneError = null
                },
                label = { Text("PHONE NUMBER *", style = MaterialTheme.typography.labelSmall) },
                isError = phoneError != null,
                shape = RectangleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = TraceHairline,
                    focusedBorderColor = TracePrimary,
                    focusedLabelColor = TracePrimary,
                    unfocusedLabelColor = TraceMuted
                ),
                modifier = Modifier.weight(1f)
            )
        }
        if (phoneError != null) {
            Text(phoneError!!.uppercase(), color = TraceRedCritical, style = MaterialTheme.typography.labelSmall)
        }

        OutlinedTextField(
            value = relationship,
            onValueChange = { relationship = it },
            label = { Text("RELATIONSHIP (OPTIONAL)", style = MaterialTheme.typography.labelSmall) },
            placeholder = { Text("e.g. Spouse, Parent, Friend") },
            shape = RectangleShape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = TraceHairline,
                focusedBorderColor = TracePrimary,
                focusedLabelColor = TracePrimary,
                unfocusedLabelColor = TraceMuted
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("EMAIL ADDRESS (OPTIONAL)", style = MaterialTheme.typography.labelSmall) },
            shape = RectangleShape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = TraceHairline,
                focusedBorderColor = TracePrimary,
                focusedLabelColor = TracePrimary,
                unfocusedLabelColor = TraceMuted
            ),
            modifier = Modifier.fillMaxWidth()
        )

        TraceDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ENABLE EMERGENCY DISPATCHES", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TracePrimary)
            Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SET AS PRIMARY CONTACT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TracePrimary)
            Switch(checked = isPrimary, onCheckedChange = { isPrimary = it })
        }

        Spacer(modifier = Modifier.height(10.dp))

        TracePrimaryButton(
            text = "SAVE CONTACT",
            onClick = { validateAndSave() }
        )

        TraceSecondaryButton(
            text = "CANCEL",
            onClick = onCancel
        )
    }
}
