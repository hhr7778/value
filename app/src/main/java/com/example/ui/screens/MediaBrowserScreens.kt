package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.model.SortOrder
import com.example.model.VaultCategory
import com.example.model.VaultFileMeta
import com.example.model.VaultFolder
import com.example.security.BiometricAvailability
import com.example.security.BiometricHelper
import com.example.ui.components.VaultFileGridCard
import com.example.ui.components.VaultFileRow
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.ProGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.theme.VioletPrimary
import com.example.ui.theme.VioletSoft
import com.google.firebase.Timestamp

@Composable
fun CategoryVaultBrowserScreen(
    title: String,
    files: List<VaultFileMeta>,
    searchQuery: String,
    sortOrder: SortOrder,
    isGridView: Boolean,
    isDocumentsHub: Boolean = false,
    activeFolder: VaultFolder? = null,
    isFolderUnlocked: Boolean = true,
    isDecoyMode: Boolean = false,
    onSearchChange: (String) -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onToggleViewMode: () -> Unit,
    onBack: (() -> Unit)? = null,
    onOpenFile: (VaultFileMeta) -> Unit,
    onShareFile: (VaultFileMeta) -> Unit,
    onDeleteFile: (VaultFileMeta) -> Unit,
    onImportClick: () -> Unit,
    onVerifyFolderPin: (VaultFolder, String) -> Boolean = { _, _ -> true },
    onUnlockFolderBiometric: (String) -> Unit = {},
    onToggleFolderLock: ((VaultFolder, Boolean, String?) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedDocFilter by remember { mutableStateOf<VaultCategory?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showFolderLockSetupDialog by remember { mutableStateOf(false) }

    val filteredFiles = remember(
        files, searchQuery, sortOrder, selectedDocFilter, isDocumentsHub, activeFolder, isDecoyMode
    ) {
        val base = if (isDecoyMode) files.filter { it.decoyItem } else files.filter { !it.decoyItem }
        val byFolderOrCat = when {
            activeFolder != null -> base.filter { it.folderId == activeFolder.id }
            isDocumentsHub && selectedDocFilter != null ->
                base.filter { VaultCategory.fromRaw(it.category) == selectedDocFilter }
            isDocumentsHub ->
                base.filter {
                    val c = VaultCategory.fromRaw(it.category)
                    c == VaultCategory.PDFS || c == VaultCategory.DOCUMENTS ||
                        c == VaultCategory.MUSIC || c == VaultCategory.OTHERS
                }
            else -> base
        }
        val searched = if (searchQuery.isBlank()) byFolderOrCat else {
            byFolderOrCat.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.extension.contains(searchQuery, ignoreCase = true)
            }
        }
        when (sortOrder) {
            SortOrder.DATE_DESC -> searched.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            SortOrder.NAME_ASC -> searched.sortedBy { it.name.lowercase() }
            SortOrder.SIZE_DESC -> searched.sortedByDescending { it.sizeBytes }
        }
    }

    // If entering a locked folder that has not yet been unlocked this session, show lock gate!
    if (activeFolder != null && activeFolder.locked && !isFolderUnlocked) {
        FolderPinChallengeView(
            folder = activeFolder,
            onBack = { onBack?.invoke() },
            onVerifyPin = { pin -> onVerifyFolderPin(activeFolder, pin) },
            onUnlockViaBiometric = {
                val activity = context as? FragmentActivity
                if (activity != null &&
                    BiometricHelper.checkCapabilities(activity).availability == BiometricAvailability.AVAILABLE
                ) {
                    BiometricHelper.showBiometricPrompt(
                        activity = activity,
                        title = "Unlock ${activeFolder.name}",
                        subtitle = "Verify fingerprint to open locked folder",
                        negativeButtonText = "Use Folder PIN",
                        onSuccess = { onUnlockFolderBiometric(activeFolder.id) },
                        onErrorOrFallback = {}
                    )
                }
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary
                                )
                            }
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (activeFolder != null && onToggleFolderLock != null && !isDecoyMode) {
                            IconButton(
                                onClick = {
                                    if (activeFolder.locked) {
                                        onToggleFolderLock(activeFolder, false, null)
                                    } else {
                                        showFolderLockSetupDialog = true
                                    }
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("folder_lock_toggle_btn")
                            ) {
                                Icon(
                                    imageVector = if (activeFolder.locked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Toggle Folder Lock",
                                    tint = if (activeFolder.locked) ProGold else TextSecondary
                                )
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { sortMenuExpanded = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("sort_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort files",
                                    tint = TextPrimary
                                )
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.label) },
                                        onClick = {
                                            sortMenuExpanded = false
                                            onSortChange(order)
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onToggleViewMode,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("grid_list_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle Grid or List view",
                                tint = VioletSoft
                            )
                        }
                    }
                }
            }

            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                    },
                    placeholder = { Text("Search encrypted files...") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VaultSurface,
                        unfocusedContainerColor = VaultSurface,
                        focusedBorderColor = VioletPrimary,
                        unfocusedBorderColor = VaultCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_search_input")
                )
            }

            // Documents / Audio / PDF / Others filter chips
            if (isDocumentsHub) {
                item {
                    val chips = listOf(
                        null to "All Docs",
                        VaultCategory.PDFS to "PDFs",
                        VaultCategory.DOCUMENTS to "Documents",
                        VaultCategory.MUSIC to "Music",
                        VaultCategory.OTHERS to "Others"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(chips) { (cat, label) ->
                            val selected = selectedDocFilter == cat
                            FilterChip(
                                selected = selected,
                                onClick = { selectedDocFilter = cat },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VioletPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = VaultSurfaceElevated,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            if (filteredFiles.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = VaultSurface),
                        border = BorderStroke(1.dp, VaultCardBorder),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOff,
                                contentDescription = null,
                                tint = VioletSoft,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No encrypted files here yet", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Use + to securely import and encrypt files into this vault.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else if (isGridView) {
                val rows = filteredFiles.chunked(2)
                items(rows, key = { r -> r.joinToString("_") { it.id } }) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { file ->
                            VaultFileGridCard(
                                file = file,
                                onOpen = { onOpenFile(file) },
                                onShare = { onShareFile(file) },
                                onDelete = { onDeleteFile(file) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                items(filteredFiles, key = { it.id }) { file ->
                    VaultFileRow(
                        file = file,
                        onOpen = { onOpenFile(file) },
                        onShare = { onShareFile(file) },
                        onDelete = { onDeleteFile(file) }
                    )
                }
            }
        }

        if (!isDecoyMode) {
            FloatingActionButton(
                onClick = onImportClick,
                containerColor = VioletPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("browser_import_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import file")
            }
        }
    }

    if (showFolderLockSetupDialog && activeFolder != null && onToggleFolderLock != null) {
        var newFolderPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showFolderLockSetupDialog = false },
            containerColor = VaultSurface,
            title = { Text("Lock \"${activeFolder.name}\"", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Set a dedicated 4-6 digit PIN for this folder (fingerprint can also unlock it).",
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = newFolderPin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) newFolderPin = it },
                        label = { Text("Folder PIN (4-6 digits)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFolderLockSetupDialog = false
                        onToggleFolderLock(activeFolder, true, newFolderPin)
                    },
                    enabled = newFolderPin.length >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
                ) {
                    Text("Lock Folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFolderLockSetupDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun FolderPinChallengeView(
    folder: VaultFolder,
    onBack: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onUnlockViaBiometric: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VaultSurface),
            border = BorderStroke(1.dp, VaultCardBorder),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Folder Locked",
                    tint = ProGold,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = "${folder.name} is Locked",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Enter this folder's PIN or authenticate with your fingerprint.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        if (it.length <= 6 && it.all(Char::isDigit)) {
                            pinInput = it
                            errorMsg = null
                        }
                    },
                    label = { Text("Folder PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(errorMsg ?: "", color = AlertCrimson, style = MaterialTheme.typography.bodyMedium)
                }
                Button(
                    onClick = {
                        if (!onVerifyPin(pinInput)) {
                            errorMsg = "Incorrect folder PIN"
                            pinInput = ""
                        }
                    },
                    enabled = pinInput.length >= 4,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
                ) {
                    Text("Unlock Folder")
                }
                OutlinedButton(
                    onClick = onUnlockViaBiometric,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    border = BorderStroke(1.dp, VaultCardBorder)
                ) {
                    Icon(Icons.Default.Fingerprint, null, tint = VioletSoft)
                    Spacer(Modifier.width(8.dp))
                    Text("Unlock with Fingerprint", color = TextPrimary)
                }
                TextButton(onClick = onBack) {
                    Text("Go Back", color = TextSecondary)
                }
            }
        }
    }
}
