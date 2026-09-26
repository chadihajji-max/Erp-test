package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        ProductVariantEntity::class,
        CategoryEntity::class,
        CustomerEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        SupplierEntity::class,
        ImportShipmentEntity::class,
        ExpenseEntity::class,
        ShiftEntity::class,
        ExchangeRateHistoryEntity::class,
        StockAuditEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun posDao(): PosDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hajji_pos_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val scope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hajji_pos_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.posDao())
                    }
                }
            }

            suspend fun populateInitialData(dao: PosDao) {
                // Initial Lebanese Exchange Rate (1 USD = 89,500 LBP)
                dao.insertExchangeRate(
                    ExchangeRateHistoryEntity(
                        rateLbp = 89500.0,
                        timestamp = System.currentTimeMillis() - 86400000,
                        updatedBy = "مدير النظام",
                        notes = "سعر الصرف الافتتاحي الرسمي"
                    )
                )
                dao.insertExchangeRate(
                    ExchangeRateHistoryEntity(
                        rateLbp = 89500.0,
                        timestamp = System.currentTimeMillis(),
                        updatedBy = "الكاشير العام",
                        notes = "سعر الصرف اليومي المعتمد في المتجر"
                    )
                )

                // Categories
                dao.insertCategory(CategoryEntity(name = "الكل", icon = "apps"))
                dao.insertCategory(CategoryEntity(name = "إلكترونيات", icon = "devices"))
                dao.insertCategory(CategoryEntity(name = "ملابس وأحذية", icon = "checkroom"))
                dao.insertCategory(CategoryEntity(name = "عطور وتجميل", icon = "spa"))
                dao.insertCategory(CategoryEntity(name = "إكسسوارات وهدايا", icon = "watch"))
                dao.insertCategory(CategoryEntity(name = "أدوات منزلية", icon = "home"))

                // Initial Products with USD base pricing & cost
                val p1Id = dao.insertProduct(
                    ProductEntity(
                        name = "سماعات بلوتوث لاسلكية TWS Pro",
                        category = "إلكترونيات",
                        barcode = "6901234567891",
                        costPriceUsd = 6.50,
                        sellingPriceUsd = 15.00,
                        stockQuantity = 24,
                        minStockLevel = 5,
                        unit = "قطعة",
                        hasVariants = false,
                        iconName = "headphones"
                    )
                )

                val p2Id = dao.insertProduct(
                    ProductEntity(
                        name = "كيبل شحن سريع Type-C إلى Lightning",
                        category = "إلكترونيات",
                        barcode = "6901234567892",
                        costPriceUsd = 1.20,
                        sellingPriceUsd = 4.00,
                        stockQuantity = 40,
                        minStockLevel = 10,
                        unit = "قطعة",
                        hasVariants = false,
                        iconName = "cable"
                    )
                )

                val p3Id = dao.insertProduct(
                    ProductEntity(
                        name = "تيشيرت قطني رجالي فاخر",
                        category = "ملابس وأحذية",
                        barcode = "6901234567893",
                        costPriceUsd = 4.50,
                        sellingPriceUsd = 12.00,
                        stockQuantity = 18,
                        minStockLevel = 6,
                        unit = "قطعة",
                        hasVariants = true,
                        variantsDescription = "أسود/L, أبيض/XL, كحلي/M",
                        iconName = "checkroom"
                    )
                )
                // Add variants for product 3
                dao.insertVariant(
                    ProductVariantEntity(
                        productId = p3Id,
                        variantName = "أسود / L",
                        sku = "TSH-BLK-L",
                        costPriceUsd = 4.50,
                        sellingPriceUsd = 12.00,
                        stockQuantity = 8
                    )
                )
                dao.insertVariant(
                    ProductVariantEntity(
                        productId = p3Id,
                        variantName = "أبيض / XL",
                        sku = "TSH-WHT-XL",
                        costPriceUsd = 4.50,
                        sellingPriceUsd = 12.00,
                        stockQuantity = 5
                    )
                )
                dao.insertVariant(
                    ProductVariantEntity(
                        productId = p3Id,
                        variantName = "كحلي / M",
                        sku = "TSH-NVY-M",
                        costPriceUsd = 4.50,
                        sellingPriceUsd = 12.00,
                        stockQuantity = 5
                    )
                )

                val p4Id = dao.insertProduct(
                    ProductEntity(
                        name = "باور بانك 20000mAh شحن سريع 22.5W",
                        category = "إلكترونيات",
                        barcode = "6901234567894",
                        costPriceUsd = 11.00,
                        sellingPriceUsd = 25.00,
                        stockQuantity = 3, // Low stock alert!
                        minStockLevel = 5,
                        unit = "قطعة",
                        hasVariants = false,
                        iconName = "battery_charging_full"
                    )
                )

                val p5Id = dao.insertProduct(
                    ProductEntity(
                        name = "عطر فرنسي كلاسيك 100 مل",
                        category = "عطور وتجميل",
                        barcode = "6901234567895",
                        costPriceUsd = 14.00,
                        sellingPriceUsd = 35.00,
                        stockQuantity = 12,
                        minStockLevel = 4,
                        unit = "علبة",
                        hasVariants = false,
                        iconName = "spa"
                    )
                )

                val p6Id = dao.insertProduct(
                    ProductEntity(
                        name = "ساعة يد ذكية مقاومة للماء Smart Watch",
                        category = "إكسسوارات وهدايا",
                        barcode = "6901234567896",
                        costPriceUsd = 12.50,
                        sellingPriceUsd = 28.00,
                        stockQuantity = 2, // Low stock alert!
                        minStockLevel = 4,
                        unit = "قطعة",
                        hasVariants = false,
                        iconName = "watch"
                    )
                )

                val p7Id = dao.insertProduct(
                    ProductEntity(
                        name = "حذاء رياضي مريح Running Shoes",
                        category = "ملابس وأحذية",
                        barcode = "6901234567897",
                        costPriceUsd = 13.00,
                        sellingPriceUsd = 30.00,
                        stockQuantity = 9,
                        minStockLevel = 3,
                        unit = "زوج",
                        hasVariants = false,
                        iconName = "snowshoeing"
                    )
                )

                // Customers with Debt & Loyalty
                dao.insertCustomer(
                    CustomerEntity(
                        name = "أحمد السيد",
                        phone = "03 123 456",
                        address = "بيروت - الحمرا",
                        creditBalanceUsd = 35.00,
                        loyaltyPoints = 120,
                        notes = "زبون دائم - يفضل الدفع عند نهاية الشهر"
                    )
                )
                dao.insertCustomer(
                    CustomerEntity(
                        name = "مريم خليل",
                        phone = "71 987 654",
                        address = "صيدا - شارع المصارف",
                        creditBalanceUsd = 0.0,
                        loyaltyPoints = 250,
                        notes = "زبونة VIP"
                    )
                )
                dao.insertCustomer(
                    CustomerEntity(
                        name = "عمر طرابلسي",
                        phone = "70 554 433",
                        address = "طرابلس - الميناء",
                        creditBalanceUsd = 70.00,
                        loyaltyPoints = 45,
                        notes = "حساب دين مؤجل"
                    )
                )

                // Suppliers (China / Alibaba / Local)
                dao.insertSupplier(
                    SupplierEntity(
                        name = "Shenzhen Electronics Tech Co.",
                        company = "Alibaba Supplier (Gold Member)",
                        country = "الصين - شنتشن",
                        phone = "+86 755 88990011",
                        email = "sales@shenzhen-tech.cn",
                        notes = "مورد رئيسي للسماعات وكوابل الشحن والشواحن"
                    )
                )
                dao.insertSupplier(
                    SupplierEntity(
                        name = "Yiwu Commodities International",
                        company = "1688 / Yiwu Market",
                        country = "الصين - إيوو",
                        phone = "+86 579 85552233",
                        email = "export@yiwu-market.cn",
                        notes = "إكسسوارات وهدايا وساعات وملابس"
                    )
                )
                dao.insertSupplier(
                    SupplierEntity(
                        name = "شركة الشرق للتوزيع",
                        company = "مورد محلي معتمد",
                        country = "لبنان - بيروت",
                        phone = "01 884422",
                        email = "info@sharq-lb.com",
                        notes = "توريد محلي فوري بالدولار"
                    )
                )

                // Import Shipments (Landed cost calculation with Beirut Port customs)
                dao.insertShipment(
                    ImportShipmentEntity(
                        trackingNumber = "CN-BEY-2026-0901",
                        supplierName = "Shenzhen Electronics Tech Co.",
                        description = "دفعة سماعات TWS وباور بانك وكوابل شحن سريعة",
                        weightKg = 48.5,
                        shippingCostUsd = 290.0, // تكلفة الشحن الجوي/البحري
                        customsBeirutPortUsd = 165.0, // رسوم جمارك مرفأ بيروت
                        portClearanceUsd = 75.0, // رسوم تخليص وأرضيات
                        goodsCostUsd = 1450.0,
                        totalLandedCostUsd = 1980.0,
                        itemCount = 210,
                        status = "ARRIVED_BEIRUT_PORT",
                        notes = "وصلت حاوية البضائع إلى مرفأ بيروت - بانتظار استكمال التخليص الجمركي"
                    )
                )

                // Operating Expenses (Lebanon realistic items: generator subscription, salaries, rent...)
                dao.insertExpense(
                    ExpenseEntity(
                        title = "اشتراك مولد الكهرباء الشهري",
                        category = "كهرباء ومولد",
                        amountUsd = 180.0,
                        amountLbp = (180.0 * 89500).toLong(),
                        exchangeRate = 89500.0,
                        cashierName = "المدير",
                        branchName = "الفرع الرئيسي",
                        notes = "5 أمبير لإنارة المتجر وتشغيل الأجهزة"
                    )
                )
                dao.insertExpense(
                    ExpenseEntity(
                        title = "شراء رول ورق حراري لطابعة الفواتير",
                        category = "شحن وتوصيل ومصاريف",
                        amountUsd = 15.0,
                        amountLbp = (15.0 * 89500).toLong(),
                        exchangeRate = 89500.0,
                        cashierName = "كاشير 1",
                        branchName = "الفرع الرئيسي",
                        notes = "صندوق ورق 80mm"
                    )
                )

                // Initial Open Shift for Cashier
                dao.insertShift(
                    ShiftEntity(
                        cashierName = "كاشير 1",
                        branchName = "الفرع الرئيسي",
                        openingCashUsd = 150.0,
                        openingCashLbp = 4500000L, // 4.5 Million LBP opening float
                        status = "OPEN",
                        notes = "وردية صباحية"
                    )
                )
            }
        }
    }
}
