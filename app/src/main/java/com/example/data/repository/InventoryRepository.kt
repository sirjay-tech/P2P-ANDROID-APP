package com.example.data.repository

import com.example.data.local.InventoryDao
import com.example.data.model.InventoryItem
import com.example.network.api.InventoryApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class InventoryRepository(
    private val inventoryDao: InventoryDao,
    private val apiService: InventoryApiService
) {
    // Expose local database flow for reactive UI
    val allItems: Flow<List<InventoryItem>> = inventoryDao.getAllItemsFlow()

    suspend fun insertItem(item: InventoryItem): Long {
        return inventoryDao.insertItem(item)
    }

    suspend fun deleteItem(id: Int) {
        inventoryDao.deleteItemById(id)
    }

    suspend fun clearLocalCache() {
        inventoryDao.clearAll()
    }

    // Network Sync Operations
    // Fetches payload from Apps Script and overwrites/merges local persistence cache
    suspend fun syncWithBackendWebapp(): Result<Unit> {
        return try {
            val response = apiService.fetchInventory()
            if (response.isSuccessful) {
                val remoteItems = response.body() ?: emptyList()
                
                // Map API dto model objects into local database Entity objects
                val entities = remoteItems.map { dto ->
                    InventoryItem(
                        id = dto.id ?: 0,
                        coin = dto.coin ?: "USDT",
                        type = dto.type ?: "BUY",
                        quantity = dto.quantity ?: 0.0,
                        price = dto.price ?: 0.0,
                        totalCost = dto.totalCost ?: (dto.quantity?.times(dto.price ?: 0.0) ?: 0.0),
                        wallet = dto.wallet ?: "Orange Money Wallet",
                        date = dto.date ?: "2026-06-10",
                        notes = dto.notes ?: ""
                    )
                }
                
                // Write to SQLite via Room
                inventoryDao.clearAll()
                entities.forEach { entity ->
                    inventoryDao.insertItem(entity)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Backend synchronized retrieval failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Push local transaction creation back up to the web application
    suspend fun uploadTransaction(item: InventoryItem): Result<InventoryItem> {
        return try {
            // Mapping from Entity to Network DTO object
            val dto = InventoryApiService.InventoryDto(
                id = if (item.id == 0) null else item.id,
                coin = item.coin,
                type = item.type,
                quantity = item.quantity,
                price = item.price,
                totalCost = item.totalCost,
                wallet = item.wallet,
                date = item.date,
                notes = item.notes
            )
            val response = apiService.submitTransaction(dto)
            if (response.isSuccessful && response.body() != null) {
                val savedDto = response.body()!!
                val resultEntity = InventoryItem(
                    id = savedDto.id ?: item.id,
                    coin = savedDto.coin ?: item.coin,
                    type = savedDto.type ?: item.type,
                    quantity = savedDto.quantity ?: item.quantity,
                    price = savedDto.price ?: item.price,
                    totalCost = savedDto.totalCost ?: item.totalCost,
                    wallet = savedDto.wallet ?: item.wallet,
                    date = savedDto.date ?: item.date,
                    notes = savedDto.notes ?: item.notes
                )
                // Persist the synced entity from server with correct ID
                inventoryDao.insertItem(resultEntity)
                Result.success(resultEntity)
            } else {
                // Return success but with local cached copy if server doesn't respond or is unreachable
                inventoryDao.insertItem(item)
                Result.success(item)
            }
        } catch (e: Exception) {
            // Keep local entry available in offline cache mode
            val localId = inventoryDao.insertItem(item).toInt()
            Result.success(item.copy(id = localId))
        }
    }
}
