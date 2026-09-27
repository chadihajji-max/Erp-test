package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseEntity
import com.example.data.model.ShiftEntity
import com.example.ui.PosUiState
import com.example.ui.PosViewModel
import com.example.ui.components.AddEditExpenseDialog
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RedAlert
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ShiftsExpensesScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    activeShift: ShiftEntity?,
    expenses: List<ExpenseEntity>
) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableStateOf(1) } // 0: Shift & Drawer, 1: Daily Expenses, 2: Monthly Report
    var showOpenShiftDialog by remember { mutableStateOf(false) }
    var showCloseShiftDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }

    // Search and filter for Daily Expenses
    var expenseSearchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("الكل") }

    // Monthly Report state (Calendar month offset: 0 is current month, -1 is previous, etc.)
    var selectedMonthCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        })
    }

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale("ar")) }
    val monthYearHeader = monthYearFormat.format(selectedMonthCalendar.time)

    // Filter expenses for selected month
    val monthlyExpenses = remember(expenses, selectedMonthCalendar) {
        val cal = Calendar.getInstance()
        val targetMonth = selectedMonthCalendar.get(Calendar.MONTH)
        val targetYear = selectedMonthCalendar.get(Calendar.YEAR)
        expenses.filter { expense ->
            cal.timeInMillis = expense.date
            cal.get(Calendar.MONTH) == targetMonth && cal.get(Calendar.YEAR) == targetYear
        }
    }

    val totalMonthlyExpensesUsd = monthlyExpenses.sumOf { it.amountUsd }
    val totalMonthlyExpensesLbp = monthlyExpenses.sumOf { it.amountLbp }

    // Today's expenses
    val todayExpenses = remember(expenses) {
        val todayCal = Calendar.getInstance()
        val cal = Calendar.getInstance()
        expenses.filter { expense ->
            cal.timeInMillis = expense.date
            cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
        }
    }
    val todayTotalUsd = todayExpenses.sumOf { it.amountUsd }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "المصاريف والورديات اليومية",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "تسجيل المصاريف اليومية وتصنيفها، التقرير الشهري، ودرج النقود",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Button(
                onClick = { showAddExpenseDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تسجيل مصروف", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3 Sub-Tabs: Shift, Daily Expenses, Monthly Report
        TabRow(selectedTabIndex = selectedSubTab) {
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("المصاريف اليومية (${expenses.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = { Text("التقرير الشهري للمصاريف", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("وردية الكاشير والصندوق", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedSubTab) {
            0 -> {
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
            }

            1 -> {
                // Tab 1: Daily Expenses List & Form
                val filteredExpenses = remember(expenses, expenseSearchQuery, selectedCategoryFilter) {
                    expenses.filter { expense ->
                        val matchesSearch = expenseSearchQuery.isBlank() ||
                                expense.title.contains(expenseSearchQuery, ignoreCase = true) ||
                                expense.notes.contains(expenseSearchQuery, ignoreCase = true)
                        val matchesCat = selectedCategoryFilter == "الكل" || expense.category == selectedCategoryFilter
                        matchesSearch && matchesCat
                    }
                }

                val allCategories = remember(expenses) {
                    listOf("الكل") + expenses.map { it.category }.distinct()
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Summary Banner Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("إجمالي مصاريف اليوم:", fontSize = 11.sp, color = Color.DarkGray)
                                Text(
                                    text = PosViewModel.formatUsd(todayTotalUsd),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = RedAlert
                                )
                                Text(
                                    text = "${todayExpenses.size} مصاريف مسجلة اليوم",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("إجمالي المصاريف الكلية:", fontSize = 11.sp, color = Color.DarkGray)
                                Text(
                                    text = PosViewModel.formatUsd(expenses.sumOf { it.amountUsd }),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${expenses.size} حركة مصروف مسجلة",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search & Category Chips
                    OutlinedTextField(
                        value = expenseSearchQuery,
                        onValueChange = { expenseSearchQuery = it },
                        placeholder = { Text("بحث عن بيان أو تفاصيل مصروف...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (expenseSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { expenseSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Categories Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allCategories) { category ->
                            FilterChip(
                                selected = selectedCategoryFilter == category,
                                onClick = { selectedCategoryFilter = category },
                                label = { Text(category, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Expenses List with Edit & Delete actions
                    if (filteredExpenses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("لا توجد مصاريف مطابقة للبحث", fontSize = 13.sp, color = Color.Gray)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredExpenses, key = { it.id }) { expense ->
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
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = expense.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = Color(0xFFFEF3C7),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = expense.category,
                                                        color = Color(0xFF92400E),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "📅 $dateStr | الفرع: ${expense.branchName} | الكاشير: ${expense.cashierName}",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                            if (expense.notes.isNotBlank()) {
                                                Text(
                                                    text = "ملاحظات: ${expense.notes}",
                                                    fontSize = 11.sp,
                                                    color = Color.DarkGray
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = PosViewModel.formatUsd(expense.amountUsd),
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 15.sp,
                                                    color = RedAlert
                                                )
                                                Text(
                                                    text = PosViewModel.formatLbp(expense.amountLbp),
                                                    fontSize = 10.sp,
                                                    color = Color.DarkGray
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            // Edit Button
                                            IconButton(
                                                onClick = { expenseToEdit = expense },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "تعديل المصروف",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            // Delete Button
                                            IconButton(
                                                onClick = { viewModel.deleteExpense(expense) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "حذف المصروف",
                                                    tint = RedAlert,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Tab 2: Monthly Expenses Report (التقرير الشهري للمصاريف)
                val categoryGroups = remember(monthlyExpenses) {
                    monthlyExpenses.groupBy { it.category }
                        .mapValues { entry -> entry.value.sumOf { it.amountUsd } }
                        .toList()
                        .sortedByDescending { it.second }
                }

                // Days count in current selected month
                val daysInMonth = remember(selectedMonthCalendar) {
                    selectedMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                }
                val dailyAverageUsd = if (daysInMonth > 0) totalMonthlyExpensesUsd / daysInMonth else 0.0

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Month Navigator & Share Header
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                val newCal = selectedMonthCalendar.clone() as Calendar
                                newCal.add(Calendar.MONTH, -1)
                                selectedMonthCalendar = newCal
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "الشهر السابق")
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "تقرير شهر: $monthYearHeader",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${monthlyExpenses.size} مصاريف مسجلة هذا الشهر",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            IconButton(onClick = {
                                val newCal = selectedMonthCalendar.clone() as Calendar
                                newCal.add(Calendar.MONTH, 1)
                                selectedMonthCalendar = newCal
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "الشهر القادم")
                            }
                        }
                    }

                    // Key Metrics Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total Month Expenses Card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF2F2),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("إجمالي مصاريف الشهر:", fontSize = 11.sp, color = Color.DarkGray)
                                Text(
                                    text = PosViewModel.formatUsd(totalMonthlyExpensesUsd),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = RedAlert
                                )
                                Text(
                                    text = "≈ ${PosViewModel.formatLbp(totalMonthlyExpensesLbp)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }

                        // Daily Average Card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF6FF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("المتوسط اليومي:", fontSize = 11.sp, color = Color.DarkGray)
                                Text(
                                    text = PosViewModel.formatUsd(dailyAverageUsd),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E40AF)
                                )
                                Text(
                                    text = "على مدار $daysInMonth يوماً",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Categorical Breakdown Card (توزيع المصاريف حسب التصنيف)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "توزيع المصاريف حسب التصنيف (Breakdown)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${categoryGroups.size} تصنيفات نشطة",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            if (categoryGroups.isEmpty()) {
                                Text(
                                    text = "لا توجد مصاريف مسجلة لشهر $monthYearHeader",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                categoryGroups.forEach { (catName, catTotalUsd) ->
                                    val percentage = if (totalMonthlyExpensesUsd > 0) (catTotalUsd / totalMonthlyExpensesUsd) else 0.0
                                    val percentDisplay = (percentage * 100).toInt()

                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = catName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = "($percentDisplay%)", fontSize = 11.sp, color = Color.Gray)
                                            }
                                            Text(
                                                text = PosViewModel.formatUsd(catTotalUsd),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = RedAlert
                                            )
                                        }

                                        LinearProgressIndicator(
                                            progress = { percentage.toFloat() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = Color(0xFFF1F5F9)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Export / Share Monthly Report Button
                    OutlinedButton(
                        onClick = {
                            val breakdownText = categoryGroups.joinToString("\n") { (name, total) ->
                                val pct = if (totalMonthlyExpensesUsd > 0) ((total / totalMonthlyExpensesUsd) * 100).toInt() else 0
                                "▪️ $name: ${PosViewModel.formatUsd(total)} ($pct%)"
                            }
                            val shareMessage = """
                                📊 *التقرير الشهري للمصاريف التشغيلية - متجر حجّي*
                                📅 شهر: $monthYearHeader
                                ━━━━━━━━━━━━━━━━━━━━
                                💵 إجمالي المصاريف بالدولار: ${PosViewModel.formatUsd(totalMonthlyExpensesUsd)}
                                🇱🇧 إجمالي المصاريف بالليرة: ${PosViewModel.formatLbp(totalMonthlyExpensesLbp)}
                                📈 متوسط الصرف اليومي: ${PosViewModel.formatUsd(dailyAverageUsd)}
                                🔢 إجمالي العمليات المسجلة: ${monthlyExpenses.size}
                                ━━━━━━━━━━━━━━━━━━━━
                                🏷️ *توزيع المصاريف حسب التصنيف:*
                                $breakdownText
                                ━━━━━━━━━━━━━━━━━━━━
                                تم استخراج التقرير بواسطة نظام Hajji POS
                            """.trimIndent()

                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareMessage)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة تقرير المصاريف الشهري"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة وتصدير التقرير الشهري (WhatsApp / نصوص)", fontWeight = FontWeight.Bold)
                    }

                    // Detailed Monthly Expenses Table/List with edit & delete
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "تفاصيل فواتير مصاريف $monthYearHeader",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            if (monthlyExpenses.isEmpty()) {
                                Text("لا توجد فواتير مصاريف في هذا الشهر.", fontSize = 12.sp, color = Color.Gray)
                            } else {
                                monthlyExpenses.forEach { exp ->
                                    val dateStr = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(exp.date))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = exp.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = "($dateStr)", fontSize = 10.sp, color = Color.Gray)
                                            }
                                            Text(text = exp.category, fontSize = 10.sp, color = Color.DarkGray)
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = PosViewModel.formatUsd(exp.amountUsd),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = RedAlert
                                            )

                                            Spacer(modifier = Modifier.width(4.dp))

                                            IconButton(
                                                onClick = { expenseToEdit = exp },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteExpense(exp) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RedAlert, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add or Edit Expense Dialog
    if (showAddExpenseDialog || expenseToEdit != null) {
        AddEditExpenseDialog(
            expenseToEdit = expenseToEdit,
            currentExchangeRate = uiState.exchangeRate,
            cashierName = uiState.currentCashier,
            branchName = uiState.currentBranch,
            onDismiss = {
                showAddExpenseDialog = false
                expenseToEdit = null
            },
            onSave = { title, category, amountUsd, amountLbp, date, notes ->
                if (expenseToEdit != null) {
                    val updated = expenseToEdit!!.copy(
                        title = title,
                        category = category,
                        amountUsd = amountUsd,
                        amountLbp = amountLbp,
                        date = date,
                        notes = notes
                    )
                    viewModel.updateExpense(updated)
                } else {
                    viewModel.addExpense(
                        title = title,
                        category = category,
                        amountUsd = amountUsd,
                        notes = notes,
                        amountLbp = amountLbp,
                        date = date
                    )
                }
                showAddExpenseDialog = false
                expenseToEdit = null
            },
            onDelete = { exp ->
                viewModel.deleteExpense(exp)
                showAddExpenseDialog = false
                expenseToEdit = null
            }
        )
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
}
