package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.PosUiState
import com.example.ui.PosViewModel
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import java.io.File

@Composable
fun SettingsScreen(
    viewModel: PosViewModel,
    uiState: PosUiState
) {
    var rateInput by remember { mutableStateOf(uiState.exchangeRate.toLong().toString()) }
    var branchInput by remember { mutableStateOf(uiState.currentBranch) }
    var cashierInput by remember { mutableStateOf(uiState.currentCashier) }
    var showSavedSnackbar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Text(
            text = "إعدادات النظام وسعر الصرف",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "تحديث سعر صرف الليرة، إدارة الأجهزة ومزامنة Hajji E-commerce",
            fontSize = 12.sp,
            color = Color.Gray
        )

        // Lebanese Exchange Rate Settings Card (CRITICAL)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = GoldAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "سعر صرف الدولار اليومي (سوق لبنان)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "سعر السوق الموازي",
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "كل فاتورة جديدة تُسجل بالسعر الحالي وتحفظه معها لضمان ثبات الحسابات التاريخية.",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { rateInput = it },
                        label = { Text("سعر الصرف (1$ = ? L.L)") },
                        suffix = { Text(" ل.ل") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val newRate = rateInput.toDoubleOrNull() ?: 89500.0
                            viewModel.updateExchangeRate(newRate)
                            showSavedSnackbar = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تحديث السعر", fontSize = 12.sp)
                    }
                }
            }
        }

        // Hardware & POS Devices Integration
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ربط ملحقات نقطة البيع (Hardware)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                HardwareDeviceRow(
                    title = "طابعة الإيصالات الحرارية (Thermal Printer)",
                    subtitle = "بلوتوث / USB - 80mm ESC/POS",
                    isConnected = true,
                    onTest = { viewModel.testThermalPrint() }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))

                HardwareDeviceRow(
                    title = "قارئ الباركود (Barcode Scanner)",
                    subtitle = "سلكي USB أو لاسلكي 2.4GHz",
                    isConnected = true,
                    onTest = { viewModel.showUserFeedback("قارئ الباركود متصل وجاهز للمسح") }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))

                HardwareDeviceRow(
                    title = "درج النقود الإلكتروني (Cash Drawer)",
                    subtitle = "متصل عبر منفذ الطابعة RJ11/RJ12",
                    isConnected = true,
                    onTest = { viewModel.triggerCashDrawer() }
                )
            }
        }

        // Hajji E-commerce Synchronization Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "مزامنة متجر Hajji E-commerce",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "ربط المخزون والمبيعات بين المحل الفعلي والمتجر الإلكتروني",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Surface(
                        color = Color(0xFFE6F8F0),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "متصل وسحابي",
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("الطلبات الجديدة غير المستلمة: 3 طلبات", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("آخر مزامنة: منذ دقائق قليلة", fontSize = 10.sp, color = Color.DarkGray)
                    }

                    Button(
                        onClick = { viewModel.syncWithEcommerce() },
                        enabled = !uiState.isSyncingEcommerce,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (uiState.isSyncingEcommerce) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("جارِ المزامنة...", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مزامنة الآن", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Store & Branch Info
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "بيانات المتجر والفرع الحالي", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                OutlinedTextField(
                    value = branchInput,
                    onValueChange = {
                        branchInput = it
                        viewModel.updateBranchAndCashier(it, cashierInput)
                    },
                    label = { Text("الفرع الحالي") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = cashierInput,
                    onValueChange = {
                        cashierInput = it
                        viewModel.updateBranchAndCashier(branchInput, it)
                    },
                    label = { Text("اسم الكاشير / المستخدم") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Company Profile & Customer Receipt Branding Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "الملف التعريفي للشركة والفواتير",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "الاسم التجاري، الهاتف، الرقم الضريبي والشعار المخصص",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFE0E7FF),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "جاهز ومعتمد",
                            color = Color(0xFF3730A3),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Profile Summary Box with Logo & Details
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo Preview
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (uiState.companyProfile.hasCustomLogo && File(uiState.companyProfile.logoUri!!).exists()) {
                                AsyncImage(
                                    model = File(uiState.companyProfile.logoUri),
                                    contentDescription = "شعار الشركة",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.img_receipt_logo),
                                    contentDescription = "Hajji POS Receipt Logo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = uiState.companyProfile.companyName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "📞 هاتف: ${uiState.companyProfile.phone}",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "🏷️ الرقم الضريبي: ${uiState.companyProfile.taxNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = "📍 ${uiState.companyProfile.address}",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Action Buttons for Company Profile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.openEditCompanyProfileDialog() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تعديل البيانات والشعار", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.previewSampleReceipt() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("معاينة الفاتورة", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HardwareDeviceRow(
    title: String,
    subtitle: String,
    isConnected: Boolean,
    onTest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(text = subtitle, fontSize = 11.sp, color = Color.Gray)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isConnected) Color(0xFFE6F8F0) else Color(0xFFFEE2E2),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = if (isConnected) "جاهز" else "غير متصل",
                    color = if (isConnected) EmeraldPrimary else Color.Red,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            OutlinedButton(
                onClick = onTest,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("اختبار", fontSize = 11.sp)
            }
        }
    }
}
