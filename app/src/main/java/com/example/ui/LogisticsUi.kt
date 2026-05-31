package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogisticsAppContent(viewModel: LogisticsViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val currentRole by viewModel.currentRole.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val rawEvents by viewModel.allEvents.collectAsState()
    val pendingCount = remember(rawEvents) { rawEvents.count { it.syncStatus == "PENDING" } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Hub,
                            contentDescription = "LogiTwin Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "LogiTwin",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "DIGITAL TWIN v1.0",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Net status badge
                    val badgeColor = if (isOnline) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    val badgeText = if (isOnline) "ONLINE" else "OFFLINE"
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .border(
                                width = 1.dp,
                                color = badgeColor.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(
                                color = badgeColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .clickable { viewModel.setOnline(!isOnline) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color = badgeColor, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            badgeText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                        if (pendingCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                Text("$pendingCount PENDING")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scanner") },
                    label = { Text("Scanner") },
                    modifier = Modifier.testTag("nav_scanner_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.HomeWork, contentDescription = "Warehouse Mode") },
                    label = { Text("Stock Bins") },
                    modifier = Modifier.testTag("nav_warehouse_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.SupervisorAccount, contentDescription = "Supervisor Mode") },
                    label = { Text("Supervisor") },
                    modifier = Modifier.testTag("nav_supervisor_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { 
                        BadgedBox(
                            badge = {
                                if (pendingCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text("$pendingCount")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Terminal, contentDescription = "REST API Console")
                        }
                    },
                    label = { Text("API Logs") },
                    modifier = Modifier.testTag("nav_logs_tab")
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Role indicator bar
            RoleIndicatorBanner(role = currentRole, onRoleSwitch = { viewModel.setRole(it) })

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "TabsTransition"
            ) { tab ->
                when (tab) {
                    0 -> ScannerModeScreen(viewModel)
                    1 -> WarehouseModeScreen(viewModel)
                    2 -> SupervisorModeScreen(viewModel)
                    3 -> ApiConsoleScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun RoleIndicatorBanner(role: String, onRoleSwitch: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val roles = listOf("Warehouse Worker", "Supervisor", "Manager", "Admin")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Shield,
                contentDescription = "Role info",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Authorized Role: ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Text(
                text = role,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Box {
            Text(
                "Change Role ⚡",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                roles.forEach { r ->
                    DropdownMenuItem(
                        text = { Text(r, fontWeight = if (r == role) FontWeight.Bold else FontWeight.Normal) },
                        onClick = {
                            onRoleSwitch(r)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 1: SCANNER MODE SCREEN (IMMUTABLE SCAN ENGINE)
// ==========================================
@Composable
fun ScannerModeScreen(viewModel: LogisticsViewModel) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    val products by viewModel.allProducts.collectAsState()
    val rawEvents by viewModel.allEvents.collectAsState()
    val scope = rememberCoroutineScope()

    // Screen-level inputs
    val selectedSku by viewModel.selectedSku.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val selectedAction by viewModel.selectedAction.collectAsState()

    var showSkuDropdown by remember { mutableStateOf(false) }
    var showLocDropdown by remember { mutableStateOf(false) }
    var showActionDropdown by remember { mutableStateOf(false) }
    
    val locations = listOf(
        "WH-SEA-ZA-A1-B1", "WH-SEA-ZA-A2-B2", "WH-SEA-ZB-B1-B3",
        "WH-BOS-ZX-A4-D9", "WH-BOS-ZY-A5-E1", "WH-SEA-PORT-01 -> WH-SEA-ZA-A1-B1"
    )

    val actions = listOf(
        "ITEM_SCANNED_IN" to "Inbound - Put-Away Stock",
        "ITEM_SCANNED_OUT" to "Outbound - Dispatch Stock",
        "MOVED_LOCATION" to "Move Location (Relocation)",
        "DAMAGED_ITEM" to "Report Damage / Quarantine",
        "SHIPPED" to "Carrier Handover (Depart Shipment)",
        "RECEIVED" to "Destination Delivered"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "Logistics Scanner Console",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Every scan is processed as an immutable ledger event updating the digital twin state.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Camera Viewer Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasCameraPermission) {
                    CameraPreview(modifier = Modifier.fillMaxSize())
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                            Icon(
                                Icons.Filled.VideocamOff,
                                contentDescription = "Camera Permission Denied",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Camera Preview Offline",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Barcode Emulator is automatically active in fallback sandbox mode.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Laser sweep graphic overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sweepY = (System.currentTimeMillis() % 4000) / 4000f * size.height
                    drawLine(
                        color = Color.Red,
                        start = Offset(0f, sweepY),
                        end = Offset(size.width, sweepY),
                        strokeWidth = 3f
                    )
                }

                // HUD labels
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "SCAN ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scanner Engine Configuration Form
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "PHYSICAL GOODS SCANNER CONTROLS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 1. SELECT SKU
                Box {
                    OutlinedTextField(
                        value = selectedSku,
                        onValueChange = { viewModel.selectedSku.value = it },
                        label = { Text("Product Barcode / SKU") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sku_input_field"),
                        trailingIcon = {
                            IconButton(onClick = { showSkuDropdown = true }) {
                                Icon(Icons.Filled.ArrowDropDown, "Select SKU")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    DropdownMenu(
                        expanded = showSkuDropdown,
                        onDismissRequest = { showSkuDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        products.forEach { p ->
                            DropdownMenuItem(
                                text = { Text("${p.sku} (${p.name})", style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    viewModel.selectedSku.value = p.sku
                                    showSkuDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. SELECT LOCATION
                Box {
                    OutlinedTextField(
                        value = selectedLocation,
                        onValueChange = { viewModel.selectedLocation.value = it },
                        label = { Text("Warehouse Location Bin ID") },
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("location_input_field"),
                        trailingIcon = {
                            IconButton(onClick = { showLocDropdown = true }) {
                                Icon(Icons.Filled.ArrowDropDown, "Select Location")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showLocDropdown,
                        onDismissRequest = { showLocDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        locations.forEach { loc ->
                            DropdownMenuItem(
                                text = { Text(loc, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    viewModel.selectedLocation.value = loc
                                    showLocDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. SELECT ACTION / EVENT
                Box {
                    OutlinedTextField(
                        value = actions.firstOrNull { it.first == selectedAction }?.second ?: selectedAction,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Logistics Process Action") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("action_selector"),
                        trailingIcon = {
                            IconButton(onClick = { showActionDropdown = true }) {
                                Icon(Icons.Filled.ArrowDropDown, "Select Action")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showActionDropdown,
                        onDismissRequest = { showActionDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        actions.forEach { act ->
                            DropdownMenuItem(
                                text = { Text(act.second, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    viewModel.selectedAction.value = act.first
                                    showActionDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // EMULATE SCAN TRIGGER
                Button(
                    onClick = {
                        viewModel.performScan(selectedSku, selectedLocation, selectedAction)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("trigger_scan_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.QrCode, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "TRIGGER BARCODE DIGITAL SCAN",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent scan ledger log for quick audit validation
        Text(
            "LOCAL SCAN LEDGER EVENT BUFFER",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val localScans = remember(rawEvents) { rawEvents.filter { it.action != "RBAC_SWITCH" }.take(4) }
        if (localScans.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Text(
                    "No local scanning events have been triggered yet. Configure inputs above and click the scan trigger.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                localScans.forEach { scan ->
                    RecentScanResultRow(scan)
                }
            }
        }
    }
}

@Composable
fun RecentScanResultRow(event: LogisticsEvent) {
    val dateText = remember(event.timestamp) {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        format.format(Date(event.timestamp))
    }

    val actionColor = when (event.eventType) {
        "ITEM_SCANNED_IN" -> Color(0xFF4CAF50)
        "ITEM_SCANNED_OUT" -> Color(0xFF2196F3)
        "MOVED_LOCATION" -> Color(0xFF9C27B0)
        "DAMAGED_ITEM" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.secondary
    }

    val statusIcon = when (event.syncStatus) {
        "SYNCED" -> Icons.Filled.CloudDone
        "FAILED" -> Icons.Filled.ErrorOutline
        else -> Icons.Filled.CloudUpload
    }

    val statusColor = when (event.syncStatus) {
        "SYNCED" -> Color(0xFF4CAF50)
        "FAILED" -> Color(0xFFF44336)
        else -> Color(0xFFFF9800)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(actionColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (event.eventType) {
                            "ITEM_SCANNED_IN" -> Icons.Filled.KeyboardDoubleArrowDown
                            "ITEM_SCANNED_OUT" -> Icons.Filled.KeyboardDoubleArrowUp
                            "MOVED_LOCATION" -> Icons.Filled.DirectionsRun
                            "DAMAGED_ITEM" -> Icons.Filled.Dangerous
                            else -> Icons.Filled.Event
                        },
                        contentDescription = null,
                        tint = actionColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = event.sku,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(actionColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                event.eventType.replace("ITEM_SCANNED_", ""),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = actionColor,
                                fontSize = 8.sp
                            )
                        }
                    }
                    Text(
                        text = "Loc: ${event.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$dateText • Operator: ${event.workerId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = "Sync Status",
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = event.syncStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp
                )
            }
        }
    }
}

// ==========================================
// SCREEN 2: WAREHOUSE MODE SCREEN (STATE / BINS)
// ==========================================
@Composable
fun WarehouseModeScreen(viewModel: LogisticsViewModel) {
    val inventory by viewModel.allInventory.collectAsState()
    val products by viewModel.allProducts.collectAsState()

    var selectedWarehouse by remember { mutableStateOf("WH-SEA") }
    var selectedSkuFilter by remember { mutableStateOf("") }
    
    val matchingInventory = remember(inventory, selectedWarehouse, selectedSkuFilter) {
        inventory.filter { item ->
            item.warehouseId.equals(selectedWarehouse, ignoreCase = true) &&
            (selectedSkuFilter.isEmpty() || item.sku.contains(selectedSkuFilter, ignoreCase = true))
        }
    }

    var showRelocateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Warehouse Storage Rack State",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Digital twin view of physical warehouse bins. Direct manual overrides trigger event-driven corrections.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Select Warehouse & SKU filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { selectedWarehouse = "WH-SEA" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedWarehouse == "WH-SEA") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Seattle Hub (WH-SEA)", maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (selectedWarehouse == "WH-SEA") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Button(
                onClick = { selectedWarehouse = "WH-BOS" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedWarehouse == "WH-BOS") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Boston Term (WH-BOS)", maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (selectedWarehouse == "WH-BOS") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = selectedSkuFilter,
                onValueChange = { selectedSkuFilter = it },
                label = { Text("Filter Stock by SKU...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) }
            )
            
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { showRelocateDialog = true },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.Filled.SwapHoriz, "Relocate Units Action")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Shelves list
        Text(
            "ACTIVE DIGITAL TWIN GRID LOCATIONS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (matchingInventory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Inbox, "No State", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No stock items register in this zone matching the criteria.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(matchingInventory) { bin ->
                    val prod = products.firstOrNull { it.sku == bin.sku }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = bin.locationCode.replace("${selectedWarehouse}-", ""),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 11.sp
                                    )
                                }

                                Text(
                                    "${bin.quantity} ${prod?.unitOfMeasure ?: "pcs"}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = bin.sku,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = prod?.name ?: "Unknown Product SKU Description",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = { 
                                        viewModel.manualAdjustInventory(bin.sku, selectedWarehouse, bin.zone, bin.aisle, bin.bin, -1)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                ) {
                                    Icon(Icons.Filled.Remove, "Decrement Stock", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = { 
                                        viewModel.manualAdjustInventory(bin.sku, selectedWarehouse, bin.zone, bin.aisle, bin.bin, 1)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                ) {
                                    Icon(Icons.Filled.Add, "Increment Stock", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // RELOCATION SWAP DIALOG
    if (showRelocateDialog) {
        var moveSku by remember { mutableStateOf("") }
        var sourceLoc by remember { mutableStateOf("WH-SEA-ZA-A1-B1") }
        var destLoc by remember { mutableStateOf("WH-SEA-ZA-A2-B2") }

        AlertDialog(
            onDismissRequest = { showRelocateDialog = false },
            title = { Text("Relocate Goods (MOVED_LOCATION event)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Formulates a physical move. This issues an immutable ledger transaction subtract at Source and add at Destination.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = moveSku,
                        onValueChange = { moveSku = it },
                        label = { Text("Product Sku (e.g. SKU-ELEC-4302)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = sourceLoc,
                        onValueChange = { sourceLoc = it },
                        label = { Text("Source Location Bin") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = destLoc,
                        onValueChange = { destLoc = it },
                        label = { Text("Destination Location Bin") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (moveSku.isNotEmpty()) {
                            viewModel.performScan(moveSku, "$sourceLoc -> $destLoc", "MOVED_LOCATION")
                        }
                        showRelocateDialog = false
                    }
                ) {
                    Text("Record Cargo Relocation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRelocateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// SCREEN 3: SUPERVISOR SCREEN (DASHBOARDS & ROLES)
// ==========================================
@Composable
fun SupervisorModeScreen(viewModel: LogisticsViewModel) {
    val auditLogs by viewModel.allAuditLogs.collectAsState()
    val events by viewModel.allEvents.collectAsState()
    val inventory by viewModel.allInventory.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()

    var showCreateSkuDialog by remember { mutableStateOf(false) }

    // Metrics computation
    val stockAccuracy = remember(auditLogs) {
        val total = auditLogs.size.toDouble()
        if (total == 0.0) 100.0
        else {
            val errors = auditLogs.count { it.level == "ERROR" }.toDouble()
            ((total - errors) / total * 100.0).roundToInt()
        }
    }

    val warehouseEfficiency = remember(inventory) {
        val totalSpace = 50 // max space factor
        val occupied = inventory.size
        val percentage = (occupied.toDouble() / totalSpace.toDouble() * 100.0).roundToInt()
        percentage.coerceAtMost(100)
    }

    val scanThroughput = remember(events) {
        val lastInterval = System.currentTimeMillis() - 3600000 // last hr
        events.count { it.timestamp > lastInterval }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Supervisor Intelligence Dashboard",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Live warehouse operational metrics, physical audit logs, and catalog configuration.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Authorized checks
        val isAuthorized = currentRole in listOf("Supervisor", "Manager", "Admin")
        if (!isAuthorized) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "RBAC Encrypted Module",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "RBAC Access Restricted",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Your current authorized profile is 'Warehouse Worker'. Change authorized role in the top header banner to view details.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        } else {
            // METRICS GRID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "STOCK ACCURACY",
                    value = "$stockAccuracy%",
                    description = "Errors vs Total Scans",
                    icon = Icons.Filled.VerifiedUser,
                    iconColor = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "RACK FILL ACC.",
                    value = "$warehouseEfficiency%",
                    description = "Bin Usage Density",
                    icon = Icons.Filled.PieChart,
                    iconColor = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "SCANRATE/HR",
                    value = "$scanThroughput",
                    description = "Live flow velocity",
                    icon = Icons.Filled.Speed,
                    iconColor = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action: Configure SKU Catalog
            Button(
                onClick = { showCreateSkuDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("add_item_sku_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.LibraryAdd, "Register New SKU Catalog Item")
                Spacer(modifier = Modifier.width(10.dp))
                Text("REGISTER NEW CATALOG PRODUCT SKU", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AUDIT & ERROR LOGS TABLE
            Text(
                "SYSTEM SECURITY & TIMESTAMP AUDIT LOGS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    if (auditLogs.isEmpty()) {
                        Text(
                            "No audit logging records registered.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        auditLogs.take(15).forEach { audit ->
                            AuditLogRow(audit)
                        }
                    }
                }
            }
        }
    }

    // DIALOG: CREATE NEW PRODUCT SKU
    if (showCreateSkuDialog) {
        var sku by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Electronics") }
        var desc by remember { mutableStateOf("") }
        var uom by remember { mutableStateOf("pcs") }

        AlertDialog(
            onDismissRequest = { showCreateSkuDialog = false },
            title = { Text("Register Structural SKU Catalog Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("Barcode SKU ID (e.g. SKU-ELEC-5591)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product Standard Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Material Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Material Details / Tech Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uom,
                        onValueChange = { uom = it },
                        label = { Text("Unit of Measure (pcs, kilograms, rolls, m)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sku.isNotEmpty() && name.isNotEmpty()) {
                            viewModel.registerNewProduct(sku, name, category, desc, uom)
                        }
                        showCreateSkuDialog = false
                    }
                ) {
                    Text("Register Asset Sku")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateSkuDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AuditLogRow(audit: AuditLog) {
    val dateStr = remember(audit.timestamp) {
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        format.format(Date(audit.timestamp))
    }

    val levelColor = when (audit.level) {
        "ERROR" -> Color(0xFFF44336)
        "WARNING" -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "[$dateStr]",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .background(levelColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = audit.level,
                style = MaterialTheme.typography.labelSmall,
                color = levelColor,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = audit.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 14.sp
            )
            if (audit.sku != null) {
                Text(
                    text = "Ref SKU: ${audit.sku} • Dev: ${audit.deviceId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 8.sp
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

// ==========================================
// SCREEN 4: REST API LOGS screen
// ==========================================
@Composable
fun ApiConsoleScreen(viewModel: LogisticsViewModel) {
    val isOnline by viewModel.isOnline.collectAsState()
    val logs by viewModel.apiTerminalLogs.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "REST API Transactions Console",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Trace live network payloads generated by Kotlin Mobile Sync Engine and validated on simulated warehouse API endpoints.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Connectivity Switch panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isOnline) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                        contentDescription = "Wifi",
                        tint = if (isOnline) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isOnline) "Cloud Socket: Connected" else "Cloud Socket: Queued",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isOnline) "Auto-transmitting POST events" else "Events pending database write cache",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isOnline,
                    onCheckedChange = { viewModel.setOnline(it) },
                    modifier = Modifier.testTag("api_sync_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Trigger Sync Manual ping
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.forceSyncNow() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.Sync, "Force sync queue")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sync Queue", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { viewModel.deleteTerminalLog() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.DeleteSweep, "Clear console")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear Logs", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Monospace Console Shell
        Text(
            "REST PACKET LEDGER TRANSMISSION LOG STREAM",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "[Await Socket Trigger...]\nNo network API payloads transmit yet.",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs) { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = if (line.contains("[ERROR]") || line.contains("failed") || line.contains("Audit Break")) Color(0xFFF87171) 
                                    else if (line.contains("200 OK") || line.contains("201 Created") || line.contains("SYNCED")) Color(0xFF4ADE80) 
                                    else if (line.contains("Header") || line.contains("Body")) Color(0xFF94A3B8)
                                    else Color(0xFF38BDF8),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", exc)
                }
            }, executor)
            previewView
        },
        modifier = modifier
    )
}
