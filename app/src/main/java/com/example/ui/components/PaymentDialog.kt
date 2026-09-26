package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CustomerEntity
import com.example.ui.PosViewModel
import com.example.ui.theme.GoldAccent
import java.util.Locale

@Composable
fun PaymentDialog(
    totalUsd: Double,
    totalLbp: Long,
    exchangeRate: Double,
    selectedCustomer: CustomerEntity?,
    onDismiss: () -> Unit,
    onCompletePayment: (
        paymentMethod: String,
        paidUsd: Double,
        paidLbp: Long,
        changeUsd: Double,
        changeLbp: Long,
        notes: String
    ) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("SPLIT_USD_LBP") }
    var inputPaidUsd by remember { mutableStateOf(totalUsd.toString()) }
    var inputPaidLbp by remember { mutableStateOf("0") }
    var changeCurrencyUsd by remember { mutableStateOf(true) } // true for USD change, false for LBP change
    var paymentNotes by remember { mutableStateOf("") }

    // Synchronize inputs based on payment method
    LaunchedEffect(selectedMethod) {
        when (selectedMethod) {
            "CASH_USD" -> {
                inputPaidUsd = String.format(Locale.US, "%.2f", totalUsd)
                inputPaidLbp = "0"
            }
            "CASH_LBP" -> {
                inputPaidUsd = "0"
                inputPaidLbp = totalLbp.toString()
            }
            "SPLIT_USD_LBP" -> {
                // Default split: half in USD, rest in LBP
                val halfUsd = (totalUsd / 2.0).toInt().toDouble()
                inputPaidUsd = halfUsd.toString()
                val remUsd = totalUsd - halfUsd
                inputPaidLbp = (remUsd * exchangeRate).toLong().toString()
            }
            "CARD" -> {
                inputPaidUsd = String.format(Locale.US, "%.2f", totalUsd)
                inputPaidLbp = "0"
            }
            "CREDIT" -> {
                inputPaidUsd = "0"
                inputPaidLbp = "0"
            }
        }
    }

    val paidUsdNum = inputPaidUsd.toDoubleOrNull() ?: 0.0
    val paidLbpNum = inputPaidLbp.toLongOrNull() ?: 0L

    // Calculate total paid in USD equivalent
    val safeRate = maxOf(1.0, exchangeRate)
    val totalPaidUsdEquivalent = paidUsdNum + (paidLbpNum / safeRate)
    val differenceUsd = totalPaidUsdEquivalent - totalUsd
    val isUnderpaid = differenceUsd < -0.01 && selectedMethod != "CREDIT"

    // Change calculations
    val changeUsd = if (differenceUsd > 0 && changeCurrencyUsd) differenceUsd else 0.0
    val changeLbp = if (differenceUsd > 0 && !changeCurrencyUsd) (differenceUsd * exchangeRate).toLong() else 0L

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إلغاء")
                    }
                    Text(
                        text = "الدفع وتسوية الفاتورة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dual Currency Total Display Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "المبلغ المطلوب للدفع",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = PosViewModel.formatUsd(totalUsd),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "≈ ${PosViewModel.formatLbp(totalLbp)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                        Text(
                            text = "سعر الصرف: 1$ = ${PosViewModel.formatLbp(exchangeRate.toLong())}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = GoldAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Payment Method Selector
                Text(
                    text = "طريقة الدفع:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentMethodChip(
                        title = "دولار + ليرة (مدمج)",
                        selected = selectedMethod == "SPLIT_USD_LBP",
                        modifier = Modifier.weight(1.2f),
                        onClick = { selectedMethod = "SPLIT_USD_LBP" }
                    )
                    PaymentMethodChip(
                        title = "نقدي دولار",
                        selected = selectedMethod == "CASH_USD",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedMethod = "CASH_USD" }
                    )
                    PaymentMethodChip(
                        title = "نقدي ليرة",
                        selected = selectedMethod == "CASH_LBP",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedMethod = "CASH_LBP" }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentMethodChip(
                        title = "بطاقة (POS/Card)",
                        selected = selectedMethod == "CARD",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedMethod = "CARD" }
                    )
                    PaymentMethodChip(
                        title = "آجل / دين (Credit)",
                        selected = selectedMethod == "CREDIT",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedMethod = "CREDIT" }
                    )
                }

                if (selectedMethod == "CREDIT") {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (selectedCustomer != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = GoldAccent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "العميل: ${selectedCustomer.name}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "سيتم تسجيل مبلغ ${PosViewModel.formatUsd(totalUsd)} كدين على حساب العميل",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تنبيه: يجب تحديد عميل من شاشة البيع لتسجيل الدين باسمه!",
                                    fontSize = 12.sp,
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input Fields for Paid USD / LBP
                if (selectedMethod != "CREDIT") {
                    if (selectedMethod == "CASH_USD" || selectedMethod == "SPLIT_USD_LBP" || selectedMethod == "CARD") {
                        OutlinedTextField(
                            value = inputPaidUsd,
                            onValueChange = { inputPaidUsd = it },
                            label = { Text("المدفوع نقداً بالدولار ($)") },
                            prefix = { Text("$ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (selectedMethod == "CASH_LBP" || selectedMethod == "SPLIT_USD_LBP") {
                        OutlinedTextField(
                            value = inputPaidLbp,
                            onValueChange = { inputPaidLbp = it },
                            label = { Text("المدفوع نقداً بالليرة اللبنانية (L.L)") },
                            suffix = { Text(" ل.ل") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Quick presets for Split Payment or Cash
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "مبالغ سريعة:", fontSize = 11.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(10.0, 20.0, 50.0, 100.0).forEach { usdPreset ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    inputPaidUsd = usdPreset.toString()
                                    if (selectedMethod == "SPLIT_USD_LBP") {
                                        val remainingUsd = maxOf(0.0, totalUsd - usdPreset)
                                        inputPaidLbp = (remainingUsd * exchangeRate).toLong().toString()
                                    }
                                },
                                label = { Text("$$usdPreset", fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Change / Remaining Status Card
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isUnderpaid) Color(0xFFFEE2E2) else Color(0xFFE6F8F0),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (isUnderpaid) {
                                val shortUsd = totalUsd - totalPaidUsdEquivalent
                                val shortLbp = (shortUsd * exchangeRate).toLong()
                                Text(
                                    text = "المبلغ المدفوع غير كافٍ!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFDC2626)
                                )
                                Text(
                                    text = "المتبقي للدفع: ${PosViewModel.formatUsd(shortUsd)} (أو ${PosViewModel.formatLbp(shortLbp)})",
                                    fontSize = 12.sp,
                                    color = Color(0xFFDC2626)
                                )
                            } else {
                                val excessUsd = differenceUsd
                                val excessLbp = (excessUsd * exchangeRate).toLong()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "الباقي للزبون (الفكة):",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF006D4D)
                                        )
                                        Text(
                                            text = if (changeCurrencyUsd) PosViewModel.formatUsd(excessUsd) else PosViewModel.formatLbp(excessLbp),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = Color(0xFF006D4D)
                                        )
                                    }

                                    // Toggle Change Currency
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "إرجاع الفكة بـ:",
                                            fontSize = 11.sp,
                                            color = Color.DarkGray
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Button(
                                            onClick = { changeCurrencyUsd = !changeCurrencyUsd },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(if (changeCurrencyUsd) "$ دولار" else "ل.ل ليرة", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val isCreditWithoutCustomer = selectedMethod == "CREDIT" && selectedCustomer == null
                val canConfirmPayment = if (selectedMethod == "CREDIT") {
                    selectedCustomer != null
                } else {
                    !isUnderpaid
                }

                // Confirm Payment Button
                Button(
                    onClick = {
                        onCompletePayment(
                            selectedMethod,
                            paidUsdNum,
                            paidLbpNum,
                            changeUsd,
                            changeLbp,
                            paymentNotes
                        )
                    },
                    enabled = canConfirmPayment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCreditWithoutCustomer) "اختر عميلاً لتسجيل الدين أولاً" else "تأكيد الدفع وإصدار الفاتورة",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodChip(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFFD1D5DB),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
