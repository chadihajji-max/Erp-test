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
import com.example.data.model.ExpenseEntity
import com.example.data.model.ShiftEntity
import com.example.ui.PosUiState
import com.example.ui.PosViewModel
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RedAlert
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ShiftsExpensesScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    activeShift: ShiftEntity?,
    expenses: List<ExpenseEntity>
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0: Cashier Shift & Drawer, 1: Expenses & Petty Cash
    var showOpenShiftDialog by remember { mutableStateOf(false) }
    var showCloseShiftDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    val totalExpensesUsd = expenses.sumOf { it.amountUsd }
    val totalExpensesLbp = expenses.sumOf { it.amountLbp }

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
                    text = "الورديات، الصندوق والمصاريف",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "إدارة وردية الكاشير، درج النقود والمصاريف التشغيلية",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (selectedSubTab == 1) {
                Button(
                    onClick = { showAddExpenseDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تسجيل مصروف", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Selector
        TabRow(selectedTabIndex = selectedSubTab) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("وردية الكاشير ودرج النقود", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("المصاريف التشغيلية (${expenses.size})", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedSubTab == 0) {
            // Cashier Shift & Drawer View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeShift != null) {
                    val startTimeStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(activeShift.startTime))

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
                                        text = "الوردية الحالية مفتوحة",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = EmeraldPrimary
                                    )
                                    Text(
                                        text = "الكاشير: ${activeShift.cashierName} | الفرع: ${activeShift.branchName}",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        text = "تاريخ البدء: $startTimeStr",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                Surface(
                                    color = Color(0xFFE6F8F0),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "نشطة (OPEN)",
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE2E8F0))

                            // Shift Financial Summary
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("عهدة الافتتاح (دولار):", fontSize = 11.sp, color = Color.Gray)
                                    Text(PosViewModel.formatUsd(activeShift.openingCashUsd), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Column {
                                    Text("عهدة الافتتاح (ليرة):", fontSize = 11.sp, color = Color.Gray)
                                    Text(PosViewModel.formatLbp(activeShift.openingCashLbp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Column {
                                    Text("مبيعات الوردية:", fontSize = 11.sp, color = Color.Gray)
                                    Text(PosViewModel.formatUsd(activeShift.totalSalesUsd), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action Buttons: Open drawer / Close shift
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.triggerCashDrawer() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("فتح درج النقود", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { showCloseShiftDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إغلاق الوردية", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    // No active shift
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لا توجد وردية مفتوحة حالياً",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "ابدأ وردية جديدة بتحديد العهدة الافتتاحية في درج النقود (دولار وليرة)",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showOpenShiftDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("فتح وردية كاشير جديدة")
                            }
                        }
                    }
                }
            }
        } else {
            // Expenses List & Summary
            Column(modifier = Modifier.fillMaxSize()) {
                // Summary Card
                Surface(
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("إجمالي المصاريف التشغيلية المسجلة:", fontSize = 12.sp, color = Color.DarkGray)
                            Text(
                                text = "${PosViewModel.formatUsd(totalExpensesUsd)} ≈ ${PosViewModel.formatLbp(totalExpensesLbp)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = RedAlert
                            )
                        }
                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = RedAlert, modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(expenses) { expense ->
                        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(expense.date))
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = expense.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "التصنيف: ${expense.category} | الفرع: ${expense.branchName} | $dateStr",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    if (expense.notes.isNotBlank()) {
                                        Text(text = "ملاحظات: ${expense.notes}", fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = PosViewModel.formatUsd(expense.amountUsd),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = RedAlert
                                    )
                                    Text(
                                        text = PosViewModel.formatLbp(expense.amountLbp),
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Open Shift Dialog
    if (showOpenShiftDialog) {
        var openUsd by remember { mutableStateOf("100.0") }
        var openLbp by remember { mutableStateOf("5000000") }
        var notes by remember { mutableStateOf("افتتاح وردية عادية") }

        AlertDialog(
            onDismissRequest = { showOpenShiftDialog = false },
            title = { Text("فتح وردية كاشير جديدة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل الرصيد الافتتاحي الفعلي في درج النقود:")
                    OutlinedTextField(
                        value = openUsd,
                        onValueChange = { openUsd = it },
                        label = { Text("الرصيد الافتتاحي بالدولار ($)") },
                        prefix = { Text("$ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = openLbp,
                        onValueChange = { openLbp = it },
                        label = { Text("الرصيد الافتتاحي بالليرة (L.L)") },
                        suffix = { Text(" ل.ل") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات الوردية") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val usd = openUsd.toDoubleOrNull() ?: 0.0
                        val lbp = openLbp.toLongOrNull() ?: 0L
                        viewModel.openShift(usd, lbp, notes)
                        showOpenShiftDialog = false
                    }
                ) {
                    Text("بدء الوردية")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenShiftDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Close Shift Dialog
    if (showCloseShiftDialog && activeShift != null) {
        val expectedUsd = activeShift.openingCashUsd + activeShift.totalSalesUsd
        var actualUsdInput by remember { mutableStateOf(expectedUsd.toString()) }
        var actualLbpInput by remember { mutableStateOf(activeShift.openingCashLbp.toString()) }
        var closeNotes by remember { mutableStateOf("") }

        val actualUsd = actualUsdInput.toDoubleOrNull() ?: 0.0
        val diff = actualUsd - expectedUsd

        AlertDialog(
            onDismissRequest = { showCloseShiftDialog = false },
            title = { Text("إغلاق وردية الكاشير وجرد الصندوق") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المبلغ المتوقع بالدولار في الدرج: ${PosViewModel.formatUsd(expectedUsd)}")
                    OutlinedTextField(
                        value = actualUsdInput,
                        onValueChange = { actualUsdInput = it },
                        label = { Text("المبلغ الفعلي المعدود بالدولار ($)") },
                        prefix = { Text("$ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = actualLbpInput,
                        onValueChange = { actualLbpInput = it },
                        label = { Text("المبلغ الفعلي المعدود بالليرة (L.L)") },
                        suffix = { Text(" ل.ل") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Difference warning / match
                    Surface(
                        color = if (kotlin.math.abs(diff) < 0.01) Color(0xFFE6F8F0) else Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (kotlin.math.abs(diff) < 0.01) "الصندوق مطابق تماماً!" else "فارق الصندوق: ${PosViewModel.formatUsd(diff)}",
                            fontWeight = FontWeight.Bold,
                            color = if (kotlin.math.abs(diff) < 0.01) EmeraldPrimary else RedAlert,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = closeNotes,
                        onValueChange = { closeNotes = it },
                        label = { Text("ملاحظات الإغلاق") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val usd = actualUsdInput.toDoubleOrNull() ?: 0.0
                        val lbp = actualLbpInput.toLongOrNull() ?: 0L
                        viewModel.closeShift(usd, lbp, closeNotes)
                        showCloseShiftDialog = false
                    }
                ) {
                    Text("تأكيد إغلاق الوردية")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseShiftDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Add Expense Dialog
    if (showAddExpenseDialog) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("اشتراك مولد وكهرباء") }
        var amountUsdInput by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            title = { Text("تسجيل مصروف تشغيلي جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان / بيان المصروف") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("التصنيف (إيجار، رواتب، كهرباء، صيانة)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = amountUsdInput,
                        onValueChange = { amountUsdInput = it },
                        label = { Text("المبلغ بالدولار ($)") },
                        prefix = { Text("$ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountUsdInput.toDoubleOrNull() ?: 0.0
                        viewModel.addExpense(title, category, amt, notes)
                        showAddExpenseDialog = false
                    },
                    enabled = title.isNotBlank() && amountUsdInput.isNotBlank()
                ) {
                    Text("حفظ المصروف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
