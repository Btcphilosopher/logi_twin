package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val sku: String,
    val name: String,
    val category: String,
    val description: String,
    val unitOfMeasure: String = "pcs"
)

@Entity(tableName = "inventory")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sku: String,
    val warehouseId: String,
    val zone: String,
    val aisle: String,
    val bin: String,
    val quantity: Int
) {
    val locationCode: String get() = "$warehouseId-Z$zone-A$aisle-B$bin"
}

@Entity(tableName = "events")
data class LogisticsEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: String = UUID.randomUUID().toString(),
    val eventType: String, // ITEM_SCANNED_IN, ITEM_SCANNED_OUT, MOVED_LOCATION, DAMAGED_ITEM, SHIPPED, RECEIVED
    val sku: String,
    val location: String, // e.g. WH1-ZA-A1-B1
    val action: String, // INBOUND, OUTBOUND, MOVE, DAMAGE_REPORT, DISPATCH, RECEIVE
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String = "scanner-01",
    val workerId: String = "worker-alex",
    var syncStatus: String = "PENDING", // PENDING, SYNCED, FAILED
    val errorMessage: String? = null
)

@Entity(tableName = "shipments")
data class Shipment(
    @PrimaryKey val shipmentId: String,
    val orderNumber: String,
    val sku: String,
    val quantity: Int,
    val destination: String,
    val status: String, // Created, Packed, Dispatched, In Transit, Delivered
    val carrier: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val level: String, // INFO, WARNING, ERROR
    val deviceId: String = "scanner-01",
    val sku: String? = null
)
