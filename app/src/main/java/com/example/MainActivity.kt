package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.local.CompanyProfileManager
import com.example.data.repository.PosRepository
import com.example.ui.PosTab
import com.example.ui.PosViewModel
import com.example.ui.PosViewModelFactory
import com.example.ui.components.EditCompanyProfileDialog
import com.example.ui.components.ReceiptDialog
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                HajjiPosApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HajjiPosApp() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context.applicationContext) }
    val repository = remember { PosRepository(database.posDao()) }
    val companyProfileManager = remember { CompanyProfileManager(context.applicationContext) }
    val viewModel: PosViewModel = viewModel(factory = PosViewModelFactory(repository, companyProfileManager))

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by viewModel.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val heldSales by viewModel.heldSales.collectAsStateWithLifecycle(initialValue = emptyList())
    val customers by viewModel.customers.collectAsStateWithLifecycle(initialValue = emptyList())
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle(initialValue = emptyList())
    val shipments by viewModel.shipments.collectAsStateWithLifecycle(initialValue = emptyList())
    val expenses by viewModel.expenses.collectAsStateWithLifecycle(initialValue = emptyList())
    val sales by viewModel.sales.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeShift by viewModel.activeShift.collectAsStateWithLifecycle(initialValue = null)

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userFeedbackMessage) {
        uiState.userFeedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserFeedback()
        }
    }

    BackHandler(enabled = uiState.selectedTab != PosTab.POS) {
        viewModel.selectTab(PosTab.POS)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Galaxy Z Fold 5 unfolded portrait is ~673dp, landscape ~820-900dp, cover screen ~374dp
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            // Foldable (unfolded) / Tablet / POS Counter Layout with Navigation Rail
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Text(
                            text = "حجّي POS",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center
                    ) {
                        NavigationRailItem(
                            selected = uiState.selectedTab == PosTab.POS,
                            onClick = { viewModel.selectTab(PosTab.POS) },
                            icon = { Icon(Icons.Default.PointOfSale, contentDescription = "الكاشير") },
                            label = { Text("الكاشير", fontSize = 11.sp) }
                        )
                        NavigationRailItem(
                            selected = uiState.selectedTab == PosTab.INVENTORY,
                            onClick = { viewModel.selectTab(PosTab.INVENTORY) },
                            icon = { Icon(Icons.Default.Inventory2, contentDescription = "المخزون") },
                            label = { Text("المخزون", fontSize = 11.sp) }
                        )
                        NavigationRailItem(
                            selected = uiState.selectedTab == PosTab.PURCHASES,
                            onClick = { viewModel.selectTab(PosTab.PURCHASES) },
                            icon = { Icon(Icons.Default.LocalShipping, contentDescription = "المشتريات") },
                            label = { Text("المشتريات", fontSize = 11.sp) }
                        )
                        NavigationRailItem(
                            selected = uiState.selectedTab == PosTab.SHIFTS_EXPENSES,
                            onClick = { viewModel.selectTab(PosTab.SHIFTS_EXPENSES) },
                            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "الصندوق") },
                            label = { Text("الصندوق", fontSize = 11.sp) }
                        )
                        NavigationRailItem(
                            selected = uiState.selectedTab == PosTab.REPORTS || uiState.selectedTab == PosTab.CUSTOMERS || uiState.selectedTab == PosTab.SETTINGS,
                            onClick = { viewModel.selectTab(PosTab.REPORTS) },
                            icon = { Icon(Icons.Default.Assessment, contentDescription = "التقارير") },
                            label = { Text("التقارير", fontSize = 11.sp) }
                        )
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        ActiveScreenContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            products = products,
                            categories = categories,
                            heldSales = heldSales,
                            customers = customers,
                            suppliers = suppliers,
                            shipments = shipments,
                            expenses = expenses,
                            sales = sales,
                            activeShift = activeShift
                        )
                    }
                }
            }
        } else {
            // Mobile / Handheld View with Bottom Navigation Bar
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = uiState.selectedTab == PosTab.POS,
                            onClick = { viewModel.selectTab(PosTab.POS) },
                            icon = { Icon(Icons.Default.PointOfSale, contentDescription = "الكاشير") },
                            label = { Text("الكاشير", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = uiState.selectedTab == PosTab.INVENTORY,
                            onClick = { viewModel.selectTab(PosTab.INVENTORY) },
                            icon = { Icon(Icons.Default.Inventory2, contentDescription = "المخزون") },
                            label = { Text("المخزون", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = uiState.selectedTab == PosTab.PURCHASES,
                            onClick = { viewModel.selectTab(PosTab.PURCHASES) },
                            icon = { Icon(Icons.Default.LocalShipping, contentDescription = "المشتريات") },
                            label = { Text("المشتريات", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = uiState.selectedTab == PosTab.SHIFTS_EXPENSES,
                            onClick = { viewModel.selectTab(PosTab.SHIFTS_EXPENSES) },
                            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "الصندوق") },
                            label = { Text("الصندوق", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = uiState.selectedTab == PosTab.REPORTS || uiState.selectedTab == PosTab.CUSTOMERS || uiState.selectedTab == PosTab.SETTINGS,
                            onClick = { viewModel.selectTab(PosTab.REPORTS) },
                            icon = { Icon(Icons.Default.Assessment, contentDescription = "التقارير والإدارة") },
                            label = { Text("التقارير", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    ActiveScreenContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        products = products,
                        categories = categories,
                        heldSales = heldSales,
                        customers = customers,
                        suppliers = suppliers,
                        shipments = shipments,
                        expenses = expenses,
                        sales = sales,
                        activeShift = activeShift
                    )
                }
            }
        }
    }

    if (uiState.isReceiptDialogOpen) {
        ReceiptDialog(
            sale = uiState.lastCompletedSale,
            items = uiState.lastCompletedSaleItems,
            companyProfile = uiState.companyProfile,
            onDismiss = { viewModel.dismissReceiptDialog() },
            onPrint = { viewModel.testThermalPrint() }
        )
    }

    if (uiState.isEditCompanyProfileDialogOpen) {
        EditCompanyProfileDialog(
            currentProfile = uiState.companyProfile,
            onDismiss = { viewModel.dismissEditCompanyProfileDialog() },
            onSaveProfile = { newProfile -> viewModel.updateCompanyProfile(newProfile) },
            onPickLogoUri = { uri -> viewModel.updateCompanyLogoFromUri(uri) },
            onResetLogo = { viewModel.resetCompanyLogoToDefault() }
        )
    }
}

@Composable
private fun ActiveScreenContent(
    uiState: com.example.ui.PosUiState,
    viewModel: PosViewModel,
    products: List<com.example.data.model.ProductEntity>,
    categories: List<com.example.data.model.CategoryEntity>,
    heldSales: List<com.example.data.model.SaleEntity>,
    customers: List<com.example.data.model.CustomerEntity>,
    suppliers: List<com.example.data.model.SupplierEntity>,
    shipments: List<com.example.data.model.ImportShipmentEntity>,
    expenses: List<com.example.data.model.ExpenseEntity>,
    sales: List<com.example.data.model.SaleEntity>,
    activeShift: com.example.data.model.ShiftEntity?
) {
    when (uiState.selectedTab) {
        PosTab.POS -> PosScreen(
            viewModel = viewModel,
            uiState = uiState,
            products = products,
            categories = categories,
            heldSales = heldSales,
            customers = customers
        )
        PosTab.INVENTORY -> InventoryScreen(
            viewModel = viewModel,
            uiState = uiState,
            products = products
        )
        PosTab.PURCHASES -> PurchasesScreen(
            viewModel = viewModel,
            uiState = uiState,
            suppliers = suppliers,
            shipments = shipments
        )
        PosTab.CUSTOMERS -> CustomersScreen(
            viewModel = viewModel,
            uiState = uiState,
            customers = customers
        )
        PosTab.SHIFTS_EXPENSES -> ShiftsExpensesScreen(
            viewModel = viewModel,
            uiState = uiState,
            activeShift = activeShift,
            expenses = expenses
        )
        PosTab.REPORTS -> ReportsScreen(
            viewModel = viewModel,
            uiState = uiState,
            sales = sales,
            expenses = expenses,
            products = products,
            customers = customers
        )
        PosTab.SETTINGS -> SettingsScreen(
            viewModel = viewModel,
            uiState = uiState
        )
    }
}
