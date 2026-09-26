package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ImportShipmentEntity
import com.example.data.model.SupplierEntity
import com.example.ui.PosUiState
import com.example.ui.PosViewModel
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PurchasesScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    suppliers: List<SupplierEntity>,
    shipments: List<ImportShipmentEntity>
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Shipments & Landed Cost, 1: Suppliers Directory
    var showAddShipmentDialog by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "المشتريات والاستيراد (الصين ومرفأ بيروت)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "حساب تكلفة الشحن، الجمارك وتوزيعها على القطع",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Button(
                onClick = {
                    if (selectedTab == 0) showAddShipmentDialog = true else showAddSupplierDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (selectedTab == 0) "شحنة استيراد" else "مورد جديد", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("شحنات الاستيراد وتكلفة المرفأ (${shipments.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("سجل الموردين (Alibaba/1688) (${suppliers.size})", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            // Shipments List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(shipments) { shipment ->
                    val landedCostPerUnit = if (shipment.itemCount > 0) shipment.totalLandedCostUsd / shipment.itemCount else 0.0
                    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(shipment.date))

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "شحنة رقم: ${shipment.trackingNumber}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "المورد: ${shipment.supplierName} | التاريخ: $dateStr",
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                }

                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "مرفأ بيروت (واصل)",
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = shipment.description, fontSize = 12.sp)

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))

                            // Import Landed Cost Breakdown Grid
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("قيمة البضاعة:", fontSize = 10.sp, color = Color.Gray)
                                    Text(PosViewModel.formatUsd(shipment.goodsCostUsd), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Column {
                                    Text("شحن (${shipment.weightKg} كغ):", fontSize = 10.sp, color = Color.Gray)
                                    Text(PosViewModel.formatUsd(shipment.shippingCostUsd), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Column {
                                    Text("جمرك مرفأ بيروت:", fontSize = 10.sp, color = Color.Gray)
                                    Text(PosViewModel.formatUsd(shipment.customsBeirutPortUsd), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GoldAccent)
                                }
                                Column {
                                    Text("رسوم التخليص:", fontSize = 10.sp, color = Color.Gray)
                                    Text(PosViewModel.formatUsd(shipment.portClearanceUsd), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Total Landed Cost & Cost per Piece
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "التكلفة الإجمالية الواصلة للمتجر:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = PosViewModel.formatUsd(shipment.totalLandedCostUsd),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "تكلفة القطعة الواحدة الواصلة:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${PosViewModel.formatUsd(landedCostPerUnit)} / قطعة",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary
                                        )
                                        Text(
                                            text = "عدد القطع: ${shipment.itemCount}",
                                            fontSize = 10.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Suppliers Directory (Alibaba / 1688 / China / Local)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suppliers) { supplier ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = supplier.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "${supplier.company} | ${supplier.country}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Surface(
                                    color = Color(0xFFDBEAFE),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = supplier.country,
                                        color = Color(0xFF1E40AF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            if (supplier.phone.isNotBlank()) {
                                Text(text = "هاتف / واتساب: ${supplier.phone}", fontSize = 11.sp)
                            }
                            if (supplier.email.isNotBlank()) {
                                Text(text = "إيميل: ${supplier.email}", fontSize = 11.sp)
                            }
                            if (supplier.notes.isNotBlank()) {
                                Text(text = "ملاحظات وتخصص: ${supplier.notes}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Shipment Dialog with Landed Cost Distribution
    if (showAddShipmentDialog) {
        var tracking by remember { mutableStateOf("CN-BEY-${System.currentTimeMillis() % 10000}") }
        var supplierName by remember { mutableStateOf(suppliers.firstOrNull()?.name ?: "Alibaba Supplier") }
        var desc by remember { mutableStateOf("") }
        var weight by remember { mutableStateOf("30.0") }
        var goodsCost by remember { mutableStateOf("1000.0") }
        var shippingCost by remember { mutableStateOf("200.0") }
        var customsBeirut by remember { mutableStateOf("120.0") }
        var clearance by remember { mutableStateOf("60.0") }
        var count by remember { mutableStateOf("100") }

        AlertDialog(
            onDismissRequest = { showAddShipmentDialog = false },
            title = { Text("تسجيل شحنة استيراد جديدة") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tracking,
                        onValueChange = { tracking = it },
                        label = { Text("رقم التتبع / الشحنة") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = supplierName,
                        onValueChange = { supplierName = it },
                        label = { Text("المورد (الصين / محلي)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("وصف البضاعة المحتواة") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = goodsCost,
                            onValueChange = { goodsCost = it },
                            label = { Text("قيمة البضاعة ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = count,
                            onValueChange = { count = it },
                            label = { Text("إجمالي عدد القطع") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("الوزن الإجمالي (كغ)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = shippingCost,
                            onValueChange = { shippingCost = it },
                            label = { Text("تكلفة الشحن ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customsBeirut,
                            onValueChange = { customsBeirut = it },
                            label = { Text("جمارك مرفأ بيروت ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = clearance,
                            onValueChange = { clearance = it },
                            label = { Text("رسوم التخليص ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Real-time calculated landed cost preview
                    val gVal = goodsCost.toDoubleOrNull() ?: 0.0
                    val sVal = shippingCost.toDoubleOrNull() ?: 0.0
                    val cVal = customsBeirut.toDoubleOrNull() ?: 0.0
                    val pVal = clearance.toDoubleOrNull() ?: 0.0
                    val totalLanded = gVal + sVal + cVal + pVal
                    val cItems = count.toIntOrNull() ?: 1
                    val unitLanded = if (cItems > 0) totalLanded / cItems else 0.0

                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "التكلفة الإجمالية الواصلة: ${PosViewModel.formatUsd(totalLanded)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "تكلفة القطعة الواحدة بعد إضافة الشحن والجمارك: ${PosViewModel.formatUsd(unitLanded)}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = GoldAccent
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addImportShipment(
                            trackingNumber = tracking,
                            supplierName = supplierName,
                            description = desc,
                            weightKg = weight.toDoubleOrNull() ?: 0.0,
                            shippingCostUsd = shippingCost.toDoubleOrNull() ?: 0.0,
                            customsBeirutPortUsd = customsBeirut.toDoubleOrNull() ?: 0.0,
                            portClearanceUsd = clearance.toDoubleOrNull() ?: 0.0,
                            goodsCostUsd = goodsCost.toDoubleOrNull() ?: 0.0,
                            itemCount = count.toIntOrNull() ?: 1,
                            notes = "استيراد مسجل عبر نظام المشتريات"
                        )
                        showAddShipmentDialog = false
                    }
                ) {
                    Text("حفظ الشحنة وتثبيت التكاليف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddShipmentDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Add Supplier Dialog
    if (showAddSupplierDialog) {
        var supName by remember { mutableStateOf("") }
        var comp by remember { mutableStateOf("Alibaba / 1688") }
        var country by remember { mutableStateOf("الصين") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSupplierDialog = false },
            title = { Text("إضافة مورد جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = supName, onValueChange = { supName = it }, label = { Text("اسم المورد / الشركة") })
                    OutlinedTextField(value = comp, onValueChange = { comp = it }, label = { Text("المنصة / الشركة (Alibaba/1688/محلي)") })
                    OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("البلد والمدينة") })
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف / واتساب") })
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("البريد الإلكتروني") })
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات البضاعة والأسعار") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addSupplier(supName, comp, country, phone, email, notes)
                        showAddSupplierDialog = false
                    },
                    enabled = supName.isNotBlank()
                ) {
                    Text("إضافة المورد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSupplierDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
