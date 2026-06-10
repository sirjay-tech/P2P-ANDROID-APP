package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockLedgerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    var searchTradeQuery by remember { mutableStateOf("") }

    // Aggregate holdings from full transaction history
    // We want to calculate detailed metrics for each coin:
    // Coin -> Quantity, Average Cost, Value
    val ledgerHoldings = remember(items) {
        val holdingDetails = mutableMapOf<String, HoldingDetail>()
        val history = mutableMapOf<String, MutableList<Pair<Double, Double>>>()

        // Pass 1: calculate holdings with FIFO/weighted cost tracking
        items.sortedBy { it.id }.forEach { item ->
            val list = history.getOrPut(item.coin) { mutableListOf() }
            if (item.type == "BUY") {
                list.add(Pair(item.quantity, item.price))
            } else if (item.type == "SELL") {
                var sellQty = item.quantity
                while (sellQty > 0 && list.isNotEmpty()) {
                    val head = list.first()
                    if (head.first <= sellQty) {
                        sellQty -= head.first
                        list.removeAt(0)
                    } else {
                        list[0] = Pair(head.first - sellQty, head.second)
                        sellQty = 0.0
                    }
                }
            }
        }

        // Pass 2: build details from remaining units
        history.forEach { (coin, remainingUnits) ->
            val totalRemainingQty = remainingUnits.sumOf { it.first }
            val totalRemainingCost = remainingUnits.sumOf { it.first * it.second }
            val avgCostUsd = if (totalRemainingQty > 0) totalRemainingCost / totalRemainingQty else 0.0
            val latestPriceUsd = viewModel.coinPrices[coin] ?: 1.0

            if (totalRemainingQty > 0.0001) {
                holdingDetails[coin] = HoldingDetail(
                    coin = coin,
                    quantity = totalRemainingQty,
                    averageUnitCostUsd = avgCostUsd,
                    latestPriceUsd = latestPriceUsd,
                    totalValueUsd = totalRemainingQty * latestPriceUsd,
                    totalValueLe = totalRemainingQty * latestPriceUsd * 22500.0 // USD_TO_LE rate
                )
            }
        }
        holdingDetails.values.toList().sortedByDescending { it.totalValueUsd }
    }

    // Filter by query
    val filteredHoldings = remember(ledgerHoldings, searchTradeQuery) {
        if (searchTradeQuery.isEmpty()) {
            ledgerHoldings
        } else {
            ledgerHoldings.filter {
                it.coin.contains(searchTradeQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ACTIVE STOCK LEDGER",
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Real-time cost basis matrix. Aggregates outstanding positions and computes net asset valuation in Leones.",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )

            // --- Real-time Search Box ---
            OutlinedTextField(
                value = searchTradeQuery,
                onValueChange = { searchTradeQuery = it },
                placeholder = { Text("Search asset by token symbol...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CyberSlate,
                    unfocusedContainerColor = CyberSlate
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ledger_search_field"),
                shape = RoundedCornerShape(12.dp)
            )

            if (filteredHoldings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchTradeQuery.isNotEmpty()) "No matching holdings found." else "Your ledger is totally empty.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredHoldings, key = { it.coin }) { holding ->
                        LedgerHoldingCard(holding)
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LedgerHoldingCard(
    holding: HoldingDetail
) {
    val coinColor = remember(holding.coin) {
        when (holding.coin.uppercase()) {
            "BTC" -> CyberAmber
            "ETH" -> CyberCyan
            "USDT", "USDC" -> CyberEmerald
            else -> CyberCyan
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color(0x0DFFFFFF)), RoundedCornerShape(24.dp))
            .testTag("ledger_card_${holding.coin}"),
        colors = CardDefaults.cardColors(containerColor = CyberSlate.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Coin Symbol + Percentage Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(coinColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, coinColor.copy(alpha = 0.3f)), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = holding.coin.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = coinColor,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = holding.coin,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Text(
                    text = "Spot Price: $${formatUsd(holding.latestPriceUsd)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = coinColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(14.dp))

            // Stats Matrix Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "STOCK VOLUME STATS",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDouble(holding.quantity),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "AVERAGE UNIT COST",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$${formatUsd(holding.averageUnitCostUsd)} USD",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Value Row Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberCardBg, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "OUTSTANDING VALUE (USD)",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                    Text(
                        text = "$${formatUsd(holding.totalValueUsd)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "OUTSTANDING (LEONES)",
                        style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan)
                    )
                    Text(
                        text = formatLeones(holding.totalValueLe),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = CyberCyan,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }
}

data class HoldingDetail(
    val coin: String,
    val quantity: Double,
    val averageUnitCostUsd: Double,
    val latestPriceUsd: Double,
    val totalValueUsd: Double,
    val totalValueLe: Double
)
