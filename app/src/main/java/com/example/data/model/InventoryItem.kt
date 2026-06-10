package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coin: String,         // e.g. "USDT", "BTC", "ETH"
    val type: String,         // "BUY" or "SELL"
    val quantity: Double,     // Quantity traded
    val price: Double,        // Price per unit in USD/Le
    val totalCost: Double,    // quantity * price
    val wallet: String,       // e.g. "Orange Money Wallet", "Bank Transfer", "Cash"
    val date: String,         // Date formatted string
    val notes: String         // Internal note description
) : Serializable
