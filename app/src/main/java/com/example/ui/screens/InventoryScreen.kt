package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.data.model.ProductEntity
import com.example.ui.PosUiState
import com.example.ui.PosViewModel
import com.example.ui.components.CameraBarcodeScannerDialog
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RedAlert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    products: List<ProductEntity>
) {
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showStockAuditDialog by remember { mutableStateOf<ProductEntity?>(null) }
    var showTransferDialog by remember { mutableStateOf<ProductEntity?>(null) }
    var showBarcodeScannerForSearch by remember { mutableStateOf(false) }
    var showBarcodeScannerForAdd by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, LOW_STOCK

    val filteredList = remember(products, searchQuery, selectedFilter) {
        products.filter { p ->
            val matchesFilter = if (selectedFilter == "LOW_STOCK") p.stockQuantity <= p.minStockLevel else true
            val matchesSearch = searchQuery.isBlank() || p.name.contains(searchQuery, ignoreCase = true) || p.barcode.contains(searchQuery)
            matchesFilter && matchesSearch
        }
    }

    val lowStockCount = products.count { it.stockQuantity <= it.minStockLevel }

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
                    text = "إدارة المخزون والمستودعات",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "إجمالي المنتجات: ${products.size} صنف",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { viewModel.checkInventoryLevelsAndAlert() },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("فحص المخزون", fontSize = 12.sp)
                }

                Button(
                    onClick = { showAddProductDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة منتج", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Low stock warning banner if any
        if (lowStockCount > 0) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFEE2E2),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedFilter = "LOW_STOCK" }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = RedAlert)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "تنبيه: يوجد $lowStockCount منتج وصل لحد الأمان أو نفد!",
                            color = RedAlert,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "اضغط هنا لعرض الأصناف المحتاجة للطلب والشراء",
                            color = Color.DarkGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Search and Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث بالاسم أو الباركود...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showBarcodeScannerForSearch = true }) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "مسح باركود الصنف بالكاميرا",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("الكل") }
            )

            FilterChip(
                selected = selectedFilter == "LOW_STOCK",
                onClick = { selectedFilter = "LOW_STOCK" },
                label = { Text("ناقص ($lowStockCount)") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Products List with margins & stock actions (Adaptive 1 or 2 columns for Foldables)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 340.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(filteredList) { product ->
                val profitUsd = product.sellingPriceUsd - product.costPriceUsd
                val marginPercent = if (product.sellingPriceUsd > 0) (profitUsd / product.sellingPriceUsd) * 100 else 0.0
                val isLow = product.stockQuantity <= product.minStockLevel

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "فئة: ${product.category} | باركود: ${product.barcode} | وحدة: ${product.unit}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            // Stock status badge
                            Surface(
                                color = if (isLow) Color(0xFFFEE2E2) else Color(0xFFE6F8F0),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "المخزون: ${product.stockQuantity} ${product.unit}",
                                    color = if (isLow) RedAlert else EmeraldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))

                        // Financial Margin Row (Cost vs Selling)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("تكلفة الشراء:", fontSize = 11.sp, color = Color.Gray)
                                Text(PosViewModel.formatUsd(product.costPriceUsd), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            Column {
                                Text("سعر البيع:", fontSize = 11.sp, color = Color.Gray)
                                Text(PosViewModel.formatUsd(product.sellingPriceUsd), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Column {
                                Text("هامش الربح:", fontSize = 11.sp, color = Color.Gray)
                                Text(
                                    "${PosViewModel.formatUsd(profitUsd)} (${String.format(java.util.Locale.US, "%.1f", marginPercent)}%)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = GoldAccent
                                )
                            }
                        }

                        if (product.hasVariants && product.variantsDescription.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "المتغيرات: ${product.variantsDescription}",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Stock Take (Audit) and Branch Transfer Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showStockAuditDialog = product }) {
                                Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("جرد دوري وتسوية", fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            TextButton(onClick = { showTransferDialog = product }) {
                                Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تحويل لفرع", fontSize = 11.sp)
                            }

                            IconButton(
                                onClick = { viewModel.deleteProduct(product.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = RedAlert, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Product Dialog
    if (showAddProductDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("إلكترونيات") }
        var barcode by remember { mutableStateOf("") }
        var costPrice by remember { mutableStateOf("") }
        var sellingPrice by remember { mutableStateOf("") }
        var quantity by remember { mutableStateOf("") }
        var minStock by remember { mutableStateOf("5") }
        var unit by remember { mutableStateOf("قطعة") }
        var hasVariants by remember { mutableStateOf(false) }
        var variantsInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("إضافة منتج جديد للكتالوج") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم المنتج") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("الفئة (إلكترونيات، ملابس، إلخ)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text("الباركود (أو امسحه بالكاميرا)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalIconButton(
                            onClick = { showBarcodeScannerForAdd = true },
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "مسح باركود الصنف بالكاميرا")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = costPrice,
                            onValueChange = { costPrice = it },
                            label = { Text("التكلفة ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sellingPrice,
                            onValueChange = { sellingPrice = it },
                            label = { Text("سعر البيع ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("الكمية") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minStock,
                            onValueChange = { minStock = it },
                            label = { Text("حد الأمان") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("وحدة القياس (قطعة، كغ، علبة...)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasVariants, onCheckedChange = { hasVariants = it })
                        Text("المنتج يحتوي على متغيرات (ألوان / مقاسات)")
                    }

                    if (hasVariants) {
                        OutlinedTextField(
                            value = variantsInput,
                            onValueChange = { variantsInput = it },
                            label = { Text("المتغيرات مفصولة بفواصل (مثال: S, M, L, XL)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cost = costPrice.toDoubleOrNull() ?: 0.0
                        val selling = sellingPrice.toDoubleOrNull() ?: 0.0
                        val qty = quantity.toIntOrNull() ?: 0
                        val min = minStock.toIntOrNull() ?: 5
                        val variantNames = if (hasVariants && variantsInput.isNotBlank()) {
                            variantsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        } else emptyList()
                        val variantsList = if (variantNames.isNotEmpty()) {
                            val perVariantQty = qty / variantNames.size
                            variantNames.map { it to perVariantQty }
                        } else emptyList()

                        viewModel.addProduct(
                            name = name,
                            category = category,
                            barcode = barcode,
                            costPriceUsd = cost,
                            sellingPriceUsd = selling,
                            stockQuantity = qty,
                            minStockLevel = min,
                            unit = unit,
                            hasVariants = hasVariants,
                            variantsList = variantsList
                        )
                        showAddProductDialog = false
                    },
                    enabled = name.isNotBlank() && sellingPrice.isNotBlank()
                ) {
                    Text("حفظ المنتج")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) {
                    Text("إلغاء")
                }
            }
        )

        // Camera Barcode Scanner for New Product Entry
        if (showBarcodeScannerForAdd) {
            CameraBarcodeScannerDialog(
                products = products,
                onDismiss = { showBarcodeScannerForAdd = false },
                onProductFound = { prod ->
                    barcode = prod.barcode
                    showBarcodeScannerForAdd = false
                },
                onManualSearch = { code ->
                    barcode = code
                    showBarcodeScannerForAdd = false
                }
            )
        }
    }

    // Stock Take (Audit) Dialog
    showStockAuditDialog?.let { prod ->
        var actualCountInput by remember { mutableStateOf(prod.stockQuantity.toString()) }
        var reasonInput by remember { mutableStateOf("تسوية جرد دوري في المتجر") }

        AlertDialog(
            onDismissRequest = { showStockAuditDialog = null },
            title = { Text("جرد دوري وتسوية المخزون") },
            text = {
                Column {
                    Text("المنتج: ${prod.name}")
                    Text("الكمية المسجلة حالياً في النظام: ${prod.stockQuantity} ${prod.unit}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = actualCountInput,
                        onValueChange = { actualCountInput = it },
                        label = { Text("الكمية الفعلية بعد العد اليدوي") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        label = { Text("سبب التسوية / ملاحظات الفارق") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val actual = actualCountInput.toIntOrNull() ?: prod.stockQuantity
                        viewModel.recordStockAudit(prod, actual, reasonInput)
                        showStockAuditDialog = null
                    }
                ) {
                    Text("تأكيد التسوية")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStockAuditDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Transfer Stock Dialog
    showTransferDialog?.let { prod ->
        var targetBranch by remember { mutableStateOf("فرع الحمرا") }
        var transferQty by remember { mutableStateOf("1") }

        AlertDialog(
            onDismissRequest = { showTransferDialog = null },
            title = { Text("تحويل مخزون بين الفروع") },
            text = {
                Column {
                    Text("المنتج: ${prod.name}")
                    Text("الكمية المتوفرة في ${uiState.currentBranch}: ${prod.stockQuantity}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("الفرع المستلم:")
                    listOf("فرع الحمرا", "فرع صيدا", "فرع طرابلس", "المستودع المركزي").forEach { branch ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { targetBranch = branch }
                        ) {
                            RadioButton(selected = targetBranch == branch, onClick = { targetBranch = branch })
                            Text(branch)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = transferQty,
                        onValueChange = { transferQty = it },
                        label = { Text("الكمية المراد تحويلها") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = transferQty.toIntOrNull() ?: 1
                        viewModel.transferStock(prod, targetBranch, qty)
                        showTransferDialog = null
                    }
                ) {
                    Text("تأكيد التحويل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Camera Barcode Scanner for Inventory Search
    if (showBarcodeScannerForSearch) {
        CameraBarcodeScannerDialog(
            products = products,
            onDismiss = { showBarcodeScannerForSearch = false },
            onProductFound = { prod ->
                searchQuery = prod.barcode
                showBarcodeScannerForSearch = false
            },
            onManualSearch = { code ->
                searchQuery = code
                showBarcodeScannerForSearch = false
            }
        )
    }
}
