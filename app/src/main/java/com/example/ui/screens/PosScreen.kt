package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartItem
import com.example.data.model.CategoryEntity
import com.example.data.model.CustomerEntity
import com.example.data.model.ProductEntity
import com.example.data.model.ProductVariantEntity
import com.example.data.model.SaleEntity
import com.example.ui.PosUiState
import com.example.ui.PosViewModel
import com.example.ui.components.CameraBarcodeScannerDialog
import com.example.ui.components.HoldSalesDialog
import com.example.ui.components.PaymentDialog
import com.example.ui.components.ReceiptDialog
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RedAlert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    products: List<ProductEntity>,
    categories: List<CategoryEntity>,
    heldSales: List<SaleEntity>,
    customers: List<CustomerEntity>
) {
    var showBarcodeScannerDialog by remember { mutableStateOf(false) }
    var showCustomerPickerSheet by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var variantSelectionProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var availableVariants by remember { mutableStateOf<List<ProductVariantEntity>>(emptyList()) }
    var showCartDrawer by remember { mutableStateOf(false) }

    // Filter products
    val filteredProducts = remember(products, uiState.searchQuery, uiState.selectedCategory) {
        products.filter { prod ->
            val matchesCategory = uiState.selectedCategory == "الكل" || prod.category == uiState.selectedCategory
            val matchesSearch = uiState.searchQuery.isBlank() ||
                    prod.name.contains(uiState.searchQuery, ignoreCase = true) ||
                    prod.barcode.contains(uiState.searchQuery)
            matchesCategory && matchesSearch
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTwoPane = maxWidth >= 580.dp

        if (isTwoPane) {
            // Galaxy Z Fold 5 (Unfolded) / Tablet Dual-Pane Canonical POS Layout
            Row(modifier = Modifier.fillMaxSize()) {
                PosProductCatalog(
                    uiState = uiState,
                    viewModel = viewModel,
                    categories = categories,
                    filteredProducts = filteredProducts,
                    heldSales = heldSales,
                    onOpenBarcodeScanner = { showBarcodeScannerDialog = true },
                    onProductClick = { product ->
                        if (product.hasVariants) {
                            variantSelectionProduct = product
                        } else {
                            viewModel.addToCart(product)
                        }
                    },
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                SideCartPane(
                    uiState = uiState,
                    viewModel = viewModel,
                    onSelectCustomer = { showCustomerPickerSheet = true },
                    onApplyDiscount = { showDiscountDialog = true },
                    modifier = Modifier
                        .widthIn(min = 280.dp, max = 340.dp)
                        .fillMaxHeight()
                )
            }
        } else {
            // Galaxy Z Fold 5 (Cover Screen / Phone) Single-Pane Layout
            Box(modifier = Modifier.fillMaxSize()) {
                PosProductCatalog(
                    uiState = uiState,
                    viewModel = viewModel,
                    categories = categories,
                    filteredProducts = filteredProducts,
                    heldSales = heldSales,
                    onOpenBarcodeScanner = { showBarcodeScannerDialog = true },
                    onProductClick = { product ->
                        if (product.hasVariants) {
                            variantSelectionProduct = product
                        } else {
                            viewModel.addToCart(product)
                        }
                    },
                    contentPadding = PaddingValues(bottom = 100.dp),
                    modifier = Modifier.fillMaxSize()
                )

                // Bottom Sticky Cart Bar
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (uiState.cartItems.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Customer selection button
                                AssistChip(
                                    onClick = { showCustomerPickerSheet = true },
                                    label = {
                                        Text(
                                            text = uiState.selectedCustomer?.let { "العميل: ${it.name}" } ?: "تحديد العميل (نقدي)",
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )

                                // Cart items count toggle
                                TextButton(onClick = { showCartDrawer = !showCartDrawer }) {
                                    Text(
                                        text = "السلة (${uiState.cartItemCount} عناصر)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Icon(
                                        if (showCartDrawer) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                        contentDescription = null
                                    )
                                }

                                // Discount button
                                IconButton(onClick = { showDiscountDialog = true }) {
                                    Icon(Icons.Default.Discount, contentDescription = "خصم", tint = GoldAccent)
                                }
                            }
                        }

                        // Active Cart Drawer (expanded items preview)
                        if (showCartDrawer && uiState.cartItems.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(uiState.cartItems) { item ->
                                    CartItemRow(
                                        item = item,
                                        onQuantityChange = { delta -> viewModel.updateCartQuantity(item, delta) },
                                        onRemove = { viewModel.removeFromCart(item) }
                                    )
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        }

                        // Total and Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = PosViewModel.formatUsd(uiState.cartTotalUsd),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = PosViewModel.formatLbp(uiState.cartTotalLbp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (uiState.invoiceDiscountUsd > 0) {
                                    Text(
                                        text = "خصم: -${PosViewModel.formatUsd(uiState.invoiceDiscountUsd)}",
                                        fontSize = 11.sp,
                                        color = RedAlert
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (uiState.cartItems.isNotEmpty()) {
                                    OutlinedIconButton(
                                        onClick = { viewModel.holdCurrentSale() },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(Icons.Default.Pause, contentDescription = "تعليق")
                                    }

                                    OutlinedIconButton(
                                        onClick = { viewModel.clearCart() },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "مسح", tint = RedAlert)
                                    }
                                }

                                Button(
                                    onClick = { viewModel.openPaymentDialog() },
                                    enabled = uiState.cartItems.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("دفع الفاتورة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. Camera Barcode Scanner Dialog (CameraX + ML Kit)
    if (showBarcodeScannerDialog) {
        CameraBarcodeScannerDialog(
            products = products,
            onDismiss = { showBarcodeScannerDialog = false },
            onProductFound = { product ->
                viewModel.addToCart(product)
            },
            onManualSearch = { query ->
                viewModel.setSearchQuery(query)
                showBarcodeScannerDialog = false
            }
        )
    }

    // 2. Invoice Discount Dialog
    if (showDiscountDialog) {
        var discountInput by remember { mutableStateOf(uiState.invoiceDiscountUsd.toString()) }
        AlertDialog(
            onDismissRequest = { showDiscountDialog = false },
            title = { Text("خصم على إجمالي الفاتورة") },
            text = {
                Column {
                    Text("أدخل قيمة الخصم بالدولار ($):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = discountInput,
                        onValueChange = { discountInput = it },
                        prefix = { Text("$ ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val disc = discountInput.toDoubleOrNull() ?: 0.0
                        viewModel.setInvoiceDiscount(disc)
                        showDiscountDialog = false
                    }
                ) {
                    Text("تطبيق الخصم")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscountDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 3. Customer Picker Dialog
    if (showCustomerPickerSheet) {
        AlertDialog(
            onDismissRequest = { showCustomerPickerSheet = false },
            title = { Text("تحديد العميل") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Card(
                            onClick = {
                                viewModel.selectCustomer(null)
                                showCustomerPickerSheet = false
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.selectedCustomer == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "عميل نقدي عام (بدون تسجيل دين)",
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    items(customers) { customer ->
                        Card(
                            onClick = {
                                viewModel.selectCustomer(customer)
                                showCustomerPickerSheet = false
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.selectedCustomer?.id == customer.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = customer.name, fontWeight = FontWeight.Bold)
                                Text(text = "هاتف: ${customer.phone}", fontSize = 11.sp)
                                if (customer.creditBalanceUsd > 0) {
                                    Text(
                                        text = "عليه دين مستحق: ${PosViewModel.formatUsd(customer.creditBalanceUsd)}",
                                        color = RedAlert,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomerPickerSheet = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // 4. Payment Dialog
    if (uiState.isPaymentDialogOpen) {
        PaymentDialog(
            totalUsd = uiState.cartTotalUsd,
            totalLbp = uiState.cartTotalLbp,
            exchangeRate = uiState.exchangeRate,
            selectedCustomer = uiState.selectedCustomer,
            onDismiss = { viewModel.dismissPaymentDialog() },
            onCompletePayment = { method, paidUsd, paidLbp, changeUsd, changeLbp, notes ->
                viewModel.completeCheckout(method, paidUsd, paidLbp, changeUsd, changeLbp, notes)
            }
        )
    }

    // 5. Hold Sales Dialog
    if (uiState.isHoldSalesDialogOpen) {
        HoldSalesDialog(
            heldSales = heldSales,
            onDismiss = { viewModel.dismissHoldSalesDialog() },
            onResumeSale = { sale -> viewModel.resumeHeldSale(sale) }
        )
    }

    // 7. Product Variant Selection Dialog
    variantSelectionProduct?.let { product ->
        LaunchedEffect(product.id) {
            availableVariants = viewModel.getVariantsForProduct(product.id)
        }

        AlertDialog(
            onDismissRequest = { variantSelectionProduct = null },
            title = {
                Column {
                    Text(text = "اختر المقاس / اللون / الموديل", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = product.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (availableVariants.isEmpty()) {
                        Text(
                            text = "لا توجد متغيرات فرعية مسجلة. يمكنك إضافة المنتج مباشرة.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.addToCart(product)
                                variantSelectionProduct = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إضافة الصنف القياسي للسلة")
                        }
                    } else {
                        availableVariants.forEach { variant ->
                            Card(
                                onClick = {
                                    viewModel.addToCart(product, variant)
                                    variantSelectionProduct = null
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = variant.variantName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = "المخزون المتوفر: ${variant.stockQuantity}",
                                            fontSize = 11.sp,
                                            color = if (variant.stockQuantity <= 2) RedAlert else EmeraldPrimary
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = PosViewModel.formatUsd(variant.sellingPriceUsd),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        FilledIconButton(
                                            onClick = {
                                                viewModel.addToCart(product, variant)
                                                variantSelectionProduct = null
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "إضافة", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { variantSelectionProduct = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun ProductCard(
    product: ProductEntity,
    exchangeRate: Double,
    onClick: () -> Unit
) {
    val isLowStock = product.stockQuantity <= product.minStockLevel
    val priceLbp = (product.sellingPriceUsd * exchangeRate).toLong()

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Category tag and low stock alert
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.category,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                if (isLowStock) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "نقص! (${product.stockQuantity})",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedAlert,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = "متوفر: ${product.stockQuantity}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Product Name
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(36.dp)
            )

            if (product.hasVariants) {
                Text(
                    text = "متوفر بمقاسات/ألوان",
                    fontSize = 10.sp,
                    color = GoldAccent
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Price in USD & LBP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = PosViewModel.formatUsd(product.sellingPriceUsd),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = PosViewModel.formatLbp(priceLbp),
                        fontSize = 10.sp,
                        color = Color.DarkGray
                    )
                }

                FilledIconButton(
                    onClick = onClick,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "إضافة",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${PosViewModel.formatUsd(item.unitPriceUsd)} × ${item.quantity} = ${PosViewModel.formatUsd(item.lineTotalUsd)}",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
            }

            // Quantity buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onQuantityChange(-1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "إنقاص", modifier = Modifier.size(16.dp))
                }

                Text(
                    text = "${item.quantity}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                IconButton(
                    onClick = { onQuantityChange(1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "زيادة", modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "حذف", modifier = Modifier.size(16.dp), tint = RedAlert)
                }
            }
        }
    }
}

@Composable
private fun PosProductCatalog(
    uiState: PosUiState,
    viewModel: PosViewModel,
    categories: List<CategoryEntity>,
    filteredProducts: List<ProductEntity>,
    heldSales: List<SaleEntity>,
    onOpenBarcodeScanner: () -> Unit,
    onProductClick: (ProductEntity) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.background)
    ) {
        // Top POS Information Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Branch and Cashier Pill
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${uiState.currentBranch} | ${uiState.currentCashier}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Lebanese Exchange Rate Badge (Interactive)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1$ = ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = PosViewModel.formatLbp(uiState.exchangeRate.toLong()),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFB45309)
                            )
                        }
                    }

                    // Held Sales Button with Badge
                    BadgedBox(
                        badge = {
                            if (heldSales.isNotEmpty()) {
                                Badge { Text("${heldSales.size}") }
                            }
                        }
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.openHoldSalesDialog() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("المعلقة", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search and Barcode row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("بحث بالاسم أو الباركود...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    )

                    Button(
                        onClick = onOpenBarcodeScanner,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "قارئ الباركود", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مسح الباركود", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Categories horizontal list
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(categories) { category ->
                val isSelected = uiState.selectedCategory == category.name
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectCategory(category.name) },
                    label = {
                        Text(
                            text = category.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        // Products Grid with Camera Scan Quick Action
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProducts) { product ->
                    ProductCard(
                        product = product,
                        exchangeRate = uiState.exchangeRate,
                        onClick = { onProductClick(product) }
                    )
                }
            }

            // Quick Scan Floating Action Button
            FloatingActionButton(
                onClick = onOpenBarcodeScanner,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مسح بالكاميرا", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SideCartPane(
    uiState: PosUiState,
    viewModel: PosViewModel,
    onSelectCustomer: () -> Unit,
    onApplyDiscount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Cart Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "سلة المبيعات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${uiState.cartItemCount}",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                if (uiState.cartItems.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearCart() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "مسح السلة",
                            tint = RedAlert,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Customer and Discount quick actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = onSelectCustomer,
                    label = {
                        Text(
                            text = uiState.selectedCustomer?.let { "العميل: ${it.name}" } ?: "عميل نقدي عام",
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.weight(1f)
                )

                OutlinedIconButton(
                    onClick = onApplyDiscount,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Discount, contentDescription = "خصم", tint = GoldAccent, modifier = Modifier.size(16.dp))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Cart Items List
            if (uiState.cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "السلة فارغة",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                        Text(
                            text = "اختر الأصناف أو امسح الباركود بالكاميرا",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(uiState.cartItems) { item ->
                        CartItemRow(
                            item = item,
                            onQuantityChange = { delta -> viewModel.updateCartQuantity(item, delta) },
                            onRemove = { viewModel.removeFromCart(item) }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Cart Totals and Checkout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                if (uiState.invoiceDiscountUsd > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "خصم:", fontSize = 11.sp, color = RedAlert)
                        Text(
                            text = "-${PosViewModel.formatUsd(uiState.invoiceDiscountUsd)}",
                            fontSize = 11.sp,
                            color = RedAlert,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "الإجمالي بالدولار:", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        text = PosViewModel.formatUsd(uiState.cartTotalUsd),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "الإجمالي بالليرة:", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = PosViewModel.formatLbp(uiState.cartTotalLbp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (uiState.cartItems.isNotEmpty()) {
                        OutlinedIconButton(
                            onClick = { viewModel.holdCurrentSale() },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "تعليق")
                        }
                    }

                    Button(
                        onClick = { viewModel.openPaymentDialog() },
                        enabled = uiState.cartItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "دفع (${PosViewModel.formatUsd(uiState.cartTotalUsd)})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
