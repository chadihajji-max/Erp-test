package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Product Entity
 * Base currency is USD ($)
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val barcode: String,
    val costPriceUsd: Double,
    val sellingPriceUsd: Double,
    val stockQuantity: Int,
    val minStockLevel: Int = 5,
    val unit: String = "قطعة", // قطعة, كغ, علبة, طرد
    val branchName: String = "الفرع الرئيسي",
    val hasVariants: Boolean = false,
    val variantsDescription: String = "", // e.g., "S, M, L, XL"
    val iconName: String = "box"
)

/**
 * Product Variant (for color/size/model)
 */
@Entity(tableName = "product_variants")
data class ProductVariantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val variantName: String, // e.g., "أزرق / L"
    val sku: String,
    val costPriceUsd: Double,
    val sellingPriceUsd: Double,
    val stockQuantity: Int
)

/**
 * Category Entity
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "category"
)

/**
 * Customer Entity
 */
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String = "",
    val creditBalanceUsd: Double = 0.0, // Debt owed by customer
    val loyaltyPoints: Int = 0,
    val notes: String = ""
)

/**
 * Sale / Invoice Entity
 * Every sale locks in the exchange rate at the moment of creation.
 */
@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val cashierName: String = "كاشير 1",
    val branchName: String = "الفرع الرئيسي",
    val exchangeRateAtSale: Double, // e.g. 89500.0
    val subtotalUsd: Double,
    val discountUsd: Double = 0.0,
    val totalUsd: Double,
    val totalLbp: Long,
    val paidUsd: Double = 0.0,
    val paidLbp: Long = 0L,
    val changeUsd: Double = 0.0,
    val changeLbp: Long = 0L,
    val paymentMethod: String = "CASH_USD", // CASH_USD, CASH_LBP, SPLIT_USD_LBP, CARD, CREDIT
    val customerId: Long? = null,
    val customerName: String? = null,
    val status: String = "COMPLETED", // COMPLETED, HELD, RETURNED
    val isReturned: Boolean = false,
    val returnReason: String = "",
    val notes: String = ""
)

/**
 * Individual items in a sale
 */
@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val variantName: String = "",
    val quantity: Int,
    val unitPriceUsd: Double,
    val costPriceUsd: Double,
    val discountUsd: Double = 0.0,
    val totalPriceUsd: Double
)

/**
 * Supplier Entity (Alibaba / China / Local)
 */
@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val company: String = "Alibaba / 1688",
    val country: String = "الصين",
    val phone: String = "",
    val email: String = "",
    val notes: String = ""
)

/**
 * Import Shipment Entity (Weight, Sea/Air freight, Beirut Port customs fees)
 */
@Entity(tableName = "import_shipments")
data class ImportShipmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackingNumber: String,
    val supplierName: String,
    val description: String,
    val date: Long = System.currentTimeMillis(),
    val weightKg: Double,
    val shippingCostUsd: Double, // الشحن البحري أو الجوي
    val customsBeirutPortUsd: Double, // رسوم جمرك مرفأ بيروت
    val portClearanceUsd: Double, // رسوم تخليص ومرفأ
    val goodsCostUsd: Double, // تكلفة البضاعة الأصلية
    val totalLandedCostUsd: Double, // إجمالي تكلفة الاستيراد الواصل
    val itemCount: Int,
    val status: String = "ARRIVED_BEIRUT_PORT", // ORDERED, IN_TRANSIT, ARRIVED_BEIRUT_PORT, RECEIVED
    val notes: String = ""
)

/**
 * Operational Expenses Entity
 */
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // إيجار, رواتب, اشتراك مولد/كهرباء, شحن وتوصيل, صيانة, بضاعة, عهدة
    val amountUsd: Double,
    val amountLbp: Long,
    val exchangeRate: Double,
    val date: Long = System.currentTimeMillis(),
    val cashierName: String = "المدير",
    val branchName: String = "الفرع الرئيسي",
    val notes: String = ""
)

/**
 * Cashier Shift & Petty Cash
 */
@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cashierName: String,
    val branchName: String,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val openingCashUsd: Double = 0.0,
    val openingCashLbp: Long = 0L,
    val closingCashUsd: Double = 0.0,
    val closingCashLbp: Long = 0L,
    val totalSalesUsd: Double = 0.0,
    val totalSalesLbp: Long = 0L,
    val cashDrawerDifferenceUsd: Double = 0.0,
    val status: String = "OPEN", // OPEN, CLOSED
    val notes: String = ""
)

/**
 * Exchange Rate History (USD to LBP daily rate)
 */
@Entity(tableName = "exchange_rates")
data class ExchangeRateHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rateLbp: Double, // e.g. 89500.0
    val timestamp: Long = System.currentTimeMillis(),
    val updatedBy: String = "مدير النظام",
    val notes: String = "سعر الصرف اليومي"
)

/**
 * Stock Audit / Take Entry
 */
@Entity(tableName = "stock_audits")
data class StockAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val productName: String,
    val expectedQuantity: Int,
    val actualQuantity: Int,
    val difference: Int,
    val reason: String = "تسوية جرد دوري",
    val performedBy: String = "أمين المخزون"
)

/**
 * In-memory Cart Item for active sale
 */
data class CartItem(
    val product: ProductEntity,
    val selectedVariant: ProductVariantEntity? = null,
    var quantity: Int = 1,
    var customDiscountUsd: Double = 0.0
) {
    val unitPriceUsd: Double
        get() = selectedVariant?.sellingPriceUsd ?: product.sellingPriceUsd

    val costPriceUsd: Double
        get() = selectedVariant?.costPriceUsd ?: product.costPriceUsd

    val lineTotalUsd: Double
        get() = maxOf(0.0, (unitPriceUsd * quantity) - customDiscountUsd)
}
