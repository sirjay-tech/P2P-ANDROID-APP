package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.InventoryDatabase
import com.example.data.model.InventoryItem
import com.example.data.repository.InventoryRepository
import com.example.network.api.InventoryApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(
    application: Application,
    private val repository: InventoryRepository
) : AndroidViewModel(application) {

    // Exchange rate: 1 USD = 22,500 Leones (Le)
    private val USD_TO_LE = 22500.0

    // Supported Coins & Mock Last Traded Prices in USD
    val supportedCoins = listOf("USDT", "USDC", "BTC", "ETH", "SOL")
    val coinPrices = mapOf(
        "USDT" to 1.0,
        "USDC" to 1.05,
        "BTC" to 67500.0,
        "ETH" to 3450.0,
        "SOL" to 160.0
    )

    // Supported wallets
    val supportedWallets = listOf("Orange Money Wallet", "Bank Transfer", "Cash")

    // Syncing status
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // Base URL configuration (mutable so users can point to their own Apps Script URL)
    private val _backendUrl = MutableStateFlow("https://ais-dev-essurmr4spn2fc4dejy76k-912272783273.europe-west2.run.app")
    val backendUrl: StateFlow<String> = _backendUrl.asStateFlow()

    // Flow of all inventory transactions
    val items: StateFlow<List<InventoryItem>> = repository.allItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Seeding database with clean, beautiful initial transactions if empty
        viewModelScope.launch {
            repository.allItems.collect { currentItems ->
                if (currentItems.isEmpty() && syncState.value == SyncState.Idle) {
                    seedDatabase()
                }
            }
        }
    }

    /**
     * Seed database with default professional trades
     */
    private suspend fun seedDatabase() {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val seed = listOf(
            InventoryItem(
                coin = "BTC",
                type = "BUY",
                quantity = 0.5,
                price = 62000.0,
                totalCost = 31000.0,
                wallet = "Orange Money Wallet",
                date = sdf.format(Date(System.currentTimeMillis() - 86400000 * 5)), // 5 days ago
                notes = "Initial purchase of Bitcoin during minor dip"
            ),
            InventoryItem(
                coin = "USDT",
                type = "BUY",
                quantity = 2500.0,
                price = 1.0,
                totalCost = 2500.0,
                wallet = "Orange Money Wallet",
                date = sdf.format(Date(System.currentTimeMillis() - 86400000 * 3)),
                notes = "Liquidity reserve swap"
            ),
            InventoryItem(
                coin = "SOL",
                type = "BUY",
                quantity = 15.0,
                price = 145.0,
                totalCost = 2175.0,
                wallet = "Bank Transfer",
                date = sdf.format(Date(System.currentTimeMillis() - 86400000 * 2)),
                notes = "Position building"
            ),
            InventoryItem(
                coin = "BTC",
                type = "SELL",
                quantity = 0.15,
                price = 68000.0,
                totalCost = 10200.0,
                wallet = "Orange Money Wallet",
                date = sdf.format(Date(System.currentTimeMillis() - 86400000)),
                notes = "Taking profit near resistance"
            )
        )
        for (item in seed) {
            repository.insertItem(item)
        }
    }

    // --- Dynamic Calculations Section ---

    // Initial Wallet balance in Leones prior to any transactions
    private val STARTING_ORANGE_MONEY_LE = 100000000.0 // 100 Million Leones

    /**
     * Compute current cash balance in Orange Money Wallet reactively.
     * Buys deduct cash, Sells credit cash.
     * All prices in USD are converted to Leones (Le).
     */
    val orangeMoneyBalance: StateFlow<Double> = items.map { transactionList ->
        var balance = STARTING_ORANGE_MONEY_LE
        transactionList.forEach { item ->
            val amountLe = item.totalCost * USD_TO_LE
            if (item.wallet == "Orange Money Wallet") {
                if (item.type == "BUY") {
                    balance -= amountLe
                } else if (item.type == "SELL") {
                    balance += amountLe
                }
            }
        }
        balance
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), STARTING_ORANGE_MONEY_LE)

    /**
     * Compute Realized Net Profit (Leones) reactively.
     * Calculates the profit made during SELL actions using the Weighted Average Cost basis for purchased coins.
     */
    val realizedNetProfitLe: StateFlow<Double> = items.map { transactionList ->
        calculateRealizedProfitLe(transactionList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /**
     * Compute Total Asset Value (Leones) reactively.
     * Aggregates remaining coin holdings priced at current mock rates.
     */
    val totalAssetValueLe: StateFlow<Double> = items.map { transactionList ->
        val balances = calculateCoinBalances(transactionList)
        var sumLe = 0.0
        balances.forEach { (coin, qty) ->
            val priceUsd = coinPrices[coin] ?: 1.0
            sumLe += qty * priceUsd * USD_TO_LE
        }
        sumLe
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /**
     * Exposes individual coin balances and allocations
     */
    data class TokenAllocation(
        val coin: String,
        val quantity: Double,
        val valueUsd: Double,
        val valueLe: Double,
        val percentage: Float
    )

    val tokenAllocations: StateFlow<List<TokenAllocation>> = items.map { transactionList ->
        val balances = calculateCoinBalances(transactionList)
        val list = mutableListOf<TokenAllocation>()
        var totalValUsd = 0.0

        balances.forEach { (coin, qty) ->
            val priceUsd = coinPrices[coin] ?: 0.0
            totalValUsd += (qty * priceUsd)
        }

        balances.forEach { (coin, qty) ->
            if (qty > 0.0001) {
                val priceUsd = coinPrices[coin] ?: 0.0
                val valUsd = qty * priceUsd
                val valLe = valUsd * USD_TO_LE
                val pct = if (totalValUsd > 0.0) (valUsd / totalValUsd).toFloat() else 0f
                list.add(TokenAllocation(coin, qty, valUsd, valLe, pct))
            }
        }
        list.sortedByDescending { it.valueUsd }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Helper: calculate net coin volumes available
    fun calculateCoinBalances(transactionList: List<InventoryItem>): Map<String, Double> {
        val balances = mutableMapOf<String, Double>()
        // Process chronologically (id ascending/descending needs ordered collection)
        transactionList.sortedBy { it.id }.forEach { item ->
            val current = balances[item.coin] ?: 0.0
            if (item.type == "BUY") {
                balances[item.coin] = current + item.quantity
            } else if (item.type == "SELL") {
                balances[item.coin] = maxOf(0.0, current - item.quantity)
            }
        }
        return balances
    }

    /**
     * Calculation engine: FIFO/Weighted Average cost profit for trade disposals
     */
    private fun calculateRealizedProfitLe(transactions: List<InventoryItem>): Double {
        var profitLe = 0.0
        val sorted = transactions.sortedBy { it.id }

        // Track remaining cost-basis units for each coin
        // Pair(quantity, priceUsd)
        val coinsHistory = mutableMapOf<String, MutableList<Pair<Double, Double>>>()

        sorted.forEach { item ->
            if (item.type == "BUY") {
                val list = coinsHistory.getOrPut(item.coin) { mutableListOf() }
                list.add(Pair(item.quantity, item.price))
            } else if (item.type == "SELL") {
                val history = coinsHistory[item.coin] ?: mutableListOf()
                var sellQty = item.quantity
                var costUsdForSell = 0.0

                // Average cost calculation or FIFO logic mapping:
                // Weighted average cost basis is standard:
                val totalRemainingQty = history.sumOf { it.first }
                val totalRemainingCost = history.sumOf { it.first * it.second }
                val avgUnitCostBasisUsd = if (totalRemainingQty > 0) totalRemainingCost / totalRemainingQty else 0.0

                costUsdForSell = sellQty * avgUnitCostBasisUsd

                // Subtract from history
                var deducted = 0.0
                while (sellQty > 0 && history.isNotEmpty()) {
                    val head = history.first()
                    if (head.first <= sellQty) {
                        sellQty -= head.first
                        deducted += head.first
                        history.removeAt(0)
                    } else {
                        val updatedHead = Pair(head.first - sellQty, head.second)
                        deducted += sellQty
                        sellQty = 0.0
                        history[0] = updatedHead
                    }
                }

                val sellRevenueUsd = item.quantity * item.price
                val profitUsd = sellRevenueUsd - costUsdForSell
                profitLe += profitUsd * USD_TO_LE
            }
        }
        return profitLe
    }

    // --- Action Methods ---

    /**
     * Execute Buy Trade
     */
    fun executeBuy(
        coin: String,
        quantity: Double,
        price: Double,
        wallet: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val buyItem = InventoryItem(
                coin = coin,
                type = "BUY",
                quantity = quantity,
                price = price,
                totalCost = quantity * price,
                wallet = wallet,
                date = sdf.format(Date()),
                notes = notes
            )
            repository.uploadTransaction(buyItem)
            onSuccess()
        }
    }

    /**
     * Execute Sell Trade
     */
    fun executeSell(
        coin: String,
        quantity: Double,
        price: Double,
        wallet: String,
        notes: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            // Check matching coin holdings to prevent selling more than in ledger
            val list = items.value
            val balances = calculateCoinBalances(list)
            val available = balances[coin] ?: 0.0
            
            if (quantity > available) {
                onFailure("Insufficient holdings! Available $coin: $available, requested: $quantity")
                return@launch
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val sellItem = InventoryItem(
                coin = coin,
                type = "SELL",
                quantity = quantity,
                price = price,
                totalCost = quantity * price,
                wallet = wallet,
                date = sdf.format(Date()),
                notes = notes
            )
            repository.uploadTransaction(sellItem)
            onSuccess()
        }
    }

    /**
     * Delete an inventory transaction item
     */
    fun deleteItem(id: Int) {
        viewModelScope.launch {
            repository.deleteItem(id)
        }
    }

    /**
     * Update Backend Url and recreate Repository Api Service
     */
    fun updateUrl(url: String) {
        _backendUrl.value = url
    }

    /**
     * Sync with Google Apps Script Deployment URL
     */
    fun triggerSync() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            val result = repository.syncWithBackendWebapp()
            if (result.isSuccess) {
                _syncState.value = SyncState.Success
            } else {
                _syncState.value = SyncState.Error(result.exceptionOrNull()?.localizedMessage ?: "Unknown network sync error")
            }
        }
    }

    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }
}

sealed interface SyncState {
    object Idle : SyncState
    object Syncing : SyncState
    object Success : SyncState
    data class Error(val message: String) : SyncState
}

/**
 * ViewModel Factory manual class helper
 */
class MainViewModelFactory(
    private val application: Application,
    private val repository: InventoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
