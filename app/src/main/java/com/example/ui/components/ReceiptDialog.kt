package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.SaleEntity
import com.example.data.model.SaleItemEntity
import com.example.ui.PosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.ui.res.painterResource
import com.example.R

@Composable
fun ReceiptDialog(
    sale: SaleEntity?,
    items: List<SaleItemEntity>,
    onDismiss: () -> Unit,
    onPrint: () -> Unit
) {
    if (sale == null) return
    val context = LocalContext.current
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(sale.timestamp))

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
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                    Text(
                        text = "معاينة إيصال الدفع",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Thermal Receipt Paper Simulation
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFFCFDFD),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(4.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "HAJJI STORE / متجر حجّي",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "بيروت - لبنان | هاتف: 01-889900",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "الفرع: ${sale.branchName} | الكاشير: ${sale.cashierName}",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "التاريخ: $dateStr",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "رقم الفاتورة: ${sale.invoiceNumber}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        if (sale.customerName != null) {
                            Text(
                                text = "العميل: ${sale.customerName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Exchange Rate Badge
                        Box(
                            modifier = Modifier
                                .padding(vertical = 6.dp)
                                .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "سعر الصرف المعتمد: 1$ = ${PosViewModel.formatLbp(sale.exchangeRateAtSale.toLong())}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 1.dp,
                            color = Color.LightGray
                        )

                        // Items Header
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "المنتج",
                                modifier = Modifier.weight(2f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "الكمية",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "السعر",
                                modifier = Modifier.weight(1.2f),
                                textAlign = TextAlign.End,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Items List
                        items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Column(modifier = Modifier.weight(2f)) {
                                    Text(
                                        text = item.productName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (item.variantName.isNotBlank()) {
                                        Text(
                                            text = "(${item.variantName})",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                Text(
                                    text = "${item.quantity}",
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = PosViewModel.formatUsd(item.totalPriceUsd),
                                    modifier = Modifier.weight(1.2f),
                                    textAlign = TextAlign.End,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 1.dp,
                            color = Color.LightGray
                        )

                        // Financial Breakdown
                        ReceiptRow(label = "المجموع الفرعي:", value = PosViewModel.formatUsd(sale.subtotalUsd))
                        if (sale.discountUsd > 0) {
                            ReceiptRow(
                                label = "الخصم:",
                                value = "- ${PosViewModel.formatUsd(sale.discountUsd)}",
                                color = Color(0xFFDC2626)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "الإجمالي الصافي بالدولار:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = PosViewModel.formatUsd(sale.totalUsd),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "الإجمالي بالليرة اللبنانية:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = PosViewModel.formatLbp(sale.totalLbp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 1.dp,
                            color = Color.LightGray
                        )

                        // Payment Details
                        val methodLabel = when (sale.paymentMethod) {
                            "CASH_USD" -> "نقدي (دولار)"
                            "CASH_LBP" -> "نقدي (ليرة)"
                            "SPLIT_USD_LBP" -> "دفع مدمج (دولار + ليرة)"
                            "CARD" -> "بطاقة بنكية"
                            "CREDIT" -> "بيع بالدين / آجل"
                            else -> sale.paymentMethod
                        }
                        ReceiptRow(label = "طريقة الدفع:", value = methodLabel)

                        if (sale.paidUsd > 0) {
                            ReceiptRow(label = "المدفوع بالدولار:", value = PosViewModel.formatUsd(sale.paidUsd))
                        }
                        if (sale.paidLbp > 0) {
                            ReceiptRow(label = "المدفوع بالليرة:", value = PosViewModel.formatLbp(sale.paidLbp))
                        }
                        if (sale.changeUsd > 0) {
                            ReceiptRow(
                                label = "الباقي (فكة بالدولار):",
                                value = PosViewModel.formatUsd(sale.changeUsd),
                                color = Color(0xFF006D4D)
                            )
                        }
                        if (sale.changeLbp > 0) {
                            ReceiptRow(
                                label = "الباقي (فكة بالليرة):",
                                value = PosViewModel.formatLbp(sale.changeLbp),
                                color = Color(0xFF006D4D)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        // Barcode Simulation
                        Text(
                            text = "||||| ||||||| |||| |||||||| |||||||||||",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = sale.invoiceNumber,
                            fontSize = 10.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "شكراً لزيارتكم! البضاعة المباعة تستبدل خلال 3 أيام مع الفاتورة",
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Prominent WhatsApp Button
                Button(
                    onClick = {
                        sendInvoiceViaWhatsApp(context = context, sale = sale, items = items)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_whatsapp),
                        contentDescription = "WhatsApp",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إرسال الفاتورة عبر واتساب",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Actions Row: Print & General Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPrint,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("طباعة حرارية", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val msg = formatInvoiceText(sale, items)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, msg)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة الإيصال"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة عامة", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * تنسيق نص الفاتورة لمشاركته عبر واتساب أو الرسائل
 */
fun formatInvoiceText(sale: SaleEntity, items: List<SaleItemEntity>): String {
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(sale.timestamp))
    val itemsLines = if (items.isNotEmpty()) {
        items.joinToString("\n") { item ->
            val variant = if (item.variantName.isNotBlank()) " (${item.variantName})" else ""
            "▪️ ${item.productName}$variant × ${item.quantity} = ${PosViewModel.formatUsd(item.totalPriceUsd)}"
        }
    } else "▪️ مشتريات عامة"

    val methodArabic = when (sale.paymentMethod) {
        "CASH_USD" -> "نقدي (دولار)"
        "CASH_LBP" -> "نقدي (ليرة)"
        "SPLIT_USD_LBP" -> "دفع مدمج (دولار + ليرة)"
        "CARD" -> "بطاقة مصرفية"
        "CREDIT" -> "آجل / بالدين"
        else -> sale.paymentMethod
    }

    return buildString {
        appendLine("🧾 *إيصال شراء - متجر حجّي (HAJJI STORE)*")
        appendLine("📍 بيروت - لبنان | هاتف: 01-889900")
        appendLine("━━━━━━━━━━━━━━━━━━━")
        appendLine("🔢 *رقم الفاتورة:* ${sale.invoiceNumber}")
        appendLine("📅 *التاريخ:* $dateStr")
        if (!sale.customerName.isNullOrBlank()) {
            appendLine("👤 *الزبون:* ${sale.customerName}")
        }
        appendLine("👨‍💼 *الكاشير:* ${sale.cashierName} (${sale.branchName})")
        appendLine("━━━━━━━━━━━━━━━━━━━")
        appendLine("🛒 *الأصناف:*")
        appendLine(itemsLines)
        appendLine("━━━━━━━━━━━━━━━━━━━")
        if (sale.discountUsd > 0) {
            appendLine("🏷️ *المجموع الفرعي:* ${PosViewModel.formatUsd(sale.subtotalUsd)}")
            appendLine("✂️ *الخصم:* ${PosViewModel.formatUsd(sale.discountUsd)}")
        }
        appendLine("💵 *الإجمالي بالدولار:* ${PosViewModel.formatUsd(sale.totalUsd)}")
        appendLine("🇱🇧 *الإجمالي بالليرة:* ${PosViewModel.formatLbp(sale.totalLbp)}")
        appendLine("💱 *سعر الصرف:* 1$ = ${PosViewModel.formatLbp(sale.exchangeRateAtSale.toLong())}")
        appendLine("💳 *طريقة الدفع:* $methodArabic")
        if (sale.paidUsd > 0) appendLine("• المدفوع دولار: ${PosViewModel.formatUsd(sale.paidUsd)}")
        if (sale.paidLbp > 0) appendLine("• المدفوع ليرة: ${PosViewModel.formatLbp(sale.paidLbp)}")
        if (sale.changeUsd > 0) appendLine("• الباقي دولار: ${PosViewModel.formatUsd(sale.changeUsd)}")
        if (sale.changeLbp > 0) appendLine("• الباقي ليرة: ${PosViewModel.formatLbp(sale.changeLbp)}")
        appendLine("━━━━━━━━━━━━━━━━━━━")
        appendLine("شكراً لتسوقكم معنا! البضاعة تستبدل خلال 3 أيام مع إبراز الفاتورة.")
    }
}

/**
 * إرسال الفاتورة مباشرة عبر تطبيق واتساب أو الرابط المباشر
 */
fun sendInvoiceViaWhatsApp(
    context: android.content.Context,
    sale: SaleEntity,
    items: List<SaleItemEntity>,
    customerPhone: String? = null
) {
    val message = formatInvoiceText(sale, items)
    val encodedMessage = try {
        java.net.URLEncoder.encode(message, "UTF-8")
    } catch (e: Exception) {
        message
    }

    try {
        // إذا كان هاتف العميل متوفراً، يتم الإرسال المباشر لرقم العميل
        val rawPhone = customerPhone?.filter { it.isDigit() } ?: ""
        val url = if (rawPhone.isNotBlank()) {
            val formattedPhone = if (rawPhone.startsWith("0")) {
                "961" + rawPhone.substring(1) // تحويل رقم لبنان المحلي 03 / 70 إلى الصيغة الدولية
            } else if (!rawPhone.startsWith("961") && rawPhone.length <= 8) {
                "961$rawPhone"
            } else rawPhone
            "https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMessage"
        } else {
            "https://api.whatsapp.com/send?text=$encodedMessage"
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse(url)
            `package` = "com.whatsapp"
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // في حال عدم وجود تطبيق واتساب الرسمي، يتم فتح رابط الويب أو منتقي المشاركة
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(
                "https://api.whatsapp.com/send?text=$encodedMessage"
            ))
            context.startActivity(webIntent)
        } catch (e2: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(shareIntent, "إرسال الفاتورة عبر واتساب"))
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String, color: Color = Color.Black) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.DarkGray)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
