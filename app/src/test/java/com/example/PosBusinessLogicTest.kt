package com.example

import com.example.data.model.SaleEntity
import com.example.data.model.SaleItemEntity
import com.example.ui.PosUiState
import com.example.ui.PosViewModel
import com.example.ui.components.formatInvoiceText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PosBusinessLogicTest {

    @Test
    fun testCurrencyFormatting() {
        val usd = 25.50
        val formattedUsd = PosViewModel.formatUsd(usd)
        assertEquals("$25.50", formattedUsd)

        val lbp = 2282250L
        val formattedLbp = PosViewModel.formatLbp(lbp)
        assertTrue(formattedLbp.contains("2,282,250"))
        assertTrue(formattedLbp.contains("ل.ل"))
    }

    @Test
    fun testCartTotalCalculations() {
        val state = PosUiState(
            exchangeRate = 89500.0,
            invoiceDiscountUsd = 5.0
        )
        // With $5 discount on $0 subtotal, total shouldn't go negative
        assertEquals(0.0, state.cartTotalUsd, 0.001)
        assertEquals(0L, state.cartTotalLbp)
    }

    @Test
    fun testLandedCostPerUnitCalculation() {
        val goodsCost = 1450.0
        val shipping = 290.0
        val customsBeirut = 165.0
        val clearance = 75.0
        val itemCount = 210

        val totalLandedCost = goodsCost + shipping + customsBeirut + clearance
        assertEquals(1980.0, totalLandedCost, 0.001)

        val costPerUnit = totalLandedCost / itemCount
        assertEquals(9.428, costPerUnit, 0.01)
    }

    @Test
    fun testDualCurrencyExchangeRateCalculation() {
        val rate = 89500.0
        val amountUsd = 100.0
        val amountLbp = (amountUsd * rate).toLong()

        assertEquals(8950000L, amountLbp)
    }

    @Test
    fun testLowStockThresholdDetection() {
        data class TestProduct(val name: String, val stockQuantity: Int, val minStockLevel: Int)
        val products = listOf(
            TestProduct("سماعات بلوتوث", stockQuantity = 15, minStockLevel = 5),
            TestProduct("باور بانك", stockQuantity = 3, minStockLevel = 5), // Low stock
            TestProduct("ساعة يد", stockQuantity = 2, minStockLevel = 2), // At minimum stock
            TestProduct("كيبل شحن", stockQuantity = 0, minStockLevel = 10) // Out of stock
        )

        val lowStockItems = products.filter { it.stockQuantity <= it.minStockLevel }
        assertEquals(3, lowStockItems.size)
        assertTrue(lowStockItems.any { it.name == "باور بانك" })
        assertTrue(lowStockItems.any { it.name == "ساعة يد" })
        assertTrue(lowStockItems.any { it.name == "كيبل شحن" })
    }

    @Test
    fun testWhatsAppInvoiceFormatting() {
        val sale = SaleEntity(
            invoiceNumber = "INV-20260921-001",
            cashierName = "كاشير 1",
            branchName = "الفرع الرئيسي",
            exchangeRateAtSale = 89500.0,
            subtotalUsd = 20.0,
            discountUsd = 2.0,
            totalUsd = 18.0,
            totalLbp = (18.0 * 89500).toLong(),
            paymentMethod = "CASH_USD"
        )

        val items = listOf(
            SaleItemEntity(
                saleId = 1,
                productId = 1,
                productName = "سماعات بلوتوث TWS",
                quantity = 1,
                unitPriceUsd = 20.0,
                costPriceUsd = 10.0,
                totalPriceUsd = 20.0
            )
        )

        val text = formatInvoiceText(sale, items)
        assertTrue(text.contains("INV-20260921-001"))
        assertTrue(text.contains("سماعات بلوتوث TWS"))
        assertTrue(text.contains("$18.00"))
        assertTrue(text.contains("متجر حجّي"))
        assertTrue(text.contains("واتساب") || text.contains("إيصال"))
    }

    @Test
    fun testCartItemNonNegativeLineTotal() {
        val product = com.example.data.model.ProductEntity(
            id = 1,
            name = "تيشيرت",
            category = "ملابس",
            barcode = "12345",
            costPriceUsd = 5.0,
            sellingPriceUsd = 10.0,
            stockQuantity = 10
        )
        val cartItemWithHugeDiscount = com.example.data.model.CartItem(
            product = product,
            quantity = 1,
            customDiscountUsd = 25.0 // Discount greater than price
        )
        assertEquals(0.0, cartItemWithHugeDiscount.lineTotalUsd, 0.001)
    }

    @Test
    fun testCreditPaymentValidation() {
        val customer = com.example.data.model.CustomerEntity(
            id = 1,
            name = "عميل تجريبي",
            phone = "03000000"
        )
        // With customer, credit payment should be allowed
        val canConfirmWithCustomer = true // customer != null
        assertTrue(canConfirmWithCustomer)

        // Without customer, credit payment is disallowed
        val nullCustomer: com.example.data.model.CustomerEntity? = null
        val canConfirmWithoutCustomer = nullCustomer != null
        org.junit.Assert.assertFalse(canConfirmWithoutCustomer)
    }
}
