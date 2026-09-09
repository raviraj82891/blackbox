package com.example.blackbox.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blackbox.data.crypto.MedicalIdData
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

    val qrText = """
        TRACE MEDICAL EMERGENCY ID
        Blood Group: $bloodGroup
        Allergies: $allergies
        Notes: $notes
        Emergency Contact: $contactName ($contactPhone)
    """.trimIndent()

    val qrBitmap = remember(qrText) { generateQrBitmap(qrText) }

    Scaffold(containerColor = OffWhite) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Medical ID", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // First Responder QR Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = SoftCoralContainer, modifier = Modifier.size(28.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = WarmCoral, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "First Responder QR",
                            color = CharcoalText,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Text("Scan to view emergency information", fontSize = 11.sp, color = MutedSlate)

                    Spacer(modifier = Modifier.height(16.dp))

                    qrBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Medical ID QR Code",
                            modifier = Modifier.size(200.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Encrypted & stored only on your device",
                        color = MutedSlate,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Medical Profile Data Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (isEditing) {
                        Text("Edit Medical Profile", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = bloodGroup, onValueChange = { bloodGroup = it }, label = { Text("Blood Group") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = allergies, onValueChange = { allergies = it }, label = { Text("Allergies") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Medical Notes") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = contactName, onValueChange = { contactName = it }, label = { Text("Primary Contact Name") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = contactPhone, onValueChange = { contactPhone = it }, label = { Text("Primary Contact Phone") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                onSaveMedicalId(MedicalIdData(bloodGroup, allergies, notes, contactName, contactPhone))
                                isEditing = false
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Mint)
                        ) {
                            Text("Save Medical ID", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        ProfileInfoTile(
                            icon = Icons.Default.WaterDrop,
                            iconColor = WarmCoral,
                            iconBg = SoftCoralContainer,
                            title = "Blood Group",
                            value = bloodGroup
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileInfoTile(
                            icon = Icons.Default.Eco,
                            iconColor = Mint,
                            iconBg = SoftGreenContainer,
                            title = "Allergies",
                            value = allergies
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileInfoTile(
                            icon = Icons.Default.Description,
                            iconColor = SoftTeal,
                            iconBg = SoftBlueContainer,
                            title = "Medical Notes",
                            value = notes
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { isEditing = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SoftBlueContainer)
                        ) {
                            Text("Edit Profile", color = DarkTeal, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoTile(
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    title: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = iconBg, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MutedSlate)
            Text(value.ifBlank { "Not Specified" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
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
