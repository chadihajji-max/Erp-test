package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CompanyProfile
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import java.io.File

@Composable
fun EditCompanyProfileDialog(
    currentProfile: CompanyProfile,
    onDismiss: () -> Unit,
    onSaveProfile: (CompanyProfile) -> Unit,
    onPickLogoUri: (Uri) -> Unit,
    onResetLogo: () -> Unit
) {
    var companyName by remember { mutableStateOf(currentProfile.companyName) }
    var phone by remember { mutableStateOf(currentProfile.phone) }
    var taxNumber by remember { mutableStateOf(currentProfile.taxNumber) }
    var address by remember { mutableStateOf(currentProfile.address) }
    var footerNote by remember { mutableStateOf(currentProfile.receiptFooterNote) }

    // Zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onPickLogoUri(uri)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Business,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "الملف التعريفي للشركة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "بيانات الفاتورة وشعار الطباعة",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إلغاء")
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Logo Section with Live Preview & Change Button
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "شعار الشركة في الفواتير (Invoice Logo)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Logo Container
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .size(76.dp)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (currentProfile.hasCustomLogo && File(currentProfile.logoUri!!).exists()) {
                                    AsyncImage(
                                        model = File(currentProfile.logoUri),
                                        contentDescription = "شعار مخصص",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_receipt_logo),
                                        contentDescription = "شعار Hajji POS الافتراضي",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons for Logo
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentProfile.hasCustomLogo) "تغيير الشعار" else "رفع شعار مخصص",
                                    fontSize = 11.sp
                                )
                            }

                            if (currentProfile.hasCustomLogo) {
                                OutlinedButton(
                                    onClick = onResetLogo,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("الافتراضي", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Company Name (اسم الشركة)
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("اسم الشركة / المتجر *") },
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Phone Number (رقم الهاتف)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف للتواصل *") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Tax Number / VAT ID (الرقم الضريبي)
                OutlinedTextField(
                    value = taxNumber,
                    onValueChange = { taxNumber = it },
                    label = { Text("الرقم الضريبي (TVA / VAT ID) *") },
                    leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Address (العنوان)
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان والمدينة *") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Footer Note (ملاحظة الفاتورة)
                OutlinedTextField(
                    value = footerNote,
                    onValueChange = { footerNote = it },
                    label = { Text("ملاحظة أسفل الإيصال (سياسة الاستبدال)") },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            val updated = currentProfile.copy(
                                companyName = companyName.trim().ifEmpty { "HAJJI POS - متجر حجّي" },
                                phone = phone.trim().ifEmpty { "01-889900" },
                                taxNumber = taxNumber.trim().ifEmpty { "345678-601" },
                                address = address.trim().ifEmpty { "بيروت - لبنان" },
                                receiptFooterNote = footerNote.trim().ifEmpty { "شكراً لزيارتكم!" }
                            )
                            onSaveProfile(updated)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ التغييرات", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
