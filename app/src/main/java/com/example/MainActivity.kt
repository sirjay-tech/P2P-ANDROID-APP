package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.InventoryDatabase
import com.example.data.repository.InventoryRepository
import com.example.network.api.InventoryApiService
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.BuyOrderScreen
import com.example.ui.screens.SellOrderScreen
import com.example.ui.screens.StockLedgerScreen
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {

    // Lazy initialization of Database & Network Repository dependencies
    private val database by lazy { InventoryDatabase.getDatabase(applicationContext) }
    private val apiService by lazy { InventoryApiService.create() }
    private val repository by lazy { InventoryRepository(database.inventoryDao(), apiService) }

    // Initialize MainViewModel with manual factory injection
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(application, repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppEngine(viewModel = mainViewModel)
            }
        }
    }
}

@Composable
fun MainAppEngine(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showUrlConfigDialog by remember { mutableStateOf(false) }
    var inputUrlText by remember { mutableStateOf("") }
    val currentUrl by viewModel.backendUrl.collectAsState()

    // Navigation Tabs List
    val navigationTabs = listOf(
        NavigationTabItem(
            route = "dashboard",
            label = "Dashboard",
            icon = Icons.Default.Home,
            testTag = "nav_item_dashboard"
        ),
        NavigationTabItem(
            route = "buy",
            label = "Buy",
            icon = Icons.Default.ShoppingCart,
            testTag = "nav_item_buy"
        ),
        NavigationTabItem(
            route = "sell",
            label = "Sell",
            icon = Icons.Default.Send,
            testTag = "nav_item_sell"
        ),
        NavigationTabItem(
            route = "stock",
            label = "Stock",
            icon = Icons.Default.List,
            testTag = "nav_item_stock"
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column {
                NavigationBar(
                    containerColor = CyberMidnight,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = BorderColor,
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                        .testTag("app_navigation_bar")
                ) {
                    navigationTabs.forEach { tab ->
                        val isSelected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberCyan,
                                selectedTextColor = CyberCyan,
                                indicatorColor = CyberCyan.copy(alpha = 0.15f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }

                    // Floating setup for Apps Script Configuration URL
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            inputUrlText = currentUrl
                            showUrlConfigDialog = true
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Config URL"
                            )
                        },
                        label = {
                            Text(
                                text = "Config",
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_item_config")
                    )
                }
            }
        },
        containerColor = CyberMidnight
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(viewModel = viewModel)
            }
            composable("buy") {
                BuyOrderScreen(viewModel = viewModel)
            }
            composable("sell") {
                SellOrderScreen(viewModel = viewModel)
            }
            composable("stock") {
                StockLedgerScreen(viewModel = viewModel)
            }
        }
    }

    if (showUrlConfigDialog) {
        AlertDialog(
            onDismissRequest = { showUrlConfigDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateUrl(inputUrlText)
                        showUrlConfigDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("Apply Target API", color = CyberMidnight)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlConfigDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            title = {
                Text(
                    text = "TARGET BACKEND API CONFIGURE",
                    color = CyberCyan,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Set your deployed Google Apps Script custom macro Web App URL below:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    OutlinedTextField(
                        value = inputUrlText,
                        onValueChange = { inputUrlText = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CyberSlate,
                            unfocusedContainerColor = CyberSlate
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("config_url_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            containerColor = CyberSlate,
            textContentColor = TextPrimary,
            titleContentColor = CyberCyan
        )
    }
}

data class NavigationTabItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
)
