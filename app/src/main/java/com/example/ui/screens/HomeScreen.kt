package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.PlanTier
import com.example.model.UserSecurityConfig
import com.example.model.VaultFileMeta
import com.example.model.VaultFolder
import com.example.ui.components.FreeTierAdBanner
import com.example.ui.components.StorageRingGauge
import com.example.ui.components.VaultFileRow
import com.example.ui.components.VaultFolderCard
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PinkMedia
import com.example.ui.theme.ProGold
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.theme.VioletGlow
import com.example.ui.theme.VioletPrimary
import com.example.ui.theme.VioletSoft
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeVaultScreen(
    config: UserSecurityConfig,
    folders: List<VaultFolder>,
    files: List<VaultFileMeta>,
    isDecoyMode: Boolean,
    onFolderClick: (VaultFolder) -> Unit,
    onOpenFile: (VaultFileMeta) -> Unit,
    onShareFile: (VaultFileMeta) -> Unit,
    onDeleteFile: (VaultFileMeta) -> Unit,
    onImportFromGallery: () -> Unit,
    onImportFromCamera: () -> Unit,
    onImportFromFilesOrDrive: () -> Unit,
    onCreateCustomFolder: (name: String, colorHex: String, iconKey: String, pin: String?) -> Unit,
    onUpgradePro: () -> Unit,
    onLockNow: () -> Unit,
    onPreviewCalculator: () -> Unit
) {
    var showImportSheet by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }

    val visibleFolders = remember(folders, isDecoyMode) {
        if (isDecoyMode) folders.filter { it.decoyOnly } else folders.filter { !it.decoyOnly }
    }
    val visibleFiles = remember(files, isDecoyMode) {
        val filtered = if (isDecoyMode) files.filter { it.decoyItem } else files.filter { !it.decoyItem }
        filtered.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
    }
    val usedBytes = remember(visibleFiles) { visibleFiles.sumOf { it.sizeBytes } }
    val planTier = PlanTier.fromRaw(config.planTier)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(VioletPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Gopon Vault",
                                tint = VioletGlow
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isDecoyMode) "Personal Notes" else "Gopon Vault",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isDecoyMode) "Standard Vault Session" else "AES-256 Encrypted • Auto-Lock ${config.autoLockSeconds}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = SafeEmerald
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isDecoyMode) {
                            IconButton(
                                onClick = onPreviewCalculator,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_calculator_preview_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = "Calculator Disguise Mode",
                                    tint = VioletSoft
                                )
                            }
                        }
                        IconButton(
                            onClick = onLockNow,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("home_lock_now_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Vault Now",
                                tint = TextPrimary
                            )
                        }
                    }
                }
            }

            // Free plan subtle ad banner
            if (planTier == PlanTier.FREE && !isDecoyMode) {
                item {
                    FreeTierAdBanner(onUpgradeClick = onUpgradePro)
                }
            }

            // Storage Circle Gauge
            item {
                StorageRingGauge(
                    usedBytes = usedBytes,
                    planTier = planTier,
                    totalFilesCount = visibleFiles.size
                )
            }

            // All Folders Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All Folders",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    if (!isDecoyMode) {
                        TextButton(
                            onClick = { showNewFolderDialog = true },
                            modifier = Modifier.testTag("new_folder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = null,
                                tint = VioletSoft,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Folder", color = VioletSoft)
                        }
                    }
                }
            }

            // 2-Column Folder Grid inside LazyColumn
            val folderPairs = visibleFolders.chunked(2)
            items(folderPairs, key = { pair -> pair.joinToString("_") { it.id } }) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { folder ->
                        val count = visibleFiles.count { it.folderId == folder.id }
                        VaultFolderCard(
                            folder = folder,
                            itemCount = count,
                            onClick = { onFolderClick(folder) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Recent Files Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Files",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "${visibleFiles.size} total",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }

            if (visibleFiles.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = VaultSurface),
                        border = BorderStroke(1.dp, VaultCardBorder),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = VioletSoft,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Vault is empty", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Tap + Import below to hide photos, videos, audio, or PDFs.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(visibleFiles.take(8), key = { it.id }) { file ->
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
                onClick = { showImportSheet = true },
                containerColor = VioletPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("import_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import file to vault")
            }
        }
    }

    if (showImportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImportSheet = false },
            containerColor = VaultSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Import & Auto-Sort to Vault",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Files are AES-256 encrypted locally and hidden from your phone gallery.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                ImportSourceOption(
                    icon = Icons.Default.PhotoLibrary,
                    tint = VioletPrimary,
                    title = "Gallery (Photos & Videos)",
                    subtitle = "Zero-permission Photo Picker • JPG, PNG, HEIC, MP4, MOV",
                    testTag = "import_gallery_option",
                    onClick = {
                        showImportSheet = false
                        onImportFromGallery()
                    }
                )
                ImportSourceOption(
                    icon = Icons.Default.CameraAlt,
                    tint = PinkMedia,
                    title = "Private Vault Camera",
                    subtitle = "Capture directly into encrypted vault storage",
                    testTag = "import_camera_option",
                    onClick = {
                        showImportSheet = false
                        onImportFromCamera()
                    }
                )
                ImportSourceOption(
                    icon = Icons.Default.UploadFile,
                    tint = CyanAccent,
                    title = "File Manager & Google Drive",
                    subtitle = "PDF, DOC, XLS, MP3, WAV, M4A, ZIP, APK",
                    testTag = "import_files_drive_option",
                    onClick = {
                        showImportSheet = false
                        onImportFromFilesOrDrive()
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showNewFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showNewFolderDialog = false },
            onCreate = { name, colorHex, iconKey, pin ->
                showNewFolderDialog = false
                onCreateCustomFolder(name, colorHex, iconKey, pin)
            }
        )
    }
}

@Composable
private fun ImportSourceOption(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = VaultSurfaceElevated,
        border = BorderStroke(1.dp, VaultCardBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = tint)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, colorHex: String, iconKey: String, pin: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var lockFolder by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#8B5CF6") }

    val colors = listOf("#8B5CF6", "#EC4899", "#22D3EE", "#F59E0B", "#10B981")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurface,
        title = { Text("Create Custom Folder", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder Name (e.g., Tax Docs)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_folder_name_input")
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colors.forEach { hex ->
                        val c = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { selectedColor = hex }
                        ) {
                            if (selectedColor == hex) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lock folder with PIN", color = TextPrimary)
                    Switch(
                        checked = lockFolder,
                        onCheckedChange = { lockFolder = it }
                    )
                }
                if (lockFolder) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                        label = { Text("4-6 Digit Folder PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        name.trim(),
                        selectedColor,
                        "folder",
                        if (lockFolder && pin.length >= 4) pin else null
                    )
                },
                enabled = name.isNotBlank() && (!lockFolder || pin.length >= 4),
                colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
