package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.ExpenseEntity
import com.example.ui.PosViewModel
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RedAlert
import java.text.SimpleDateFormat
import java.util.*

val EXPENSE_CATEGORIES = listOf(
    "اشتراك مولد وكهرباء" to Icons.Default.Bolt,
    "إيجار المحل" to Icons.Default.HomeWork,
    "رواتب وأجور موظفين" to Icons.Default.Badge,
    "ماء وإنترنت وهاتف" to Icons.Default.Wifi,
    "بضاعة ومشتريات" to Icons.Default.ShoppingCart,
    "نقل وشحن وتوصيل" to Icons.Default.LocalShipping,
    "صيانة وتجهيزات" to Icons.Default.Build,
    "ضيافة ونثريات يومية" to Icons.Default.Coffee,
    "ضرائب ورسوم حكومية" to Icons.Default.ReceiptLong,
    "مصاريف متنوعة" to Icons.Default.Category
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditExpenseDialog(
    expenseToEdit: ExpenseEntity? = null,
    currentExchangeRate: Double = 89500.0,
    cashierName: String = "المدير",
    branchName: String = "الفرع الرئيسي",
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, amountUsd: Double, amountLbp: Long, date: Long, notes: String) -> Unit,
    onDelete: ((ExpenseEntity) -> Unit)? = null
) {
    val isEditMode = expenseToEdit != null

    var title by remember { mutableStateOf(expenseToEdit?.title ?: "") }
    var selectedCategory by remember { mutableStateOf(expenseToEdit?.category ?: EXPENSE_CATEGORIES[0].first) }
    var amountUsdInput by remember { mutableStateOf(expenseToEdit?.amountUsd?.toString() ?: "") }
    var notes by remember { mutableStateOf(expenseToEdit?.notes ?: "") }
    var selectedDateMillis by remember { mutableStateOf(expenseToEdit?.date ?: System.currentTimeMillis()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Calculated LBP amount based on entered USD
    val calculatedLbp: Long = remember(amountUsdInput, currentExchangeRate) {
        val usd = amountUsdInput.toDoubleOrNull() ?: 0.0
        (usd * currentExchangeRate).toLong()
    }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

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
                            color = if (isEditMode) Color(0xFFFEF3C7) else Color(0xFFFEE2E2),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isEditMode) Icons.Default.Edit else Icons.Default.AddCard,
                                    contentDescription = null,
                                    tint = if (isEditMode) Color(0xFFB45309) else RedAlert,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isEditMode) "تعديل المصروف اليومي" else "إضافة مصروف يومي جديد",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تسجيل وتصنيف المصاريف التشغيلية للمتجر",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("بيان / عنوان المصروف *") },
                    placeholder = { Text("مثال: فاتورة مولد شهر 3، أجرة عامل إضافي") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selection Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "تصنيف المصروف:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EXPENSE_CATEGORIES.forEach { (catName, catIcon) ->
                            val isSelected = selectedCategory == catName
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = catName },
                                leadingIcon = {
                                    Icon(
                                        catIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                },
                                label = {
                                    Text(
                                        text = catName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Amount Section (Dual Currency: USD & LBP)
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = amountUsdInput,
                            onValueChange = { amountUsdInput = it },
                            label = { Text("المبلغ بالدولار ($) *") },
                            prefix = { Text("$ ", fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Live LBP conversion banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEFF6FF), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المعادل بالليرة (سعر 1$ = ${PosViewModel.formatLbp(currentExchangeRate.toLong())}):",
                                fontSize = 11.sp,
                                color = Color(0xFF1E40AF)
                            )
                            Text(
                                text = PosViewModel.formatLbp(calculatedLbp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF1E3A8A)
                            )
                        }
                    }
                }

                // Date Selection Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "تاريخ المصروف:", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = dateFormatter.format(Date(selectedDateMillis)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = { selectedDateMillis = System.currentTimeMillis() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("اليوم", fontSize = 11.sp)
                        }

                        FilledTonalButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.DAY_OF_YEAR, -1)
                                selectedDateMillis = cal.timeInMillis
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("الأمس", fontSize = 11.sp)
                        }
                    }
                }

                // Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية / رقم الإيصال أو الفاتورة") },
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
                    if (isEditMode && onDelete != null) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAlert),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حذف", fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("إلغاء")
                        }
                    }

                    Button(
                        onClick = {
                            val amt = amountUsdInput.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && amt > 0) {
                                onSave(title.trim(), selectedCategory, amt, calculatedLbp, selectedDateMillis, notes.trim())
                                onDismiss()
                            }
                        },
                        enabled = title.isNotBlank() && (amountUsdInput.toDoubleOrNull() ?: 0.0) > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEditMode) Color(0xFFD97706) else EmeraldPrimary
                        ),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Save else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEditMode) "تحديث المصروف" else "تسجيل المصروف",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm && expenseToEdit != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = RedAlert) },
            title = { Text("تأكيد حذف المصروف") },
            text = {
                Text("هل أنت متأكد من رغبتك في حذف المصروف \"${expenseToEdit.title}\" بقيمة ${PosViewModel.formatUsd(expenseToEdit.amountUsd)} نهائياً؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(expenseToEdit)
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
                    Text("تراجع")
                }
            }
        )
    }
}
