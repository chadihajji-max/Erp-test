package com.example.ui.components

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import com.example.data.model.CompanyProfile
import com.example.data.model.SaleEntity
import com.example.data.model.SaleItemEntity
import com.example.ui.PosViewModel
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object BluetoothThermalPrinterManager {
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return try {
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice): Boolean {
        return try {
            disconnect()
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            bluetoothSocket = socket
            outputStream = socket.outputStream
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (_: Exception) {}
        outputStream = null
        bluetoothSocket = null
    }

    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true && outputStream != null
    }

    fun printReceipt(
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        companyProfile: CompanyProfile
    ): Boolean {
        if (!isConnected() || outputStream == null) return false

        try {
            val out = outputStream ?: return false

            // ESC/POS Commands
            val INIT = byteArrayOf(0x1B, 0x40)
            val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
            val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
            val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
            val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
            val CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x41, 0x10)

            out.write(INIT)

            // Header - Center Aligned
            out.write(ALIGN_CENTER)
            out.write(BOLD_ON)
            out.write("${companyProfile.companyName}\n".toByteArray(charset("GB2312")))
            out.write(BOLD_OFF)
            out.write("${companyProfile.address}\n".toByteArray(charset("GB2312")))
            out.write("Tel: ${companyProfile.phone}\n".toByteArray(charset("GB2312")))
            if (companyProfile.taxNumber.isNotBlank()) {
                out.write("Tax No: ${companyProfile.taxNumber}\n".toByteArray(charset("GB2312")))
            }
            out.write("--------------------------------\n".toByteArray())

            // Invoice Info - Left Aligned
            out.write(ALIGN_LEFT)
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(sale.timestamp))
            out.write("Invoice #: ${sale.invoiceNumber}\n".toByteArray())
            out.write("Date: $dateStr\n".toByteArray())
            if (!sale.customerName.isNullOrBlank()) {
                out.write("Customer: ${sale.customerName}\n".toByteArray(charset("GB2312")))
            }
            out.write("Cashier: ${sale.cashierName}\n".toByteArray(charset("GB2312")))
            out.write("--------------------------------\n".toByteArray())

            // Items Table Header
            out.write(BOLD_ON)
            out.write("Item          Qty   Price   Total\n".toByteArray())
            out.write(BOLD_OFF)
            out.write("--------------------------------\n".toByteArray())

            for (item in items) {
                val name = item.productName.take(12).padEnd(12)
                val qty = item.quantity.toString().padStart(3)
                val price = String.format(Locale.US, "%.1f", item.unitPriceUsd).padStart(6)
                val total = String.format(Locale.US, "%.1f", item.totalPriceUsd).padStart(7)
                out.write("$name $qty $price $total\n".toByteArray(charset("GB2312")))
            }

            out.write("--------------------------------\n".toByteArray())

            // Totals
            if (sale.discountUsd > 0) {
                out.write("Subtotal: $${String.format(Locale.US, "%.2f", sale.subtotalUsd)}\n".toByteArray())
                out.write("Discount: $${String.format(Locale.US, "%.2f", sale.discountUsd)}\n".toByteArray())
            }

            out.write(BOLD_ON)
            out.write("TOTAL USD: $${String.format(Locale.US, "%.2f", sale.totalUsd)}\n".toByteArray())
            out.write("TOTAL LBP: ${PosViewModel.formatLbp(sale.totalLbp)}\n".toByteArray())
            out.write(BOLD_OFF)

            out.write("Payment: ${sale.paymentMethod}\n".toByteArray())
            out.write("--------------------------------\n".toByteArray())

            // Footer - Center
            out.write(ALIGN_CENTER)
            out.write("${companyProfile.receiptFooterNote}\n".toByteArray(charset("GB2312")))
            out.write("Thank you for your visit!\n\n\n".toByteArray())

            // Cut paper
            out.write(CUT_PAPER)
            out.flush()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
