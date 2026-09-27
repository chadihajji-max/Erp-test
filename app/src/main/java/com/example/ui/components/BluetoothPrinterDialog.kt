package com.example.ui.components

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat

@Composable
fun BluetoothPrinterDialog(
    onDismiss: () -> Unit,
    onPrintTest: () -> Unit = {}
) {
    val context = LocalContext.current
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var isConnected by remember { mutableStateOf(BluetoothThermalPrinterManager.isConnected()) }
    var statusMessage by remember { mutableStateOf(if (isConnected) "✅ متصل بالطابعة بنجاح" else "اختر طابعة بلوتوث حرارية للاتصال") }
    var isLoading by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        pairedDevices = BluetoothThermalPrinterManager.getPairedPrinters()
    }

    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
        }

        val hasAll = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (hasAll) {
            pairedDevices = BluetoothThermalPrinterManager.getPairedPrinters()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إعداد طابعة الفواتير (بلوتوث)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "الأجهزة المقترنة (Bluetooth):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (pairedDevices.isEmpty()) {
                    Text(
                        text = "لا توجد أجهزة بلوتوث مقترنة. يجدر بك إقتران طابعة حرارية (مثل POS-58 / POS-80) من إعدادات الهاتف أولاً.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(pairedDevices) { device ->
                            val isThisConnected = selectedDevice?.address == device.address && isConnected
                            OutlinedCard(
                                onClick = {
                                    selectedDevice = device
                                    isLoading = true
                                    statusMessage = "جاري الاتصال بـ ${device.name ?: device.address}..."
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        val success = BluetoothThermalPrinterManager.connect(device)
                                        isConnected = success
                                        isLoading = false
                                        statusMessage = if (success) "✅ تم الاتصال بنجاح بـ ${device.name}" else "❌ فشل الاتصال بالطابعة"
                                    }, 200)
                                },
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (isThisConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = device.name ?: "طابعة غير معروفة",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = device.address,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isThisConnected) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text("متصل", color = MaterialTheme.colorScheme.onPrimary, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isConnected) {
                        Button(
                            onClick = {
                                onPrintTest()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة تجريبية", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            BluetoothThermalPrinterManager.disconnect()
                            isConnected = false
                            statusMessage = "تم قطع الاتصال."
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("قطع الاتصال", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
