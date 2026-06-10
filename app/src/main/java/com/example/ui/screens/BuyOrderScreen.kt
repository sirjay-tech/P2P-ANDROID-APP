package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
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
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyOrderScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val coins = viewModel.supportedCoins
    val wallets = viewModel.supportedWallets

    var selectedCoin by remember { mutableStateOf(coins.first()) }
    var quantityText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var selectedWallet by remember { mutableStateOf(wallets.first()) }
    var notesText by remember { mutableStateOf("") }

    var coinDropdownExpanded by remember { mutableStateOf(false) }
    var walletDropdownExpanded by remember { mutableStateOf(false) }

    var validationErrorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Auto cost calculation logic ($Quantity * Price)
    val quantity = quantityText.toDoubleOrNull() ?: 0.0
    val price = priceText.toDoubleOrNull() ?: 0.0
    val totalCostUsd = quantity * price
    val totalCostLe = totalCostUsd * 22500.0 // USD_TO_LE exchange rate

    // Use current coin prices as reference helper
    val defaultPriceForCoin = viewModel.coinPrices[selectedCoin] ?: 1.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "EXECUTE BUY ORDER",
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
                text = "Enter a new trade acquisition to log in your offline inventory ledger. Synchronizes with server-side configurations.",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )

            // --- 1. Drop-down Crypto Coin Selection ---
            Column {
                Text(
                    text = "SELECT ASSET COIN",
                    style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold)
                )
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
                            .testTag("coin_select_dropdown"),
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
                                    // Autofill with mock market reference standard
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
                        modifier = Modifier.fillMaxWidth().testTag("buy_quantity_input"),
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
                        modifier = Modifier.fillMaxWidth().testTag("buy_price_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // --- 3. Wallet binding dropdown selection ---
            Column {
                Text(
                    text = "WALLET BINDING OUTLET",
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
                            .testTag("wallet_select_dropdown"),
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
                    placeholder = { Text("Enter trade notes, details, exchange counterparty...", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = CyberSlate,
                        unfocusedContainerColor = CyberSlate
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("buy_notes_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3
                )
            }

            // --- 5. Calculation Card ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberCardBg)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "ORDER COST CALCULATION CARDS",
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
                        Text("Gross Position Cost:", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                        Text(
                            text = "$${formatUsd(totalCostUsd)} USD",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Wallet Deduction:", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                        Text(
                            text = formatLeones(totalCostLe),
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reference spot price: $1 $selectedCoin = $defaultPriceForCoin USD",
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
                    modifier = Modifier.padding(vertical = 4.dp).testTag("validation_error_label")
                )
            }

            // Submit Button
            Button(
                onClick = {
                    if (quantity <= 0) {
                        validationErrorMessage = "Quantity must be greater than zero!"
                    } else if (price <= 0) {
                        validationErrorMessage = "Price must be greater than zero!"
                    } else {
                        viewModel.executeBuy(
                            coin = selectedCoin,
                            quantity = quantity,
                            price = price,
                            wallet = selectedWallet,
                            notes = notesText,
                            onSuccess = {
                                showSuccessDialog = true
                                // Clean the inputs
                                quantityText = ""
                                priceText = ""
                                notesText = ""
                                validationErrorMessage = null
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_buy_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = CyberMidnight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "COMMIT BUY ORDER ENTRY",
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
            title = { Text("Order Persisted!", color = CyberCyan) },
            text = { Text("The acquisition buy order has been successfully committed into SQLite Room cache database, triggering automatic allocation recalc.", color = TextPrimary) },
            containerColor = CyberSlate,
            textContentColor = TextPrimary,
            titleContentColor = CyberCyan
        )
    }
}
