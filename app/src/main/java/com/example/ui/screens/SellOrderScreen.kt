package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellOrderScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val coins = viewModel.supportedCoins
    val wallets = viewModel.supportedWallets
    val items by viewModel.items.collectAsStateWithLifecycle()

    var selectedCoin by remember { mutableStateOf(coins.first()) }
    var quantityText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var selectedWallet by remember { mutableStateOf(wallets.first()) }
    var notesText by remember { mutableStateOf("") }

    var coinDropdownExpanded by remember { mutableStateOf(false) }
    var walletDropdownExpanded by remember { mutableStateOf(false) }

    var validationErrorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Calculate available volume from existing transactions
    val currentBalances = remember(items) {
        viewModel.calculateCoinBalances(items)
    }
    val availableVolume = currentBalances[selectedCoin] ?: 0.0

    // Compute average unit cost for selectedCoin.
    // Standard weighted average cost of remaining units.
    val averageCost = remember(items, selectedCoin) {
        val history = mutableListOf<Pair<Double, Double>>()
        items.sortedBy { it.id }.forEach { item ->
            if (item.coin == selectedCoin) {
                if (item.type == "BUY") {
                    history.add(Pair(item.quantity, item.price))
                } else if (item.type == "SELL") {
                    var sellQty = item.quantity
                    while (sellQty > 0 && history.isNotEmpty()) {
                        val h = history.first()
                        if (h.first <= sellQty) {
                            sellQty -= h.first
                            history.removeAt(0)
                        } else {
                            history[0] = Pair(h.first - sellQty, h.second)
                            sellQty = 0.0
                        }
                    }
                }
            }
        }
        val totalQty = history.sumOf { it.first }
        val totalCost = history.sumOf { it.first * it.second }
        if (totalQty > 0) totalCost / totalQty else 0.0
    }

    val quantity = quantityText.toDoubleOrNull() ?: 0.0
    val price = priceText.toDoubleOrNull() ?: 0.0

    // Live calculations
    val grossInflowUsd = quantity * price
    val grossInflowLe = grossInflowUsd * 22500.0

    // Realized Profit: Qty sold * (Sell Price - Avg Cost)
    val profitUsd = if (quantity > 0.0) {
        quantity * (price - averageCost)
    } else {
        0.0
    }
    val profitLe = profitUsd * 22500.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "EXECUTE SELL ORDER",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            letterSpacing = 1.5.sp
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberSlate),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = BorderColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(0.dp)
                )
            )
        },
        containerColor = CyberMidnight,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Liquidate assets from transaction history. Validates against available volumes in offline SQLite Room cache.",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )

            // --- 1. Drop-down Crypto Coin Selection ---
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SELECT ASSET COIN",
                        style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold)
                    )
                    // Available volume indicator block
                    Box(
                        modifier = Modifier
                            .background(CyberCyan.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .border(BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "HOLDINGS: ${formatDouble(availableVolume)} $selectedCoin",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = coinDropdownExpanded,
                    onExpandedChange = { coinDropdownExpanded = !coinDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCoin,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = CyberCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CyberSlate,
                            unfocusedContainerColor = CyberSlate
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("sell_coin_select_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = coinDropdownExpanded,
                        onDismissRequest = { coinDropdownExpanded = false },
                        modifier = Modifier.border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp))
                    ) {
                        coins.forEach { coin ->
                            DropdownMenuItem(
                                text = { Text(coin, color = TextPrimary) },
                                onClick = {
                                    selectedCoin = coin
                                    priceText = (viewModel.coinPrices[coin] ?: 1.0).toString()
                                    coinDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // --- 2. Numeric Input Fields with Validation Constraints ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "QUANTITY",
                        style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = {
                            if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) {
                                quantityText = it
                                validationErrorMessage = null
                            }
                        },
                        placeholder = { Text("0.00", color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CyberSlate,
                            unfocusedContainerColor = CyberSlate
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("sell_quantity_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PRICE (USD / UNIT)",
                        style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = {
                            if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) {
                                priceText = it
                                validationErrorMessage = null
                            }
                        },
                        placeholder = { Text("0.00", color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CyberSlate,
                            unfocusedContainerColor = CyberSlate
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("sell_price_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // --- 3. Wallet binding dropdown selection ---
            Column {
                Text(
                    text = "WALLET RECEIVING OUTLET",
                    style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = walletDropdownExpanded,
                    onExpandedChange = { walletDropdownExpanded = !walletDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedWallet,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = CyberCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CyberSlate,
                            unfocusedContainerColor = CyberSlate
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("sell_wallet_select_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = walletDropdownExpanded,
                        onDismissRequest = { walletDropdownExpanded = false },
                        modifier = Modifier.border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp))
                    ) {
                        wallets.forEach { wallet ->
                            DropdownMenuItem(
                                text = { Text(wallet, color = TextPrimary) },
                                onClick = {
                                    selectedWallet = wallet
                                    walletDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // --- 4. Notes Text Field ---
            Column {
                Text(
                    text = "INTERNAL TRANSACTIONAL NOTES",
                    style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    placeholder = { Text("Include trade notes, counterparty reference ID standard...", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = CyberSlate,
                        unfocusedContainerColor = CyberSlate
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("sell_notes_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3
                )
            }

            // --- 5. Calculation Card: Gross Cash Inflows and Realized Profits ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberCardBg)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "TRADE LIQUIDATION PROJECTIONS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gross Cash Inflow:", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                        Text(
                            text = "$${formatUsd(grossInflowUsd)} USD",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Wallet Receipt Cash:", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                        Text(
                            text = formatLeones(grossInflowLe),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = CyberAmber,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = BorderColor)
                    Spacer(modifier = Modifier.height(10.dp))

                    val profitTextColor = if (profitLe >= 0.0) CyberEmerald else Color.Red.copy(alpha = 0.8f)
                    val labelSignStr = if (profitLe >= 0.0) "Net Realized Profit:" else "Net Realized Loss:"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(labelSignStr, style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                        Text(
                            text = formatLeones(profitLe),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = profitTextColor,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Weighted avg purchase cost: $${formatUsd(averageCost)} USD",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }
            }

            // Error Display Label
            if (validationErrorMessage != null) {
                Text(
                    text = validationErrorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp).testTag("sell_validation_error_label")
                )
            }

            // Submit Button
            Button(
                onClick = {
                    if (quantity <= 0) {
                        validationErrorMessage = "Quantity must be greater than zero!"
                    } else if (price <= 0) {
                        validationErrorMessage = "Price must be greater than zero!"
                    } else if (quantity > availableVolume) {
                        validationErrorMessage = "Holds constraint failure: Only $availableVolume $selectedCoin are available."
                    } else {
                        viewModel.executeSell(
                            coin = selectedCoin,
                            quantity = quantity,
                            price = price,
                            wallet = selectedWallet,
                            notes = notesText,
                            onSuccess = {
                                showSuccessDialog = true
                                quantityText = ""
                                priceText = ""
                                notesText = ""
                                validationErrorMessage = null
                            },
                            onFailure = { err ->
                                validationErrorMessage = err
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_sell_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberAmber),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = CyberMidnight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "COMMIT SELL ORDER ENTRY",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CyberMidnight,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("OK", color = CyberCyan)
                }
            },
            title = { Text("Liquidation Saved!", color = CyberCyan) },
            text = { Text("The liquidation trade order has been successfully committed into SQLite Room cache database, triggering automatic profit and value recalculation.", color = TextPrimary) },
            containerColor = CyberSlate,
            textContentColor = TextPrimary,
            titleContentColor = CyberCyan
        )
    }
}
