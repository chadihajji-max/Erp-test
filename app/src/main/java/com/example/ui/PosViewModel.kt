package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

enum class PosTab(val titleAr: String) {
    POS("نقطة البيع"),
    INVENTORY("المخزون"),
    PURCHASES("المشتريات والاستيراد"),
    CUSTOMERS("العملاء والديون"),
    SHIFTS_EXPENSES("الورديات والمصاريف"),
    REPORTS("التقارير المالية"),
    SETTINGS("الإعدادات والصرف")
}

data class PosUiState(
    val currentTab: PosTab = PosTab.POS,
    val exchangeRate: Double = 89500.0,
    val selectedCategory: String = "الكل",
    val searchQuery: String = "",
    val cartItems: List<CartItem> = emptyList(),
    val invoiceDiscountUsd: Double = 0.0,
    val selectedCustomer: CustomerEntity? = null,
    val currentCashier: String = "كاشير 1",
    val currentBranch: String = "الفرع الرئيسي",
    val userRole: String = "المدير", // المدير, الكاشير, أمين المخزون
    val isReceiptDialogOpen: Boolean = false,
    val lastCompletedSale: SaleEntity? = null,
    val lastCompletedSaleItems: List<SaleItemEntity> = emptyList(),
    val isHoldSalesDialogOpen: Boolean = false,
    val isPaymentDialogOpen: Boolean = false,
    val notificationMessage: String? = null,
    val printerConnected: Boolean = true,
    val scannerConnected: Boolean = true,
    val cashDrawerOpen: Boolean = false,
    val isSyncingEcommerce: Boolean = false,
    val ecommerceSyncStatus: String = "متصل بمتجر Hajji E-commerce"
) {
    val cartSubtotalUsd: Double
        get() = cartItems.sumOf { it.lineTotalUsd }

    val cartTotalUsd: Double
        get() = maxOf(0.0, cartSubtotalUsd - invoiceDiscountUsd)

    val cartTotalLbp: Long
        get() = (cartTotalUsd * exchangeRate).toLong()

    val cartItemCount: Int
        get() = cartItems.sumOf { it.quantity }

    val selectedTab: PosTab
        get() = currentTab

    val userFeedbackMessage: String?
        get() = notificationMessage
}

class PosViewModel(private val repository: PosRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    // Data streams from repository
    val products: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<ProductEntity>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customersWithDebt: StateFlow<List<CustomerEntity>> = repository.customersWithDebt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSales: StateFlow<List<SaleEntity>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<SaleEntity>>
        get() = allSales

    val heldSales: StateFlow<List<SaleEntity>> = repository.heldSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSaleItems: StateFlow<List<SaleItemEntity>> = repository.allSaleItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<SupplierEntity>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shipments: StateFlow<List<ImportShipmentEntity>> = repository.allShipments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shifts: StateFlow<List<ShiftEntity>> = repository.allShifts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeShift: StateFlow<ShiftEntity?> = repository.activeShift
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val exchangeRateHistory: StateFlow<List<ExchangeRateHistoryEntity>> = repository.allExchangeRates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockAudits: StateFlow<List<StockAuditEntity>> = repository.allStockAudits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Observe latest exchange rate from DB
        viewModelScope.launch {
            repository.latestExchangeRate.collect { rateEntity ->
                if (rateEntity != null) {
                    _uiState.update { it.copy(exchangeRate = rateEntity.rateLbp) }
                }
            }
        }
    }

    // --- Tab Navigation ---
    fun selectTab(tab: PosTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    // --- Search & Filters ---
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    // --- Cart Actions ---
    fun addToCart(product: ProductEntity, variant: ProductVariantEntity? = null) {
        val currentItems = _uiState.value.cartItems.toMutableList()
        val existingIndex = currentItems.indexOfFirst {
            it.product.id == product.id && it.selectedVariant?.id == variant?.id
        }

        if (existingIndex >= 0) {
            val existing = currentItems[existingIndex]
            currentItems[existingIndex] = existing.copy(quantity = existing.quantity + 1)
        } else {
            currentItems.add(CartItem(product = product, selectedVariant = variant, quantity = 1))
        }

        _uiState.update { it.copy(cartItems = currentItems) }
    }

    /**
     * مسح الباركود بالكاميرا وإضافته مباشرة إلى سلة المبيعات
     */
    fun scanBarcodeAndAddToCart(barcode: String, onResult: (ProductEntity?) -> Unit = {}) {
        viewModelScope.launch {
            val clean = barcode.trim()
            val prod = repository.getProductByBarcode(clean)
            if (prod != null) {
                addToCart(prod)
                _uiState.update {
                    it.copy(notificationMessage = "✅ تم مسح الباركود وإضافة: ${prod.name} إلى الفاتورة")
                }
            } else {
                _uiState.update {
                    it.copy(notificationMessage = "⚠️ الباركود $clean غير موجود بقاعدة البيانات")
                }
            }
            onResult(prod)
        }
    }

    fun updateCartQuantity(item: CartItem, delta: Int) {
        val currentItems = _uiState.value.cartItems.toMutableList()
        val index = currentItems.indexOfFirst {
            it.product.id == item.product.id && it.selectedVariant?.id == item.selectedVariant?.id
        }
        if (index >= 0) {
            val newQty = currentItems[index].quantity + delta
            if (newQty <= 0) {
                currentItems.removeAt(index)
            } else {
                currentItems[index] = currentItems[index].copy(quantity = newQty)
            }
            _uiState.update { it.copy(cartItems = currentItems) }
        }
    }

    fun removeFromCart(item: CartItem) {
        val currentItems = _uiState.value.cartItems.toMutableList()
        currentItems.removeAll {
            it.product.id == item.product.id && it.selectedVariant?.id == item.selectedVariant?.id
        }
        _uiState.update { it.copy(cartItems = currentItems) }
    }

    fun setItemDiscount(item: CartItem, discountUsd: Double) {
        val currentItems = _uiState.value.cartItems.toMutableList()
        val index = currentItems.indexOfFirst {
            it.product.id == item.product.id && it.selectedVariant?.id == item.selectedVariant?.id
        }
        if (index >= 0) {
            currentItems[index] = currentItems[index].copy(customDiscountUsd = discountUsd)
            _uiState.update { it.copy(cartItems = currentItems) }
        }
    }

    fun clearCart() {
        _uiState.update {
            it.copy(
                cartItems = emptyList(),
                invoiceDiscountUsd = 0.0,
                selectedCustomer = null
            )
        }
    }

    fun setInvoiceDiscount(discountUsd: Double) {
        _uiState.update { it.copy(invoiceDiscountUsd = discountUsd) }
    }

    fun selectCustomer(customer: CustomerEntity?) {
        _uiState.update { it.copy(selectedCustomer = customer) }
    }

    // --- Exchange Rate Updates ---
    fun updateExchangeRate(newRate: Double, notes: String = "تحديث يدوي لسعر الصرف اليومي") {
        viewModelScope.launch {
            repository.updateExchangeRate(newRate, _uiState.value.currentCashier, notes)
            _uiState.update {
                it.copy(
                    exchangeRate = newRate,
                    notificationMessage = "تم تحديث سعر الصرف إلى ${formatLbp(newRate.toLong())} للدولار الواحد"
                )
            }
        }
    }

    // --- Checkout & Payment ---
    fun openPaymentDialog() {
        if (_uiState.value.cartItems.isNotEmpty()) {
            _uiState.update { it.copy(isPaymentDialogOpen = true) }
        }
    }

    fun dismissPaymentDialog() {
        _uiState.update { it.copy(isPaymentDialogOpen = false) }
    }

    fun completeCheckout(
        paymentMethod: String,
        paidUsd: Double,
        paidLbp: Long,
        changeUsd: Double,
        changeLbp: Long,
        notes: String = ""
    ) {
        val state = _uiState.value
        if (state.cartItems.isEmpty()) return

        viewModelScope.launch {
            val sale = repository.processSale(
                cartItems = state.cartItems,
                exchangeRate = state.exchangeRate,
                discountUsd = state.invoiceDiscountUsd,
                paymentMethod = paymentMethod,
                paidUsd = paidUsd,
                paidLbp = paidLbp,
                changeUsd = changeUsd,
                changeLbp = changeLbp,
                cashierName = state.currentCashier,
                branchName = state.currentBranch,
                customer = state.selectedCustomer,
                notes = notes
            )

            val saleItems = repository.getItemsForSale(sale.id)

            _uiState.update {
                it.copy(
                    cartItems = emptyList(),
                    invoiceDiscountUsd = 0.0,
                    selectedCustomer = null,
                    isPaymentDialogOpen = false,
                    lastCompletedSale = sale,
                    lastCompletedSaleItems = saleItems,
                    isReceiptDialogOpen = true,
                    notificationMessage = "تم إصدار الفاتورة رقم ${sale.invoiceNumber} بنجاح!"
                )
            }
        }
    }

    fun dismissReceiptDialog() {
        _uiState.update { it.copy(isReceiptDialogOpen = false) }
    }

    fun showReceiptForSale(sale: SaleEntity) {
        viewModelScope.launch {
            val items = repository.getItemsForSale(sale.id)
            _uiState.update {
                it.copy(
                    lastCompletedSale = sale,
                    lastCompletedSaleItems = items,
                    isReceiptDialogOpen = true
                )
            }
        }
    }

    // --- Hold / Resume Sale ---
    fun holdCurrentSale() {
        val state = _uiState.value
        if (state.cartItems.isEmpty()) return

        viewModelScope.launch {
            repository.holdSale(
                cartItems = state.cartItems,
                exchangeRate = state.exchangeRate,
                cashierName = state.currentCashier,
                branchName = state.currentBranch,
                customer = state.selectedCustomer
            )
            _uiState.update {
                it.copy(
                    cartItems = emptyList(),
                    invoiceDiscountUsd = 0.0,
                    selectedCustomer = null,
                    notificationMessage = "تم تعليق الفاتورة بنجاح. يمكنك استرجاعها في أي وقت."
                )
            }
        }
    }

    fun openHoldSalesDialog() {
        _uiState.update { it.copy(isHoldSalesDialogOpen = true) }
    }

    fun dismissHoldSalesDialog() {
        _uiState.update { it.copy(isHoldSalesDialogOpen = false) }
    }

    fun resumeHeldSale(sale: SaleEntity) {
        viewModelScope.launch {
            val items = repository.getItemsForSale(sale.id)
            val cartList = mutableListOf<CartItem>()

            for (saleItem in items) {
                val prod = repository.getProductById(saleItem.productId)
                if (prod != null) {
                    val variant = if (saleItem.variantName.isNotBlank()) {
                        repository.getVariantsList(prod.id).firstOrNull { it.variantName == saleItem.variantName }
                    } else null

                    cartList.add(
                        CartItem(
                            product = prod,
                            selectedVariant = variant,
                            quantity = saleItem.quantity,
                            customDiscountUsd = saleItem.discountUsd
                        )
                    )
                }
            }

            // Remove held sale from DB
            repository.deleteSale(sale.id)

            val customer = if (sale.customerId != null) {
                repository.allCustomers.first().firstOrNull { it.id == sale.customerId }
            } else null

            _uiState.update {
                it.copy(
                    cartItems = cartList,
                    invoiceDiscountUsd = sale.discountUsd,
                    selectedCustomer = customer,
                    isHoldSalesDialogOpen = false,
                    notificationMessage = "تم استرجاع الفاتورة المعلقة بنجاح"
                )
            }
        }
    }

    suspend fun getVariantsForProduct(productId: Long): List<ProductVariantEntity> {
        return repository.getVariantsList(productId)
    }

    // --- Return & Refund Sale ---
    fun returnSale(sale: SaleEntity, reason: String) {
        viewModelScope.launch {
            repository.processReturn(sale, reason)
            _uiState.update {
                it.copy(notificationMessage = "تم إرجاع الفاتورة ${sale.invoiceNumber} وإعادة البضاعة للمخزن.")
            }
        }
    }

    // --- Product & Inventory Management ---
    fun addProduct(
        name: String,
        category: String,
        barcode: String,
        costPriceUsd: Double,
        sellingPriceUsd: Double,
        stockQuantity: Int,
        minStockLevel: Int,
        unit: String,
        hasVariants: Boolean = false,
        variantsList: List<Pair<String, Int>> = emptyList() // Variant name to initial stock
    ) {
        viewModelScope.launch {
            val newProd = ProductEntity(
                name = name,
                category = category,
                barcode = if (barcode.isNotBlank()) barcode else "BC${System.currentTimeMillis() % 10000000}",
                costPriceUsd = costPriceUsd,
                sellingPriceUsd = sellingPriceUsd,
                stockQuantity = stockQuantity,
                minStockLevel = minStockLevel,
                unit = unit,
                hasVariants = hasVariants,
                variantsDescription = variantsList.joinToString(", ") { it.first }
            )
            val id = repository.insertProduct(newProd)

            variantsList.forEachIndexed { index, (variantName, vStock) ->
                repository.insertVariant(
                    ProductVariantEntity(
                        productId = id,
                        variantName = variantName,
                        sku = "SKU-$id-$index",
                        costPriceUsd = costPriceUsd,
                        sellingPriceUsd = sellingPriceUsd,
                        stockQuantity = vStock
                    )
                )
            }

            _uiState.update {
                it.copy(notificationMessage = "تمت إضافة المنتج بنجاح: $name")
            }
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateProduct(product)
            _uiState.update {
                it.copy(notificationMessage = "تم تعديل بيانات المنتج: ${product.name}")
            }
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch {
            repository.deleteProduct(id)
            _uiState.update {
                it.copy(notificationMessage = "تم حذف المنتج بنجاح")
            }
        }
    }

    fun recordStockAudit(product: ProductEntity, actualQuantity: Int, reason: String) {
        viewModelScope.launch {
            repository.recordStockAudit(
                productName = product.name,
                expected = product.stockQuantity,
                actual = actualQuantity,
                reason = reason,
                performedBy = _uiState.value.userRole
            )
            repository.updateStock(product.id, actualQuantity)
            _uiState.update {
                it.copy(notificationMessage = "تمت تسوية الجرد للمنتج ${product.name} إلى $actualQuantity ${product.unit}")
            }
        }
    }

    fun transferStock(product: ProductEntity, toBranch: String, quantity: Int) {
        if (product.stockQuantity < quantity) return
        viewModelScope.launch {
            repository.updateStock(product.id, product.stockQuantity - quantity)
            _uiState.update {
                it.copy(notificationMessage = "تم تحويل $quantity ${product.unit} من ${product.name} إلى $toBranch")
            }
        }
    }

    /**
     * يفحص المخزون الحالي ويرسل تنبيهاً في حال وصول أي منتج إلى الحد الأدنى المحدد أو نفاده.
     */
    fun checkInventoryLevelsAndAlert() {
        viewModelScope.launch {
            val allCurrentProducts = repository.allProducts.first()
            val lowStockItems = allCurrentProducts.filter { it.stockQuantity <= it.minStockLevel }
            if (lowStockItems.isNotEmpty()) {
                val details = lowStockItems.take(3).joinToString("، ") {
                    "${it.name} (المتبقي: ${it.stockQuantity} / حد الأمان: ${it.minStockLevel})"
                }
                val extra = lowStockItems.size - 3
                val message = if (extra > 0) {
                    "⚠️ تنبيه نقص مخزون: $details بالإضافة إلى $extra منتجات أخرى بلغت الحد الأدنى!"
                } else {
                    "⚠️ تنبيه نقص مخزون: $details بلغت الحد الأدنى المحدد أو نفدت!"
                }
                _uiState.update { it.copy(notificationMessage = message) }
            } else {
                _uiState.update {
                    it.copy(notificationMessage = "✅ فحص المخزون: جميع المنتجات متوفرة بكميات آمنة وفوق الحد الأدنى.")
                }
            }
        }
    }

    fun checkLowStockAndAlert() = checkInventoryLevelsAndAlert()

    // --- Customers & Debts ---
    fun addCustomer(name: String, phone: String, address: String, notes: String) {
        viewModelScope.launch {
            repository.insertCustomer(
                CustomerEntity(name = name, phone = phone, address = address, notes = notes)
            )
            _uiState.update {
                it.copy(notificationMessage = "تمت إضافة العميل $name بنجاح")
            }
        }
    }

    fun settleCustomerDebt(customer: CustomerEntity, amountUsd: Double) {
        viewModelScope.launch {
            repository.settleCustomerDebt(customer.id, amountUsd)
            _uiState.update {
                it.copy(notificationMessage = "تم تسديد مبلغ $$amountUsd من دين العميل ${customer.name}")
            }
        }
    }

    // --- Purchases & Import Landed Cost Calculation ---
    fun addSupplier(name: String, company: String, country: String, phone: String, email: String, notes: String) {
        viewModelScope.launch {
            repository.insertSupplier(
                SupplierEntity(name = name, company = company, country = country, phone = phone, email = email, notes = notes)
            )
            _uiState.update {
                it.copy(notificationMessage = "تمت إضافة المورد $name ($company)")
            }
        }
    }

    fun addImportShipment(
        trackingNumber: String,
        supplierName: String,
        description: String,
        weightKg: Double,
        shippingCostUsd: Double,
        customsBeirutPortUsd: Double,
        portClearanceUsd: Double,
        goodsCostUsd: Double,
        itemCount: Int,
        notes: String
    ) {
        val totalLandedCost = shippingCostUsd + customsBeirutPortUsd + portClearanceUsd + goodsCostUsd
        viewModelScope.launch {
            repository.insertShipment(
                ImportShipmentEntity(
                    trackingNumber = trackingNumber,
                    supplierName = supplierName,
                    description = description,
                    weightKg = weightKg,
                    shippingCostUsd = shippingCostUsd,
                    customsBeirutPortUsd = customsBeirutPortUsd,
                    portClearanceUsd = portClearanceUsd,
                    goodsCostUsd = goodsCostUsd,
                    totalLandedCostUsd = totalLandedCost,
                    itemCount = itemCount,
                    status = "ARRIVED_BEIRUT_PORT",
                    notes = notes
                )
            )
            _uiState.update {
                it.copy(notificationMessage = "تم تسجيل الشحنة $trackingNumber بتكلفة إجمالية واصلة $$totalLandedCost")
            }
        }
    }

    // --- Expenses ---
    fun addExpense(title: String, category: String, amountUsd: Double, notes: String) {
        viewModelScope.launch {
            val rate = _uiState.value.exchangeRate
            val amountLbp = (amountUsd * rate).toLong()
            repository.insertExpense(
                ExpenseEntity(
                    title = title,
                    category = category,
                    amountUsd = amountUsd,
                    amountLbp = amountLbp,
                    exchangeRate = rate,
                    cashierName = _uiState.value.currentCashier,
                    branchName = _uiState.value.currentBranch,
                    notes = notes
                )
            )
            _uiState.update {
                it.copy(notificationMessage = "تم تسجيل المصروف: $title بمبلغ $$amountUsd")
            }
        }
    }

    // --- Shifts & Cash Drawer ---
    fun openShift(openingUsd: Double, openingLbp: Long, notes: String) {
        viewModelScope.launch {
            repository.insertShift(
                ShiftEntity(
                    cashierName = _uiState.value.currentCashier,
                    branchName = _uiState.value.currentBranch,
                    openingCashUsd = openingUsd,
                    openingCashLbp = openingLbp,
                    status = "OPEN",
                    notes = notes
                )
            )
            _uiState.update {
                it.copy(notificationMessage = "تم فتح وردية جديدة للكاشير ${_uiState.value.currentCashier}")
            }
        }
    }

    fun closeShift(closingUsd: Double, closingLbp: Long, notes: String) {
        val shift = activeShift.value ?: return
        viewModelScope.launch {
            val expectedUsd = shift.openingCashUsd + shift.totalSalesUsd
            val diffUsd = closingUsd - expectedUsd

            val updated = shift.copy(
                endTime = System.currentTimeMillis(),
                closingCashUsd = closingUsd,
                closingCashLbp = closingLbp,
                cashDrawerDifferenceUsd = diffUsd,
                status = "CLOSED",
                notes = notes
            )
            repository.updateShift(updated)
            _uiState.update {
                it.copy(notificationMessage = "تم إغلاق الوردية. فارق الصندوق: $$diffUsd")
            }
        }
    }

    fun triggerCashDrawer() {
        _uiState.update { it.copy(cashDrawerOpen = true, notificationMessage = "تم فتح درج النقود (Cash Drawer)") }
    }

    fun testThermalPrint() {
        _uiState.update { it.copy(notificationMessage = "جارٍ إرسال أمر الطباعة عبر البلوتوث إلى طابعة الفواتير...") }
    }

    fun syncEcommerce() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingEcommerce = true) }
            kotlinx.coroutines.delay(1200)
            _uiState.update {
                it.copy(
                    isSyncingEcommerce = false,
                    ecommerceSyncStatus = "تمت المزامنة بنجاح مع Hajji E-commerce!",
                    notificationMessage = "تم تحديث مستويات المخزون والأسعار على متجر الأونلاين"
                )
            }
        }
    }

    fun switchBranch(branch: String) {
        _uiState.update { it.copy(currentBranch = branch) }
    }

    fun switchRole(role: String) {
        _uiState.update { it.copy(userRole = role) }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }

    fun clearUserFeedback() = dismissNotification()

    fun showUserFeedback(msg: String) {
        _uiState.update { it.copy(notificationMessage = msg) }
    }

    fun syncWithEcommerce() = syncEcommerce()

    fun updateBranchAndCashier(branch: String, cashier: String) {
        _uiState.update { it.copy(currentBranch = branch, currentCashier = cashier) }
    }

    // Currency formatters
    companion object {
        fun formatUsd(amount: Double): String {
            return String.format(Locale.US, "$%.2f", amount)
        }

        fun formatLbp(amount: Long): String {
            val formatter = NumberFormat.getNumberInstance(Locale.US)
            return "${formatter.format(amount)} ل.ل"
        }
    }
}

class PosViewModelFactory(private val repository: PosRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PosViewModel::class.java)) {
            return PosViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

