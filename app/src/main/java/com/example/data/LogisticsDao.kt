package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LogisticsDao {
    // Products
    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Query("SELECT * FROM products WHERE sku = :sku LIMIT 1")
    suspend fun getProductBySku(sku: String): Product?

    // Inventory
    @Query("SELECT * FROM inventory")
    fun getAllInventoryFlow(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory")
    suspend fun getAllInventoryList(): List<InventoryItem>

    @Query("SELECT * FROM inventory WHERE sku = :sku")
    suspend fun getInventoryBySku(sku: String): List<InventoryItem>

    @Query("SELECT * FROM inventory WHERE warehouseId = :whId AND zone = :zone AND aisle = :aisle AND bin = :bin AND sku = :sku LIMIT 1")
    suspend fun getInventoryAtLocationItem(whId: String, zone: String, aisle: String, bin: String, sku: String): InventoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem)

    @Update
    suspend fun updateInventoryItem(item: InventoryItem)

    @Query("DELETE FROM inventory WHERE quantity <= 0")
    suspend fun deleteZeroQuantityItems()

    // Events
    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    fun getAllEventsFlow(): Flow<List<LogisticsEvent>>

    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    suspend fun getAllEventsList(): List<LogisticsEvent>

    @Query("SELECT * FROM events WHERE syncStatus = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingEvents(): List<LogisticsEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: LogisticsEvent)

    @Update
    suspend fun updateEvent(event: LogisticsEvent)

    @Query("UPDATE events SET syncStatus = :status, errorMessage = :error WHERE id = :id")
    suspend fun updateEventSyncStatus(id: Long, status: String, error: String?)

    // Shipments
    @Query("SELECT * FROM shipments ORDER BY lastUpdated DESC")
    fun getAllShipments(): Flow<List<Shipment>>

    @Query("SELECT * FROM shipments WHERE shipmentId = :id LIMIT 1")
    suspend fun getShipmentById(id: String): Shipment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShipment(shipment: Shipment)

    // Audit Logs
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)
}
