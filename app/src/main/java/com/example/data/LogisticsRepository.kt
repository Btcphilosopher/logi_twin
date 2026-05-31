package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogisticsRepository(private val dao: LogisticsDao) {

    val allProducts: Flow<List<Product>> = dao.getAllProducts()
    val allInventory: Flow<List<InventoryItem>> = dao.getAllInventoryFlow()
    val allEvents: Flow<List<LogisticsEvent>> = dao.getAllEventsFlow()
    val allShipments: Flow<List<Shipment>> = dao.getAllShipments()
    val allAuditLogs: Flow<List<AuditLog>> = dao.getAllAuditLogs()

    suspend fun getProductBySku(sku: String): Product? = dao.getProductBySku(sku)

    suspend fun getShipmentById(id: String): Shipment? = dao.getShipmentById(id)

    /**
     * Seeds the database with high-fidelity logistics data if empty.
     */
    suspend fun seedDatabase() {
        val existingProducts = dao.getAllProducts().first()
        if (existingProducts.isEmpty()) {
            Log.d("LogisticsRepository", "Seeding products table...")
            val products = listOf(
                Product("SKU-MED-8492", "Seamless Carbon Steel Tubing", "Metalwork", "Industrial hydraulic grading 15mm-diameter tube", "m"),
                Product("SKU-ELEC-4302", "Quasar LED Module 50W", "Electronics", "High efficiency outdoor signage chip with heatsink", "pcs"),
                Product("SKU-COMP-0182", "Octane DDR5 Server RAM 32G", "Computing", "ECC registered server hardware, high frequency", "pcs"),
                Product("SKU-GEN-9511", "Heavy Duty Pallet Tension Strap", "Packaging", "Plated steel reinforced securing wraps", "rolls"),
                Product("SKU-CHEM-7731", "Thermal Silicone Compound TG-7", "Consumables", "High thermal conductivity non-curing paste", "tubes")
            )
            for (p in products) {
                dao.insertProduct(p)
            }

            Log.d("LogisticsRepository", "Seeding inventory table...")
            val inventory = listOf(
                InventoryItem(0, "SKU-ELEC-4302", "WH-SEA", "A", "1", "A1", 120),
                InventoryItem(0, "SKU-MED-8492", "WH-SEA", "A", "2", "A2", 45),
                InventoryItem(0, "SKU-COMP-0182", "WH-SEA", "B", "1", "B1", 98),
                InventoryItem(0, "SKU-GEN-9511", "WH-BOS", "X", "4", "D9", 650),
                InventoryItem(0, "SKU-CHEM-7731", "WH-BOS", "Y", "5", "E1", 84)
            )
            for (i in inventory) {
                dao.insertInventoryItem(i)
            }

            Log.d("LogisticsRepository", "Seeding shipments table...")
            val shipments = listOf(
                Shipment("SHIP-7791", "ORD-109", "SKU-COMP-0182", 15, "Austin Fulfillment Center", "Created", "FedEx Express"),
                Shipment("SHIP-8812", "ORD-110", "SKU-ELEC-4302", 5, "Miami Port Terminal", "Packed", "DHL Logistics"),
                Shipment("SHIP-9904", "ORD-111", "SKU-MED-8492", 20, "Detroit Steel Assembly", "Dispatched", "UPS Freight")
            )
            for (s in shipments) {
                dao.insertShipment(s)
            }

            Log.d("LogisticsRepository", "Seeding initial audits...")
            dao.insertAuditLog(AuditLog(
                timestamp = System.currentTimeMillis() - 3600000 * 4,
                message = "System inventory digital twin initialized. Database schema v1.0 active.",
                level = "INFO",
                deviceId = "cloud-server",
                sku = null
            ))
            dao.insertAuditLog(AuditLog(
                timestamp = System.currentTimeMillis() - 3600000 * 2,
                message = "SKU scan warning: unrecognized code code39_048x on Scanner-02 flagged and rejected.",
                level = "WARNING",
                deviceId = "scanner-02",
                sku = "code39_048x"
            ))
        }
    }

    /**
     * The core financial-grade EVENT PROCESSING ENGINE.
     * Every modification to inventory state MUST flow through this function.
     */
    suspend fun processEvent(event: LogisticsEvent): ProcessResult {
        Log.d("LogisticsRepository", "Processing event: ${event.eventType} for SKU ${event.sku}")

        // 1. Verify SKU exists
        val product = dao.getProductBySku(event.sku)
        if (product == null) {
            val errorMessage = "Invalid Scan: SKU '${event.sku}' does not exist in standard product catalog."
            dao.insertAuditLog(AuditLog(
                message = errorMessage,
                level = "ERROR",
                deviceId = event.deviceId,
                sku = event.sku
            ))
            dao.insertEvent(event.copy(syncStatus = "FAILED", errorMessage = errorMessage))
            return ProcessResult.Failure(errorMessage)
        }

        // Parse Warehouse Location components
        // Expecting event.location format: "WH_ID-Z[Zone]-A[Aisle]-B[Bin]"
        // If not formatted, fallback to defaults
        val parsedLocation = parseLocation(event.location)

        // 2. Perform event-driven state calculation
        when (event.eventType) {
            "ITEM_SCANNED_IN" -> {
                // Inbound receive / Put-away
                val existing = dao.getInventoryAtLocationItem(
                    parsedLocation.warehouseId,
                    parsedLocation.zone,
                    parsedLocation.aisle,
                    parsedLocation.bin,
                    event.sku
                )
                if (existing != null) {
                    val updated = existing.copy(quantity = existing.quantity + 1)
                    dao.insertInventoryItem(updated)
                } else {
                    dao.insertInventoryItem(
                        InventoryItem(
                            sku = event.sku,
                            warehouseId = parsedLocation.warehouseId,
                            zone = parsedLocation.zone,
                            aisle = parsedLocation.aisle,
                            bin = parsedLocation.bin,
                            quantity = 1
                        )
                    )
                }
                dao.insertAuditLog(AuditLog(
                    message = "Inbound process: SKU ${event.sku} placed in location ${event.location}. Qty incremented by 1.",
                    level = "INFO",
                    deviceId = event.deviceId,
                    sku = event.sku
                ))
            }

            "ITEM_SCANNED_OUT" -> {
                // Outbound ship / pick
                val existing = dao.getInventoryAtLocationItem(
                    parsedLocation.warehouseId,
                    parsedLocation.zone,
                    parsedLocation.aisle,
                    parsedLocation.bin,
                    event.sku
                )
                if (existing == null || existing.quantity < 1) {
                    val errorMsg = "Audit Break: Outbound pick failed for ${event.sku} at ${event.location}. No registered stock."
                    dao.insertAuditLog(AuditLog(
                        message = errorMsg,
                        level = "ERROR",
                        deviceId = event.deviceId,
                        sku = event.sku
                    ))
                    dao.insertEvent(event.copy(syncStatus = "FAILED", errorMessage = errorMsg))
                    return ProcessResult.Failure(errorMsg)
                } else {
                    val updated = existing.copy(quantity = existing.quantity - 1)
                    dao.insertInventoryItem(updated)
                    dao.deleteZeroQuantityItems()
                    dao.insertAuditLog(AuditLog(
                        message = "Outbound process: SKU ${event.sku} dispatched from ${event.location}. Qty decremented by 1.",
                        level = "INFO",
                        deviceId = event.deviceId,
                        sku = event.sku
                    ))
                }
            }

            "MOVED_LOCATION" -> {
                // For a MOVED_LOCATION, the location string encodes "SOURCE_LOC -> DEST_LOC"
                // or similar. Let's make it robust: event.location stores e.g. "WH-SEA-ZA-A1-B1 -> WH-SEA-ZA-A2-B2"
                val nodes = event.location.split(" -> ")
                if (nodes.size != 2) {
                    val errorMsg = "Location Move Error: Relocation location field must format as 'SRC -> DEST'"
                    dao.insertAuditLog(AuditLog(message = errorMsg, level = "ERROR", deviceId = event.deviceId, sku = event.sku))
                    dao.insertEvent(event.copy(syncStatus = "FAILED", errorMessage = errorMsg))
                    return ProcessResult.Failure(errorMsg)
                }
                val srcLoc = parseLocation(nodes[0])
                val destLoc = parseLocation(nodes[1])

                val existingSrc = dao.getInventoryAtLocationItem(
                    srcLoc.warehouseId, srcLoc.zone, srcLoc.aisle, srcLoc.bin, event.sku
                )
                if (existingSrc == null || existingSrc.quantity < 1) {
                    val errorMsg = "Relocation Mismatch: SKU ${event.sku} source location ${nodes[0]} has 0 units. Stock re-aligned."
                    dao.insertAuditLog(AuditLog(message = errorMsg, level = "WARNING", deviceId = event.deviceId, sku = event.sku))
                    // Still record the move, but add Dest anyway (re-align stock)
                    val existingDest = dao.getInventoryAtLocationItem(
                        destLoc.warehouseId, destLoc.zone, destLoc.aisle, destLoc.bin, event.sku
                    )
                    if (existingDest != null) {
                        dao.insertInventoryItem(existingDest.copy(quantity = existingDest.quantity + 1))
                    } else {
                        dao.insertInventoryItem(
                            InventoryItem(
                                sku = event.sku,
                                warehouseId = destLoc.warehouseId,
                                zone = destLoc.zone,
                                aisle = destLoc.aisle,
                                bin = destLoc.bin,
                                quantity = 1
                            )
                        )
                    }
                } else {
                    // Update source matching real move
                    dao.insertInventoryItem(existingSrc.copy(quantity = existingSrc.quantity - 1))
                    dao.deleteZeroQuantityItems()

                    // Update destination
                    val existingDest = dao.getInventoryAtLocationItem(
                        destLoc.warehouseId, destLoc.zone, destLoc.aisle, destLoc.bin, event.sku
                    )
                    if (existingDest != null) {
                        dao.insertInventoryItem(existingDest.copy(quantity = existingDest.quantity + 1))
                    } else {
                        dao.insertInventoryItem(
                            InventoryItem(
                                sku = event.sku,
                                warehouseId = destLoc.warehouseId,
                                zone = destLoc.zone,
                                aisle = destLoc.aisle,
                                bin = destLoc.bin,
                                quantity = 1
                            )
                        )
                    }
                }
                dao.insertAuditLog(AuditLog(
                    message = "Stock relocated: SKU ${event.sku} moved from ${nodes[0]} to ${nodes[1]}.",
                    level = "INFO",
                    deviceId = event.deviceId,
                    sku = event.sku
                ))
            }

            "DAMAGED_ITEM" -> {
                // Remove damaged item from inventory and log warning
                val existing = dao.getInventoryAtLocationItem(
                    parsedLocation.warehouseId,
                    parsedLocation.zone,
                    parsedLocation.aisle,
                    parsedLocation.bin,
                    event.sku
                )
                if (existing == null || existing.quantity < 1) {
                    val errorMsg = "Reporting Damage Error: Attempted to report damage on ${event.sku} at ${event.location}, but 0 units exist."
                    dao.insertAuditLog(AuditLog(message = errorMsg, level = "ERROR", deviceId = event.deviceId, sku = event.sku))
                    dao.insertEvent(event.copy(syncStatus = "FAILED", errorMessage = errorMsg))
                    return ProcessResult.Failure(errorMsg)
                } else {
                    dao.insertInventoryItem(existing.copy(quantity = existing.quantity - 1))
                    dao.deleteZeroQuantityItems()
                    dao.insertAuditLog(AuditLog(
                        message = "Material quarantine: Unit of ${event.sku} marked DAMAGED and quarantined from ${event.location}.",
                        level = "WARNING",
                        deviceId = event.deviceId,
                        sku = event.sku
                    ))
                }
            }

            "SHIPPED" -> {
                // Find matching shipment
                val activeShipment = findMatchingShipmentForSku(event.sku)
                if (activeShipment != null) {
                    dao.insertShipment(activeShipment.copy(status = "In Transit", lastUpdated = System.currentTimeMillis()))
                    dao.insertAuditLog(AuditLog(
                        message = "Logistics Carrier dispatch: Shipment ${activeShipment.shipmentId} (Order ${activeShipment.orderNumber}) has departed.",
                        level = "INFO",
                        deviceId = event.deviceId,
                        sku = event.sku
                    ))
                } else {
                    dao.insertAuditLog(AuditLog(
                        message = "Shipment update skipped: SKU ${event.sku} scanned but no active shipments match.",
                        level = "WARNING",
                        deviceId = event.deviceId,
                        sku = event.sku
                    ))
                }
            }

            "RECEIVED" -> {
                // Deliver package at destination
                val activeShipment = findMatchingShipmentForSku(event.sku, "In Transit")
                if (activeShipment != null) {
                    dao.insertShipment(activeShipment.copy(status = "Delivered", lastUpdated = System.currentTimeMillis()))
                    dao.insertAuditLog(AuditLog(
                        message = "Delivered: Carrier confirmation for shipment ${activeShipment.shipmentId}. Package handoff completed.",
                        level = "INFO",
                        deviceId = event.deviceId,
                        sku = event.sku
                    ))
                } else {
                    dao.insertAuditLog(AuditLog(
                        message = "Delivery sweep skipped: Received scanner payload for SKU ${event.sku} but found no active transit shipments.",
                        level = "WARNING",
                        deviceId = event.deviceId,
                        sku = event.sku
                    ))
                }
            }
        }

        // 3. Store event with status pending/complete depending on sync engine state (default is stored)
        dao.insertEvent(event)
        return ProcessResult.Success(event)
    }

    /**
     * Simulated network upload logic for the Sync Engine.
     */
    suspend fun syncPendingEvents(): Int {
        val pending = dao.getPendingEvents()
        if (pending.isEmpty()) return 0

        var syncedCount = 0
        for (event in pending) {
            try {
                // Simulate network latency (0.2s per log upload)
                kotlinx.coroutines.delay(100)
                // Mark synced
                dao.updateEventSyncStatus(event.id, "SYNCED", null)
                syncedCount++
            } catch (e: Exception) {
                dao.updateEventSyncStatus(event.id, "FAILED", e.localizedMessage)
            }
        }
        return syncedCount
    }

    private suspend fun findMatchingShipmentForSku(sku: String, requiredStatus: String? = null): Shipment? {
        val list = dao.getAllShipments().first()
        return if (requiredStatus != null) {
            list.firstOrNull { it.sku == sku && it.status.equals(requiredStatus, ignoreCase = true) }
        } else {
            list.firstOrNull { it.sku == sku && !it.status.equals("Delivered", ignoreCase = true) }
        }
    }

    private fun parseLocation(locationStr: String): ParsedLoc {
        return try {
            // Location format: WH-SEA-ZA-A1-B1
            val parts = locationStr.split("-")
            ParsedLoc(
                warehouseId = parts.getOrNull(0) ?: "WH-GEN",
                zone = parts.getOrNull(1)?.replace("Z", "") ?: "A",
                aisle = parts.getOrNull(2)?.replace("A", "") ?: "1",
                bin = parts.getOrNull(3)?.replace("B", "") ?: "1"
            )
        } catch (e: Exception) {
            ParsedLoc("WH-SEA", "A", "1", "1")
        }
    }

    private data class ParsedLoc(val warehouseId: String, val zone: String, val aisle: String, val bin: String)
}

sealed class ProcessResult {
    data class Success(val event: LogisticsEvent) : ProcessResult()
    data class Failure(val message: String) : ProcessResult()
}
