package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.SaleEntity
import com.example.ui.PosViewModel
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RedAlert
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditInvoiceDialog(
    sale: SaleEntity,
    onDismiss: () -> Unit,
    onSaveSale: (SaleEntity) -> Unit,
    onDeleteSale: (SaleEntity, restoreStock: Boolean) -> Unit
) {
    var customerName by remember { mutableStateOf(sale.customerName ?: "") }
    var cashierName by remember { mutableStateOf(sale.cashierName) }
    var selectedPaymentMethod by remember { mutableStateOf(sale.paymentMethod) }
    var notes by remember { mutableStateOf(sale.notes) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var restoreStockOnDelete by remember { mutableStateOf(true) }

    val dateStr = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(sale.timestamp))
    }

    val paymentOptions = listOf(
        "CASH_USD" to "نقدي (دولار)",
        "CASH_LBP" to "نقدي (ليرة)",
        "SPLIT_USD_LBP" to "دفع مدمج ($ + L.L)",
        "CARD" to "بطاقة بنكية",
        "CREDIT" to "آجل / ذمة"
    )

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
                                    Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تعديل الفاتورة #${sale.invoiceNumber}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تاريخ الإصدار: $dateStr",
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

                // Invoice Summary Box
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("قيمة الفاتورة الإجمالية:", fontSize = 11.sp, color = Color.DarkGray)
                            Text(
                                text = "${PosViewModel.formatUsd(sale.totalUsd)} ≈ ${PosViewModel.formatLbp(sale.totalLbp)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            color = if (sale.status == "COMPLETED") Color(0xFFE6F8F0) else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (sale.status == "COMPLETED") "مكتملة" else sale.status,
                                color = if (sale.status == "COMPLETED") EmeraldPrimary else Color(0xFFB45309),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Customer Name
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("اسم العميل / الزبون") },
                    placeholder = { Text("زبون نقدي عام") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Cashier Name
                OutlinedTextField(
                    value = cashierName,
                    onValueChange = { cashierName = it },
                    label = { Text("اسم الكاشير المسؤول") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Payment Method Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "طريقة الدفع المسجلة:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        paymentOptions.forEach { (methodCode, methodLabel) ->
                            val isSelected = selectedPaymentMethod == methodCode
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPaymentMethod = methodCode },
                                label = { Text(methodLabel, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات الفاتورة") },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAlert),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حذف الفاتورة", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val updated = sale.copy(
                                customerName = customerName.trim().ifEmpty { null },
                                cashierName = cashierName.trim().ifEmpty { sale.cashierName },
                                paymentMethod = selectedPaymentMethod,
                                notes = notes.trim()
                            )
                            onSaveSale(updated)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ التعديلات", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = RedAlert) },
            title = { Text("تأكيد حذف الفاتورة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "هل أنت متأكد من رغبتك في حذف الفاتورة رقم ${sale.invoiceNumber} بقيمة ${PosViewModel.formatUsd(sale.totalUsd)} نهائياً؟",
                        fontSize = 13.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Checkbox(
                            checked = restoreStockOnDelete,
                            onCheckedChange = { restoreStockOnDelete = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إعادة كميات المنتجات المباعة إلى المخزون تلقائياً",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSale(sale, restoreStockOnDelete)
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
                ) {
                    Text("نعم، حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
