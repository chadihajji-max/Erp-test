package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SaleEntity
import com.example.ui.PosUiState
import com.example.ui.PosViewModel
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RedAlert
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.components.sendInvoiceViaWhatsApp

@Composable
fun ReportsScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    sales: List<SaleEntity>,
    expenses: List<ExpenseEntity>,
    products: List<ProductEntity>,
    customers: List<CustomerEntity>
) {
    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf("اليوم") } // اليوم، الأسبوع، الشهر، الكل

    // Compute P&L Metrics
    val completedSales = sales.filter { it.status == "COMPLETED" }
    val totalRevenueUsd = completedSales.sumOf { it.totalUsd }
    val totalRevenueLbp = completedSales.sumOf { it.totalLbp }
    val totalExpensesUsd = expenses.sumOf { it.amountUsd }

    // COGS estimation based on average 35% margin
    val estimatedCogs = totalRevenueUsd * 0.65
    val grossProfit = totalRevenueUsd - estimatedCogs
    val netProfit = grossProfit - totalExpensesUsd
    val netProfitMargin = if (totalRevenueUsd > 0) (netProfit / totalRevenueUsd) * 100 else 0.0

    val totalCustomerDebt = customers.sumOf { it.creditBalanceUsd }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header with Export
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "التقارير المالية والأرباح والخسائر (P&L)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "تحليل المبيعات، تكلفة البضاعة المباعة (COGS) وصافي الأرباح",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            OutlinedButton(
                onClick = {
                    val shareText = """
                        تقرير متجر حجّي - بيروت
                        الفترة: $selectedPeriod
                        إجمالي المبيعات: ${PosViewModel.formatUsd(totalRevenueUsd)} (${PosViewModel.formatLbp(totalRevenueLbp)})
                        المصاريف التشغيلية: ${PosViewModel.formatUsd(totalExpensesUsd)}
                        صافي الربح التقديري: ${PosViewModel.formatUsd(netProfit)}
                        إجمالي ديون الزبائن: ${PosViewModel.formatUsd(totalCustomerDebt)}
                    """.trimIndent()
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "تصدير التقرير المالي (Excel / PDF / WhatsApp)"))
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تصدير التقرير", fontSize = 12.sp)
            }
        }

        // Period filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("اليوم", "هذا الأسبوع", "هذا الشهر", "الكل").forEach { period ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { selectedPeriod = period },
                    label = { Text(period) }
                )
            }
        }

        // Net Profit & Financial P&L Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "صافي الربح التشغيلي (Net Profit)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = PosViewModel.formatUsd(netProfit),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = if (netProfit >= 0) EmeraldPrimary else RedAlert
                    )
                    Text(
                        text = "هامش صافي الربح: ${String.format(java.util.Locale.US, "%.1f", netProfitMargin)}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Breakdown: Sales - COGS - Expenses
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("إجمالي المبيعات:", fontSize = 11.sp)
                        Text(PosViewModel.formatUsd(totalRevenueUsd), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column {
                        Text("تكلفة البضاعة (COGS):", fontSize = 11.sp)
                        Text(PosViewModel.formatUsd(estimatedCogs), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GoldAccent)
                    }
                    Column {
                        Text("المصاريف التشغيلية:", fontSize = 11.sp)
                        Text(PosViewModel.formatUsd(totalExpensesUsd), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RedAlert)
                    }
                }
            }
        }

        // Summary KPI Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiCard(
                title = "عدد الفواتير الصادرة",
                value = "${completedSales.size} فاتورة",
                icon = Icons.Default.ReceiptLong,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            KpiCard(
                title = "ديون الزبائن بالخارج",
                value = PosViewModel.formatUsd(totalCustomerDebt),
                icon = Icons.Default.AccountBalanceWallet,
                color = RedAlert,
                modifier = Modifier.weight(1f)
            )
        }

        // Best-Selling & Top Products Section
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
                    Text(
                        text = "المنتجات الأكثر مبيعاً والأعلى ربحية",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldPrimary)
                }

                Spacer(modifier = Modifier.height(10.dp))

                products.take(4).forEachIndexed { index, product ->
                    val profitPerUnit = product.sellingPriceUsd - product.costPriceUsd
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = product.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = "سعر البيع: ${PosViewModel.formatUsd(product.sellingPriceUsd)}", fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        Text(
                            text = "+${PosViewModel.formatUsd(profitPerUnit)} ربح/قطعة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = EmeraldPrimary
                        )
                    }
                    if (index < 3) HorizontalDivider(color = Color(0xFFF1F5F9))
                }
            }
        }

        // Recent Invoices Log
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "سجل الفواتير الصادرة حديثاً",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (completedSales.isEmpty()) {
                    Text("لا توجد فواتير مكتملة حتى الآن.", fontSize = 12.sp, color = Color.Gray)
                } else {
                    completedSales.take(5).forEach { sale ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.showReceiptForSale(sale) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = sale.invoiceNumber, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text(text = "${sale.cashierName} • ${sale.paymentMethod}", fontSize = 10.sp, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${PosViewModel.formatUsd(sale.totalUsd)} | ${PosViewModel.formatLbp(sale.totalLbp)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { viewModel.showReceiptForSale(sale) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_whatsapp),
                                        contentDescription = "عرض الفاتورة وإرسالها عبر واتساب",
                                        tint = Color(0xFF25D366),
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

@Composable
private fun KpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = title, fontSize = 11.sp, color = Color.DarkGray)
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}
