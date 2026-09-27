package com.example.data.repository

import com.example.data.local.PosDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PosRepository(private val dao: PosDao) {

    // --- Products ---
    val allProducts: Flow<List<ProductEntity>> = dao.getAllProducts()
    val lowStockProducts: Flow<List<ProductEntity>> = dao.getLowStockProducts()
    val allCategories: Flow<List<CategoryEntity>> = dao.getAllCategories()

    suspend fun getProductByBarcode(barcode: String): ProductEntity? = dao.getProductByBarcode(barcode)
    suspend fun getProductById(id: Long): ProductEntity? = dao.getProductById(id)
    suspend fun insertProduct(product: ProductEntity): Long = dao.insertProduct(product)
    suspend fun updateProduct(product: ProductEntity) = dao.updateProduct(product)
    suspend fun deleteProduct(id: Long) = dao.deleteProduct(id)
    suspend fun updateStock(productId: Long, newStock: Int) = dao.setStock(productId, newStock)

    // Variants
    fun getVariantsForProduct(productId: Long): Flow<List<ProductVariantEntity>> = dao.getVariantsForProduct(productId)
    suspend fun getVariantsList(productId: Long): List<ProductVariantEntity> = dao.getVariantsList(productId)
    suspend fun insertVariant(variant: ProductVariantEntity): Long = dao.insertVariant(variant)

    // Categories
    suspend fun insertCategory(category: CategoryEntity): Long = dao.insertCategory(category)

    // --- Customers ---
    val allCustomers: Flow<List<CustomerEntity>> = dao.getAllCustomers()
    val customersWithDebt: Flow<List<CustomerEntity>> = dao.getCustomersWithDebt()

    suspend fun insertCustomer(customer: CustomerEntity): Long = dao.insertCustomer(customer)
    suspend fun updateCustomer(customer: CustomerEntity) = dao.updateCustomer(customer)
    suspend fun settleCustomerDebt(customerId: Long, amountUsd: Double) = dao.settleCustomerDebt(customerId, amountUsd)

    // --- Sales ---
    val allSales: Flow<List<SaleEntity>> = dao.getAllSales()
    val heldSales: Flow<List<SaleEntity>> = dao.getHeldSales()
    val allSaleItems: Flow<List<SaleItemEntity>> = dao.getAllSaleItems()

    suspend fun getSaleById(id: Long): SaleEntity? = dao.getSaleById(id)
    suspend fun getItemsForSale(saleId: Long): List<SaleItemEntity> = dao.getItemsForSale(saleId)

    // --- Suppliers & Imports ---
    val allSuppliers: Flow<List<SupplierEntity>> = dao.getAllSuppliers()
    val allShipments: Flow<List<ImportShipmentEntity>> = dao.getAllShipments()

    suspend fun insertSupplier(supplier: SupplierEntity): Long = dao.insertSupplier(supplier)
    suspend fun insertShipment(shipment: ImportShipmentEntity): Long = dao.insertShipment(shipment)
    suspend fun updateShipment(shipment: ImportShipmentEntity) = dao.updateShipment(shipment)

    // --- Expenses ---
    val allExpenses: Flow<List<ExpenseEntity>> = dao.getAllExpenses()
    suspend fun insertExpense(expense: ExpenseEntity): Long = dao.insertExpense(expense)

    // --- Shifts ---
    val allShifts: Flow<List<ShiftEntity>> = dao.getAllShifts()
    val activeShift: Flow<ShiftEntity?> = dao.getActiveShift()
    suspend fun getActiveShiftSync(): ShiftEntity? = dao.getActiveShiftSync()
    suspend fun insertShift(shift: ShiftEntity): Long = dao.insertShift(shift)
    suspend fun updateShift(shift: ShiftEntity) = dao.updateShift(shift)

    // --- Exchange Rates ---
    val allExchangeRates: Flow<List<ExchangeRateHistoryEntity>> = dao.getAllExchangeRates()
    val latestExchangeRate: Flow<ExchangeRateHistoryEntity?> = dao.getLatestExchangeRate()
    suspend fun getLatestExchangeRateSync(): ExchangeRateHistoryEntity? = dao.getLatestExchangeRateSync()
    suspend fun updateExchangeRate(newRate: Double, updatedBy: String, notes: String): Long {
        return dao.insertExchangeRate(
            ExchangeRateHistoryEntity(
                rateLbp = newRate,
                timestamp = System.currentTimeMillis(),
                updatedBy = updatedBy,
                notes = notes
            )
        )
    }

    // --- Stock Audits ---
    val allStockAudits: Flow<List<StockAuditEntity>> = dao.getAllStockAudits()
    suspend fun recordStockAudit(productName: String, expected: Int, actual: Int, reason: String, performedBy: String): Long {
        return dao.insertStockAudit(
            StockAuditEntity(
                productName = productName,
                expectedQuantity = expected,
                actualQuantity = actual,
                difference = actual - expected,
                reason = reason,
                performedBy = performedBy
            )
        )
    }

    // --- Checkout & Complete Sale ---
    suspend fun processSale(
        cartItems: List<CartItem>,
        exchangeRate: Double,
        discountUsd: Double,
        paymentMethod: String,
        paidUsd: Double,
        paidLbp: Long,
        changeUsd: Double,
        changeLbp: Long,
        cashierName: String,
        branchName: String,
        customer: CustomerEntity?,
        notes: String = ""
    ): SaleEntity {
        val subtotalUsd = cartItems.sumOf { it.lineTotalUsd }
        val finalTotalUsd = maxOf(0.0, subtotalUsd - discountUsd)
        val finalTotalLbp = (finalTotalUsd * exchangeRate).toLong()

        val timeFormat = SimpleDateFormat("yyMMddHHmmss", Locale.US)
        val invoiceNumber = "INV-" + timeFormat.format(Date())

        val saleEntity = SaleEntity(
            invoiceNumber = invoiceNumber,
            timestamp = System.currentTimeMillis(),
            cashierName = cashierName,
            branchName = branchName,
            exchangeRateAtSale = exchangeRate,
            subtotalUsd = subtotalUsd,
            discountUsd = discountUsd,
            totalUsd = finalTotalUsd,
            totalLbp = finalTotalLbp,
            paidUsd = paidUsd,
            paidLbp = paidLbp,
            changeUsd = changeUsd,
            changeLbp = changeLbp,
            paymentMethod = paymentMethod,
            customerId = customer?.id,
            customerName = customer?.name,
            status = "COMPLETED",
            notes = notes
        )

        val saleId = dao.insertSale(saleEntity)

        // Insert items & deduct inventory
        val saleItems = cartItems.map { item ->
            // Deduct stock
            dao.deductStock(item.product.id, item.quantity)
            if (item.selectedVariant != null) {
                dao.deductVariantStock(item.selectedVariant.id, item.quantity)
            }

            SaleItemEntity(
                saleId = saleId,
                productId = item.product.id,
                productName = item.product.name,
                variantName = item.selectedVariant?.variantName ?: "",
                quantity = item.quantity,
                unitPriceUsd = item.unitPriceUsd,
                costPriceUsd = item.costPriceUsd,
                discountUsd = item.customDiscountUsd,
                totalPriceUsd = item.lineTotalUsd
            )
        }
        dao.insertSaleItems(saleItems)

        // Customer Debt or Loyalty handling
        if (customer != null) {
            if (paymentMethod == "CREDIT") {
                dao.addCustomerDebt(customer.id, finalTotalUsd)
            }
            // Add loyalty points: 1 point per $1 spent
            val pointsEarned = finalTotalUsd.toInt()
            if (pointsEarned > 0) {
                dao.addLoyaltyPoints(customer.id, pointsEarned)
            }
        }

        // Update active shift if any
        val currentShift = dao.getActiveShiftSync()
        if (currentShift != null) {
            val updatedShift = currentShift.copy(
                totalSalesUsd = currentShift.totalSalesUsd + finalTotalUsd,
                totalSalesLbp = currentShift.totalSalesLbp + finalTotalLbp
            )
            dao.updateShift(updatedShift)
        }

        return saleEntity.copy(id = saleId)
    }

    // Hold Sale
    suspend fun holdSale(
        cartItems: List<CartItem>,
        exchangeRate: Double,
        cashierName: String,
        branchName: String,
        customer: CustomerEntity?
    ): Long {
        val subtotalUsd = cartItems.sumOf { it.lineTotalUsd }
        val totalLbp = (subtotalUsd * exchangeRate).toLong()
        val timeFormat = SimpleDateFormat("yyMMddHHmmss", Locale.US)
        val invoiceNumber = "HOLD-" + timeFormat.format(Date())

        val sale = SaleEntity(
            invoiceNumber = invoiceNumber,
            exchangeRateAtSale = exchangeRate,
            subtotalUsd = subtotalUsd,
            totalUsd = subtotalUsd,
            totalLbp = totalLbp,
            cashierName = cashierName,
            branchName = branchName,
            customerId = customer?.id,
            customerName = customer?.name,
            status = "HELD"
        )
        val saleId = dao.insertSale(sale)
        val items = cartItems.map { item ->
            SaleItemEntity(
                saleId = saleId,
                productId = item.product.id,
                productName = item.product.name,
                variantName = item.selectedVariant?.variantName ?: "",
                quantity = item.quantity,
                unitPriceUsd = item.unitPriceUsd,
                costPriceUsd = item.costPriceUsd,
                discountUsd = item.customDiscountUsd,
                totalPriceUsd = item.lineTotalUsd
            )
        }
        dao.insertSaleItems(items)
        return saleId
    }

    // Delete held sale when resumed
    suspend fun deleteSale(id: Long) = dao.deleteSale(id)

    // Process Return / Refund
    suspend fun processReturn(sale: SaleEntity, reason: String) {
        val items = dao.getItemsForSale(sale.id)
        // Restore inventory
        items.forEach { item ->
            dao.addStock(item.productId, item.quantity)
            if (item.variantName.isNotBlank()) {
                val variants = dao.getVariantsList(item.productId)
                val matchingVariant = variants.firstOrNull { it.variantName == item.variantName }
                if (matchingVariant != null) {
                    dao.deductVariantStock(matchingVariant.id, -item.quantity)
                }
            }
        }
        val updatedSale = sale.copy(
            isReturned = true,
            returnReason = reason,
            status = "RETURNED"
        )
        dao.updateSale(updatedSale)
        // If credit was used, reduce customer debt
        if (sale.paymentMethod == "CREDIT" && sale.customerId != null) {
            dao.settleCustomerDebt(sale.customerId, sale.totalUsd)
        }
    }

    // Expense Management
    suspend fun updateExpense(expense: ExpenseEntity) = dao.updateExpense(expense)
    suspend fun deleteExpense(expense: ExpenseEntity) = dao.deleteExpense(expense)
    suspend fun deleteExpenseById(id: Long) = dao.deleteExpenseById(id)

    // Invoice / Sale Management
    suspend fun updateSale(sale: SaleEntity) = dao.updateSale(sale)

    suspend fun deleteSaleCompletely(saleId: Long, restoreStock: Boolean = true) {
        if (restoreStock) {
            val items = dao.getItemsForSale(saleId)
            items.forEach { item ->
                dao.addStock(item.productId, item.quantity)
            }
        }
        dao.deleteSaleItemsBySaleId(saleId)
        dao.deleteSale(saleId)
    }
}
