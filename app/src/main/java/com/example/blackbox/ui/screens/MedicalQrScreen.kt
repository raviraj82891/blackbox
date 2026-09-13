package com.example.blackbox.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blackbox.data.crypto.MedicalIdData
import com.example.blackbox.ui.designsystem.*
import com.example.blackbox.ui.theme.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun MedicalQrScreen(
    medicalId: MedicalIdData,
    onSaveMedicalId: (MedicalIdData) -> Unit
) {
    var bloodGroup by remember(medicalId) { mutableStateOf(medicalId.bloodGroup) }
    var allergies by remember(medicalId) { mutableStateOf(medicalId.allergies) }
    var notes by remember(medicalId) { mutableStateOf(medicalId.medicalNotes) }
    var contactName by remember(medicalId) { mutableStateOf(medicalId.emergencyContactName) }
    var contactPhone by remember(medicalId) { mutableStateOf(medicalId.emergencyContactPhone) }

    var isEditing by remember { mutableStateOf(false) }

    var isQrVisible by remember { mutableStateOf(false) }
    var isMinimalQrMode by remember { mutableStateOf(true) }
    var showQrConfirmationDialog by remember { mutableStateOf(false) }

    val qrText = remember(bloodGroup, allergies, notes, contactName, contactPhone, isMinimalQrMode) {
        if (isMinimalQrMode) {
            """
                TRACE EMERGENCY MEDICAL ID (MINIMAL)
                Blood Group: ${bloodGroup.ifBlank { "Not Specified" }}
                Critical Allergies: ${allergies.ifBlank { "None Known" }}
                Emergency Phone: ${contactPhone.ifBlank { "None Provided" }}
            """.trimIndent()
        } else {
            """
                TRACE EMERGENCY MEDICAL ID (FULL)
                Blood Group: ${bloodGroup.ifBlank { "Not Specified" }}
                Allergies: ${allergies.ifBlank { "None Known" }}
                Medical Notes: ${notes.ifBlank { "None" }}
                Emergency Contact: ${contactName.ifBlank { "Contact" }} (${contactPhone.ifBlank { "None Provided" }})
            """.trimIndent()
        }
    }

    val qrBitmap = remember(qrText, isQrVisible) {
        if (isQrVisible) generateQrBitmap(qrText) else null
    }

    Scaffold(
        containerColor = TraceCanvas,
        topBar = {
            TraceTopBar(
                title = "MEDICAL ID",
                subtitle = "ENCRYPTED PROFILE & PUBLIC EMERGENCY QR"
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. ENCRYPTED MEDICAL ID PROFILE
            TraceSectionHeader(
                title = "MEDICAL PROFILE (ENCRYPTED)",
                actionText = if (isEditing) "CANCEL" else "EDIT PROFILE",
                onActionClick = { isEditing = !isEditing }
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TraceHairline, RectangleShape),
                color = TraceSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isEditing) {
                        OutlinedTextField(
                            value = bloodGroup,
                            onValueChange = { bloodGroup = it },
                            label = { Text("BLOOD GROUP", style = MaterialTheme.typography.labelSmall) },
                            shape = RectangleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = TraceHairline,
                                focusedBorderColor = TracePrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = allergies,
                            onValueChange = { allergies = it },
                            label = { Text("CRITICAL ALLERGIES", style = MaterialTheme.typography.labelSmall) },
                            shape = RectangleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = TraceHairline,
                                focusedBorderColor = TracePrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("MEDICAL NOTES", style = MaterialTheme.typography.labelSmall) },
                            shape = RectangleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = TraceHairline,
                                focusedBorderColor = TracePrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            label = { Text("EMERGENCY CONTACT NAME", style = MaterialTheme.typography.labelSmall) },
                            shape = RectangleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = TraceHairline,
                                focusedBorderColor = TracePrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("EMERGENCY CONTACT PHONE", style = MaterialTheme.typography.labelSmall) },
                            shape = RectangleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = TraceHairline,
                                focusedBorderColor = TracePrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TracePrimaryButton(
                            text = "SAVE MEDICAL ID SECURELY",
                            onClick = {
                                onSaveMedicalId(MedicalIdData(bloodGroup, allergies, notes, contactName, contactPhone))
                                isEditing = false
                            }
                        )
                    } else {
                        TraceSpecRow(label = "BLOOD GROUP", value = bloodGroup.ifBlank { "NOT SPECIFIED" })
                        TraceSpecRow(label = "CRITICAL ALLERGIES", value = allergies.ifBlank { "NONE KNOWN" })
                        TraceSpecRow(label = "MEDICAL NOTES", value = notes.ifBlank { "NONE REPORTED" })
                        TraceSpecRow(label = "EMERGENCY CONTACT", value = if (contactName.isNotBlank() || contactPhone.isNotBlank()) "$contactName ($contactPhone)" else "NOT SPECIFIED")
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 2. PUBLIC EMERGENCY QR SECTION
            TraceSectionHeader(title = "PUBLIC EMERGENCY QR")

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TraceHairline, RectangleShape),
                color = TraceSurface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DISPLAY PUBLIC EMERGENCY QR",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TracePrimary,
                            letterSpacing = 1.sp
                        )

                        Switch(
                            checked = isQrVisible,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showQrConfirmationDialog = true
                                } else {
                                    isQrVisible = false
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "WARNING: Anyone scanning this QR code with a smartphone camera can read the encoded details below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TraceAmberWarning
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // QR Scope Mode Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isMinimalQrMode) {
                            TracePrimaryButton(
                                text = "MINIMAL QR",
                                onClick = { isMinimalQrMode = true },
                                modifier = Modifier.weight(1f)
                            )
                            TraceSecondaryButton(
                                text = "FULL MEDICAL ID",
                                onClick = { isMinimalQrMode = false },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            TraceSecondaryButton(
                                text = "MINIMAL QR",
                                onClick = { isMinimalQrMode = true },
                                modifier = Modifier.weight(1f)
                            )
                            TracePrimaryButton(
                                text = "FULL MEDICAL ID",
                                onClick = { isMinimalQrMode = false },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TraceDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isQrVisible && qrBitmap != null) {
                        Surface(
                            modifier = Modifier
                                .size(240.dp)
                                .background(Color.White)
                                .padding(12.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Public Emergency Medical QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "PRIVACY NOTICE: Do not share screenshots of this QR on public platforms.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TraceAmberWarning,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .border(1.dp, TraceHairline, RectangleShape),
                            color = TraceSoftSurface
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "PUBLIC QR CODE HIDDEN",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TraceMuted,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Toggle switch above to generate QR code", style = MaterialTheme.typography.bodySmall, color = TraceMuted)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }

    // Confirmation Dialog before generating unencrypted QR
    if (showQrConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showQrConfirmationDialog = false },
            shape = RectangleShape,
            containerColor = TraceSurface,
            title = { Text("GENERATE PUBLIC EMERGENCY QR?", fontWeight = FontWeight.Bold, color = TracePrimary, letterSpacing = 1.sp) },
            text = {
                Column {
                    Text("This will render a scannable QR code on your screen. Please note:", style = MaterialTheme.typography.bodyMedium, color = TraceMuted)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Anyone with a camera can read the encoded information.", style = MaterialTheme.typography.bodySmall, color = TraceMuted)
                    Text("• Device storage remains AES-256 encrypted, but QR codes are plaintext.", style = MaterialTheme.typography.bodySmall, color = TraceMuted)
                    Text("• ${if (isMinimalQrMode) "Minimal QR mode restricts output to blood group, allergies, and contact phone." else "Full QR mode includes all medical notes."}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TracePrimary)
                }
            },
            confirmButton = {
                TracePrimaryButton(
                    text = "GENERATE PUBLIC QR",
                    onClick = {
                        showQrConfirmationDialog = false
                        isQrVisible = true
                    },
                    modifier = Modifier.width(180.dp)
                )
            },
            dismissButton = {
                TraceSecondaryButton(
                    text = "CANCEL",
                    onClick = { showQrConfirmationDialog = false },
                    modifier = Modifier.width(100.dp)
                )
            }
        )
    }
}

private fun generateQrBitmap(content: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bmp
    } catch (e: Exception) {
        null
    }
}
