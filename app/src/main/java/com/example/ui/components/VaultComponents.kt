package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FREE_TIER_LIMIT_BYTES
import com.example.model.PlanTier
import com.example.model.VaultCategory
import com.example.model.VaultFileMeta
import com.example.model.VaultFolder
import com.example.model.formatBytesReadable
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PinkMedia
import com.example.ui.theme.ProGold
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultBackgroundBrush
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.theme.VioletGlow
import com.example.ui.theme.VioletPrimary
import com.example.ui.theme.VioletSoft

@Composable
fun VaultGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VaultBackgroundBrush),
        content = content
    )
}

@Composable
fun StorageRingGauge(
    usedBytes: Long,
    planTier: PlanTier,
    totalFilesCount: Int,
    modifier: Modifier = Modifier
) {
    val limitBytes = if (planTier == PlanTier.PRO) (50L * 1024L * 1024L * 1024L) else FREE_TIER_LIMIT_BYTES
    val rawRatio = if (usedBytes <= 0L) 0.02f else (usedBytes.toFloat() / limitBytes.toFloat()).coerceIn(0.03f, 1f)
    val animatedSweep by animateFloatAsState(
        targetValue = rawRatio * 270f,
        animationSpec = tween(durationMillis = 800),
        label = "storage_sweep"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("storage_circle_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = VaultSurface.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, VaultCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(124.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 12.dp.toPx()
                    val pad = strokeWidth / 2f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    drawArc(
                        color = Color(0xFF2B1E4A),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(VioletPrimary, VioletGlow, CyanAccent, VioletPrimary)
                        ),
                        startAngle = 135f,
                        sweepAngle = animatedSweep,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatBytesReadable(usedBytes),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (planTier == PlanTier.PRO) "UNLIMITED" else "of 2.00 GB",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (planTier == PlanTier.PRO) ProGold else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Vault Storage",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (planTier == PlanTier.PRO) ProGold.copy(alpha = 0.18f) else VioletPrimary.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = if (planTier == PlanTier.PRO) "PRO" else "FREE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (planTier == PlanTier.PRO) ProGold else VioletSoft,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$totalFilesCount encrypted items hidden from device gallery",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                val freeText = if (planTier == PlanTier.PRO) {
                    "Unlimited AES-256 Encrypted Storage"
                } else {
                    "Free: ${formatBytesReadable((FREE_TIER_LIMIT_BYTES - usedBytes).coerceAtLeast(0L))}"
                }
                Text(
                    text = freeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = SafeEmerald
                )
            }
        }
    }
}

@Composable
fun FreeTierAdBanner(
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onUpgradeClick)
            .testTag("free_tier_ad_banner"),
        color = Color(0xFF1F1638),
        border = BorderStroke(1.dp, ProGold.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Pro feature badge",
                    tint = ProGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Free Plan • Light Ad Slot",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Go Pro for 99 Taka/mo — Unlimited Storage & Zero Ads",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = ProGold,
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = "PRO",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF120A24),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
fun VaultFolderCard(
    folder: VaultFolder,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = parseFolderColor(folder.colorHex)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("folder_card_${folder.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VaultSurfaceElevated),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconForFolderKey(folder.iconKey),
                        contentDescription = folder.name,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (folder.locked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Folder locked",
                        tint = ProGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$itemCount items",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun VaultFileRow(
    file: VaultFileMeta,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val cat = VaultCategory.fromRaw(file.category)
    val iconColor = colorForCategory(cat)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .testTag("file_row_${file.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VaultSurface),
        border = BorderStroke(1.dp, VaultCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForCategory(cat),
                    contentDescription = cat.label,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${file.extension.uppercase()} • ${formatBytesReadable(file.sizeBytes)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    if (file.backedUp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Backed up",
                            tint = SafeEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("file_menu_${file.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "File options",
                        tint = TextSecondary
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Export / Share (Fingerprint)") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete (Fingerprint)") },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VaultFileGridCard(
    file: VaultFileMeta,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val cat = VaultCategory.fromRaw(file.category)
    val accent = colorForCategory(cat)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .testTag("file_grid_${file.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = VaultSurfaceElevated),
        border = BorderStroke(1.dp, VaultCardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(accent.copy(alpha = 0.28f), Color(0xFF140B29))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForCategory(cat),
                    contentDescription = cat.label,
                    tint = accent,
                    modifier = Modifier.size(34.dp)
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = file.extension.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share (Fingerprint)") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onShare()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete (Fingerprint)") },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = file.name,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatBytesReadable(file.sizeBytes),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

/**
 * Tactile dark circular PIN keypad inspired by luxury obsidian security UIs.
 */
@Composable
fun PinPadKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { digit ->
                    KeypadCircleButton(
                        label = digit,
                        onClick = { onDigit(digit) },
                        testTag = "pin_key_$digit"
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onClear != null) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClear)
                        .testTag("pin_key_clear"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CLR",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(72.dp))
            }

            KeypadCircleButton(
                label = "0",
                onClick = { onDigit("0") },
                testTag = "pin_key_0"
            )

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1526))
                    .border(1.dp, VaultCardBorder.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onBackspace)
                    .testTag("pin_key_backspace"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun KeypadCircleButton(
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color(0xFF1F1A2E))
            .border(1.dp, Color(0xFF332A4D), CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

fun parseFolderColor(hex: String): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrElse { VioletPrimary }
}

fun iconForFolderKey(key: String): ImageVector = when (key) {
    "photo" -> Icons.Default.Image
    "video" -> Icons.Default.VideoFile
    "music" -> Icons.Default.AudioFile
    "pdf" -> Icons.Default.PictureAsPdf
    "doc" -> Icons.Default.Description
    "archive" -> Icons.Default.Archive
    "bank" -> Icons.Default.AccountBalance
    "personal" -> Icons.Default.Person
    "cert" -> Icons.Default.Badge
    else -> Icons.Default.Folder
}

fun iconForCategory(category: VaultCategory): ImageVector = when (category) {
    VaultCategory.PHOTOS -> Icons.Default.Image
    VaultCategory.VIDEOS -> Icons.Default.VideoFile
    VaultCategory.MUSIC -> Icons.Default.AudioFile
    VaultCategory.PDFS -> Icons.Default.PictureAsPdf
    VaultCategory.DOCUMENTS -> Icons.Default.Description
    VaultCategory.OTHERS -> Icons.AutoMirrored.Filled.InsertDriveFile
}

fun colorForCategory(category: VaultCategory): Color = when (category) {
    VaultCategory.PHOTOS -> VioletPrimary
    VaultCategory.VIDEOS -> PinkMedia
    VaultCategory.MUSIC -> CyanAccent
    VaultCategory.PDFS -> ProGold
    VaultCategory.DOCUMENTS -> SafeEmerald
    VaultCategory.OTHERS -> TextMuted
}
