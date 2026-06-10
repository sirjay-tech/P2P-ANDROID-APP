package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.InventoryItem
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.SyncState
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val orangeMoneyLe by viewModel.orangeMoneyBalance.collectAsStateWithLifecycle()
    val totalAssetLe by viewModel.totalAssetValueLe.collectAsStateWithLifecycle()
    val realizedNetProfitLe by viewModel.realizedNetProfitLe.collectAsStateWithLifecycle()
    val allocations by viewModel.tokenAllocations.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncState) {
        if (syncState is SyncState.Success) {
            snackbarHostState.showSnackbar("Synchronization Successful!")
            viewModel.resetSyncState()
        } else if (syncState is SyncState.Error) {
            snackbarHostState.showSnackbar("Sync failed: ${(syncState as SyncState.Error).message}")
            viewModel.resetSyncState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "P2P LEDGER",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = CyberCyan,
                                letterSpacing = 2.sp,
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Dashboard",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.triggerSync() },
                        modifier = Modifier.testTag("sync_button")
                    ) {
                        val rotationAnim = remember { Animatable(0f) }
                        if (syncState is SyncState.Syncing) {
                            LaunchedEffect(Unit) {
                                rotationAnim.animateTo(
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    )
                                )
                            }
                        } else {
                            run { LaunchedEffect(Unit) { rotationAnim.snapTo(0f) } }
                        }
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Data with Script URL",
                            tint = CyberCyan,
                            modifier = Modifier.rotate(rotationAnim.value)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberSlate)
                            .border(1.dp, BorderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "JD",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CyberCyan,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberSlate
                ),
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Live Synchronizer indicator bar
            if (syncState is SyncState.Syncing) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                        color = CyberCyan,
                        trackColor = BorderColor
                    )
                }
            }

            // --- Dashboard Metrics Cards Section ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 1. Orange Money Wallet Card (Amber)
                    MetricCard(
                        title = "ORANGE MONEY WALLET",
                        value = formatLeones(orangeMoneyLe),
                        subtext = "Starting Fund: 100M Le • Live Balance",
                        accentColor = CyberAmber,
                        icon = Icons.Default.Info,
                        testTag = "wallet_metric_card"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 2. Total Asset Value Card (Cyan)
                        Box(modifier = Modifier.weight(1f)) {
                            MiniMetricCard(
                                title = "TOTAL ASSET VALUE",
                                value = formatLeones(totalAssetLe),
                                usdEquivalent = "$${formatUsd(totalAssetLe / 22500.0)}",
                                accentColor = CyberCyan,
                                testTag = "asset_value_card"
                            )
                        }

                        // 3. Realized Net Profit Card (Emerald)
                        Box(modifier = Modifier.weight(1f)) {
                            MiniMetricCard(
                                title = "REALIZED NET PROFIT",
                                value = formatLeones(realizedNetProfitLe),
                                usdEquivalent = "$${formatUsd(realizedNetProfitLe / 22500.0)}",
                                accentColor = CyberEmerald,
                                testTag = "profit_card"
                            )
                        }
                    }
                }
            }

            // --- Token Allocation Metrics Section (Horizontal Scroll component) ---
            item {
                Column {
                    Text(
                        text = "TOKEN ALLOCATION METRICS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    if (allocations.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(95.dp)
                                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(12.dp))
                                .background(CyberSlate),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "No assets holding. Execute a buy order.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            allocations.forEach { token ->
                                AllocationCard(token)
                            }
                        }
                    }
                }
            }

            // --- Historic Logs Section ---
            item {
                Text(
                    text = "HISTORIC ACTIVITY LOGS",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(bottom = 4.dp).testTag("historic_logs_heading")
                )
            }

            if (items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No history available. Check database connection.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                }
            } else {
                items(items, key = { it.id }) { transaction ->
                    TransactionHistoryRow(
                        item = transaction,
                        onDeleteClick = { viewModel.deleteItem(transaction.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtext: String,
    accentColor: Color,
    icon: ImageVector,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)), RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(GradientStart, GradientEnd)
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                )
                if (title.contains("ORANGE", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(CyberEmerald.copy(alpha = 0.1f), RoundedCornerShape(100.dp))
                                .border(BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.2f)), RoundedCornerShape(100.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "SYNCING OK",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                        Text(
                            text = "Last update: 2m ago",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtext,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun MiniMetricCard(
    title: String,
    value: String,
    usdEquivalent: String,
    accentColor: Color,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(16.dp))
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = CyberSlate),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = usdEquivalent,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }
}

@Composable
fun AllocationCard(
    token: MainViewModel.TokenAllocation
) {
    val coinColor = remember(token.coin) {
        when (token.coin.uppercase()) {
            "BTC" -> CyberAmber
            "ETH" -> CyberCyan
            "USDT", "USDC" -> CyberEmerald
            else -> CyberCyan
        }
    }

    Card(
        modifier = Modifier
            .width(170.dp)
            .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberSlate.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(coinColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, coinColor.copy(alpha = 0.3f)), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = token.coin.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = coinColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = token.coin,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${(token.percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = coinColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = String.format(Locale.getDefault(), "%,.4f Qty", token.quantity),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$${String.format(Locale.getDefault(), "%,.0f", token.valueUsd)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = coinColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun TransactionHistoryRow(
    item: InventoryItem,
    onDeleteClick: () -> Unit
) {
    val isBuy = item.type == "BUY"
    val badgeColor = if (isBuy) CyberCyan else CyberAmber
    val textSign = if (isBuy) "+" else "-"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(BorderStroke(1.dp, Color(0x0DFFFFFF)), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0x08FFFFFF)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Outer circle for a modern transaction layout
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(badgeColor, CircleShape)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${item.type}: ${item.coin}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.wallet} • ${item.date}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    if (item.notes.isNotEmpty()) {
                        Text(
                            text = item.notes,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontWeight = FontWeight.Light),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$textSign${formatDouble(item.quantity)} ${item.coin}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Text(
                        text = "$${formatDouble(item.totalCost)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Order Entry",
                        tint = Color.Red.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Helpers
fun formatLeones(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    val formatted = formatter.format(amount)
    // Replace '$' with 'Le ' for Leones formatting representation
    return "Le " + formatted.substring(1)
}

fun formatUsd(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 2
    return formatter.format(amount)
}

fun formatDouble(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    formatter.maximumFractionDigits = 4
    return formatter.format(value)
}
