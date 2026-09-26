package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.data.model.CustomerEntity
import com.example.ui.PosUiState
import com.example.ui.PosViewModel
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RedAlert

@Composable
fun CustomersScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    customers: List<CustomerEntity>
) {
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var settleDebtCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var filterDebtOnly by remember { mutableStateOf(false) }

    val filteredCustomers = remember(customers, searchQuery, filterDebtOnly) {
        customers.filter { cust ->
            val matchesDebt = if (filterDebtOnly) cust.creditBalanceUsd > 0 else true
            val matchesSearch = searchQuery.isBlank() ||
                    cust.name.contains(searchQuery, ignoreCase = true) ||
                    cust.phone.contains(searchQuery)
            matchesDebt && matchesSearch
        }
    }

    val totalDebtUsd = customers.sumOf { it.creditBalanceUsd }
    val totalDebtLbp = (totalDebtUsd * uiState.exchangeRate).toLong()

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
                    text = "دليل العملاء وحسابات الديون",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "متابعة ديون الزبائن، السداد وبرنامج نقاط الولاء",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Button(
                onClick = { showAddCustomerDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("عميل جديد", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Total Outstanding Credit Summary Banner
        Surface(
            color = if (totalDebtUsd > 0) Color(0xFFFEF3C7) else Color(0xFFE6F8F0),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = if (totalDebtUsd > 0) GoldAccent else Color(0xFF006D4D),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "إجمالي الديون المستحقة على العملاء:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${PosViewModel.formatUsd(totalDebtUsd)} ≈ ${PosViewModel.formatLbp(totalDebtLbp)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (totalDebtUsd > 0) Color(0xFFB45309) else Color(0xFF006D4D)
                        )
                    }
                }

                FilterChip(
                    selected = filterDebtOnly,
                    onClick = { filterDebtOnly = !filterDebtOnly },
                    label = { Text("الديون فقط") }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث باسم العميل أو رقم الهاتف...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Customers List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredCustomers) { customer ->
                val hasDebt = customer.creditBalanceUsd > 0
                val debtLbp = (customer.creditBalanceUsd * uiState.exchangeRate).toLong()

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
                                Text(text = customer.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(text = "هاتف: ${customer.phone} | ${customer.address}", fontSize = 12.sp, color = Color.Gray)
                            }

                            // Loyalty points badge
                            Surface(
                                color = Color(0xFFDBEAFE),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${customer.loyaltyPoints} نقطة",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E40AF)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Credit Debt Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "رصيد الحساب / الدين:", fontSize = 11.sp, color = Color.DarkGray)
                                if (hasDebt) {
                                    Text(
                                        text = "${PosViewModel.formatUsd(customer.creditBalanceUsd)} (${PosViewModel.formatLbp(debtLbp)})",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = RedAlert
                                    )
                                } else {
                                    Text(
                                        text = "لا توجد ديون مستحقة (0$)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF006D4D)
                                    )
                                }
                            }

                            if (hasDebt) {
                                Button(
                                    onClick = { settleDebtCustomer = customer },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("سداد دين", fontSize = 12.sp)
                                }
                            }
                        }

                        if (customer.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "ملاحظات: ${customer.notes}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    // Add Customer Dialog
    if (showAddCustomerDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text("إضافة عميل جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم العميل الكامل") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("العنوان / المنطقة") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات إضافية") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addCustomer(name, phone, address, notes)
                        showAddCustomerDialog = false
                    },
                    enabled = name.isNotBlank() && phone.isNotBlank()
                ) {
                    Text("حفظ العميل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Settle Debt Dialog
    settleDebtCustomer?.let { customer ->
        var settleAmountInput by remember { mutableStateOf(customer.creditBalanceUsd.toString()) }

        AlertDialog(
            onDismissRequest = { settleDebtCustomer = null },
            title = { Text("سداد دين العميل: ${customer.name}") },
            text = {
                Column {
                    Text("إجمالي الدين الحالي: ${PosViewModel.formatUsd(customer.creditBalanceUsd)}")
                    Text("ما يعادل بالليرة: ${PosViewModel.formatLbp((customer.creditBalanceUsd * uiState.exchangeRate).toLong())}")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = settleAmountInput,
                        onValueChange = { settleAmountInput = it },
                        label = { Text("المبلغ المسدد بالدولار ($)") },
                        prefix = { Text("$ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = settleAmountInput.toDoubleOrNull() ?: 0.0
                        viewModel.settleCustomerDebt(customer, amount)
                        settleDebtCustomer = null
                    },
                    enabled = settleAmountInput.isNotBlank()
                ) {
                    Text("تأكيد السداد")
                }
            },
            dismissButton = {
                TextButton(onClick = { settleDebtCustomer = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
