package com.example.ui

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LogisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repo = LogisticsRepository(db.logisticsDao())

    // UI state states
    val allProducts: StateFlow<List<Product>> = repo.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInventory: StateFlow<List<InventoryItem>> = repo.allInventory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEvents: StateFlow<List<LogisticsEvent>> = repo.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allShipments: StateFlow<List<Shipment>> = repo.allShipments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAuditLogs: StateFlow<List<AuditLog>> = repo.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // System Settings & Toggles
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _currentRole = MutableStateFlow("Warehouse Worker") // Supervisor, Manager, Admin, Warehouse Worker
    val currentRole: StateFlow<String> = _currentRole.asStateFlow()

    private val _activeWorker = MutableStateFlow("Worker Alex")
    val activeWorker: StateFlow<String> = _activeWorker.asStateFlow()

    // Mock API terminal logs
    private val _apiTerminalLogs = MutableStateFlow<List<String>>(emptyList())
    val apiTerminalLogs: StateFlow<List<String>> = _apiTerminalLogs.asStateFlow()

    // Active screen inputs (temporary Scanner State)
    val selectedSku = MutableStateFlow("SKU-COMP-0182")
    val selectedLocation = MutableStateFlow("WH-SEA-ZA-A1-B1")
    val selectedAction = MutableStateFlow("ITEM_SCANNED_IN")

    init {
        viewModelScope.launch {
            repo.seedDatabase()
            addTerminalLog("[SYSTEM] App started. Digital twin database synced offline.")
            
            // Continuous Sync background engine loop
            launch {
                while (true) {
                    kotlinx.coroutines.delay(4000)
                    if (_isOnline.value) {
                        triggerSyncCycle()
                    }
                }
            }
        }
    }

    fun setOnline(online: Boolean) {
        _isOnline.value = online
        val state = if (online) "ONLINE (REST API automatic sync active)" else "OFFLINE (Queue storing events locally)"
        addTerminalLog("[NETWORK] Network status changed: $state")
    }

    fun setRole(role: String) {
        _currentRole.value = role
        val message = "Operator role authorized as: $role"
        addTerminalLog("[SECURITY] RBAC update: $message")
        viewModelScope.launch {
            repo.processEvent(
                LogisticsEvent(
                    eventType = "ROLE_CHANGE",
                    sku = "SKU-GEN-9511", // placeholder SKU
                    location = "WH-SEA-PORT-01",
                    action = "RBAC_SWITCH",
                    deviceId = "scanner-01",
                    workerId = _activeWorker.value,
                    syncStatus = "SYNCED"
                )
            )
        }
    }

    fun setWorker(name: String) {
        _activeWorker.value = name
        addTerminalLog("[SESSION] Operator changed to: $name")
    }

    private fun addTerminalLog(logStr: String) {
        val stamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        _apiTerminalLogs.update { current ->
            listOf("[$stamp] $logStr") + current.take(49) // Keep last 50 logs
        }
    }

    /**
     * Scanner Trigger: Simulates Camera barcode locked / manual barcode commit.
     */
    fun performScan(sku: String, location: String, action: String) {
        viewModelScope.launch {
            // Beep audio feedback
            playScanBeep()

            val event = LogisticsEvent(
                eventType = action,
                sku = sku,
                location = location,
                action = when (action) {
                    "ITEM_SCANNED_IN" -> "INBOUND"
                    "ITEM_SCANNED_OUT" -> "OUTBOUND"
                    "MOVED_LOCATION" -> "MOVE"
                    "DAMAGED_ITEM" -> "DAMAGES"
                    "SHIPPED" -> "SHIPPING_OUT"
                    "RECEIVED" -> "CARRIER_DELIVERY"
                    else -> "IDENTIFY"
                },
                deviceId = "scanner-01",
                workerId = _activeWorker.value,
                syncStatus = "PENDING" // starts as pending, then sync engine fires
            )

            // Dynamic Terminal output matching requested API structures
            val payloadText = """
                POST /api/v1/scan
                Header: "Authorization: Bearer dev-sec-token-${_currentRole.value.uppercase().replace(" ", "_")}"
                Body: {
                  "sku": "$sku",
                  "location": "$location",
                  "action": "$action",
                  "deviceId": "scanner-01",
                  "worker": "${_activeWorker.value}"
                }
            """.trimIndent()
            addTerminalLog("[REST API TX]\n$payloadText")

            val result = repo.processEvent(event)
            when (result) {
                is ProcessResult.Success -> {
                    addTerminalLog("[LOCAL RECORD] Saved scan database event internally. Queue updated.")
                }
                is ProcessResult.Failure -> {
                    addTerminalLog("[ERROR] State validation failed: ${result.message}")
                }
            }

            if (_isOnline.value) {
                triggerSyncCycle()
            }
        }
    }

    /**
     * Triggers the Sync Engine transaction cycle.
     */
    private suspend fun triggerSyncCycle() {
        val syncedCount = repo.syncPendingEvents()
        if (syncedCount > 0) {
            addTerminalLog("[SYNC ENGINE] Successfully synced $syncedCount pending event(s) with central cloud server.")
            addTerminalLog("[SYS_SYNCS] Digital twin updated live on cloud database. REST /api/v1/warehouse/WH-SEA/state returned 200 OK.")
        }
    }

    /**
     * Sound & Buzz feedback.
     */
    private fun playScanBeep() {
        try {
            val toneG = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            Log.e("LogisticsViewModel", "Failed to play beep: ${e.localizedMessage}")
        }
    }

    /**
     * Force full sync action from the Sync Console.
     */
    fun forceSyncNow() {
        viewModelScope.launch {
            addTerminalLog("[SYNC ENGINE] Manual sync requested. Pinging cloud gateway...")
            val synced = repo.syncPendingEvents()
            if (synced > 0) {
                addTerminalLog("[SYNC ENGINE] Sync complete. $synced packet(s) uploaded successfully.")
            } else {
                addTerminalLog("[SYNC ENGINE] Sync complete. Queue was already clean (0 pending events).")
            }
        }
    }

    /**
     * Manual Stock adjustment (Warehouse Mode)
     */
    fun manualAdjustInventory(sku: String, wh: String, zone: String, aisle: String, bin: String, difference: Int) {
        viewModelScope.launch {
            playScanBeep()
            val eventType = if (difference > 0) "ITEM_SCANNED_IN" else "ITEM_SCANNED_OUT"
            val locCode = "$wh-Z$zone-A$aisle-B$bin"
            
            val event = LogisticsEvent(
                eventType = eventType,
                sku = sku,
                location = locCode,
                action = "MANUAL_ADJUSTMENT",
                deviceId = "scanner-01",
                workerId = _activeWorker.value
            )

            repo.processEvent(event)
            addTerminalLog("[REST API - MANUAL] POST /api/v1/warehouse/adjustment for SKU $sku adjusted by $difference units.")
            if (_isOnline.value) {
                triggerSyncCycle()
            }
        }
    }

    /**
     * Helper to add a custom Product in Supervisor mode.
     */
    fun registerNewProduct(sku: String, name: String, category: String, desc: String, uom: String) {
        viewModelScope.launch {
            val newProduct = Product(sku, name, category, desc, uom)
            db.logisticsDao().insertProduct(newProduct)
            db.logisticsDao().insertAuditLog(AuditLog(
                message = "New Product SKU registered on catalog: $sku - $name",
                level = "INFO",
                deviceId = "supervisor-dashboard",
                sku = sku
            ))
            addTerminalLog("[API] POST /api/v1/inventory/register returned 201 Created for structural SKU '$sku'.")
        }
    }

    fun deleteTerminalLog() {
        _apiTerminalLogs.value = emptyList()
    }
}

class LogisticsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LogisticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LogisticsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
