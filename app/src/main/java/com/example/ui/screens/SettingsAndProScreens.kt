package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AlertType
import com.example.model.CloudBackupRecord
import com.example.model.PlanTier
import com.example.model.ProPlanCycle
import com.example.model.SecurityAlertLog
import com.example.model.UserSecurityConfig
import com.example.model.formatBytesReadable
import com.example.ui.components.VaultGradientBackground
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyanAccent
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
import java.io.File

@Composable
fun SettingsScreen(
    config: UserSecurityConfig,
    backups: List<CloudBackupRecord>,
    alertsCount: Int,
    onUpdateSettings: (Map<String, Any>, String?) -> Unit,
    onUpdateMasterPin: (String) -> Unit,
    onUpdateDecoyPin: (String) -> Unit,
    onUpdatePanicPin: (String) -> Unit,
    onCreateBackup: (String) -> Unit,
    onRestoreBackup: (String, CloudBackupRecord) -> Unit,
    onOpenBreakInAlerts: () -> Unit,
    onOpenProUpgrade: () -> Unit,
    onPreviewCalculator: () -> Unit,
    onRequestIntruderPermissions: () -> Unit,
    onSignOut: () -> Unit
) {
    var showPinEditor by remember { mutableStateOf<String?>(null) } // "MASTER", "DECOY", "PANIC", "CALC"
    var showBackupDialog by remember { mutableStateOf(false) }
    var restoreTarget by remember { mutableStateOf<CloudBackupRecord?>(null) }

    val planTier = PlanTier.fromRaw(config.planTier)

    LazyColumn(
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Security & Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = config.email.ifBlank { "Google Account Connected" },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Pro Plan Banner Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenProUpgrade)
                    .testTag("settings_pro_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF231645)),
                border = BorderStroke(1.dp, ProGold.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Star, null, tint = ProGold, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (planTier == PlanTier.PRO) "Gopon Pro Active" else "Upgrade to Gopon Pro",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = if (planTier == PlanTier.PRO) {
                                    "${ProPlanCycle.fromRaw(config.proPlanCycle).label} • Unlimited Storage"
                                } else {
                                    "99 Taka/mo or 799 Taka/yr • Unlimited, Zero Ads & Alerts"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = ProGold
                            )
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = ProGold)
                }
            }
        }

        // Biometrics & Unlock Modes
        item {
            SectionLabel("Biometric & Unlock Modes")
            Card(
                colors = CardDefaults.cardColors(containerColor = VaultSurface),
                border = BorderStroke(1.dp, VaultCardBorder),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column {
                    SettingsToggleRow(
                        icon = Icons.Default.Fingerprint,
                        title = "Fingerprint Unlock",
                        subtitle = "Required to unlock vault & authorize Share/Delete",
                        checked = config.fingerprintEnabled,
                        testTag = "toggle_fingerprint",
                        onCheckedChange = {
                            onUpdateSettings(mapOf("fingerprintEnabled" to it), null)
                        }
                    )
                    SettingsToggleRow(
                        icon = Icons.Default.Face,
                        title = "Face Lock",
                        subtitle = "Use device face recognition when supported",
                        checked = config.faceLockEnabled,
                        testTag = "toggle_face_lock",
                        onCheckedChange = {
                            onUpdateSettings(mapOf("faceLockEnabled" to it), null)
                        }
                    )
                    SettingsToggleRow(
                        icon = Icons.Default.Security,
                        title = "Double Security Mode",
                        subtitle = "Require BOTH Fingerprint + Master PIN",
                        checked = config.doubleSecurityMode,
                        testTag = "toggle_double_security",
                        onCheckedChange = { enabled ->
                            onUpdateSettings(
                                mapOf(
                                    "doubleSecurityMode" to enabled,
                                    "fastMode" to if (enabled) false else config.fastMode
                                ),
                                if (enabled) "Double Security enabled" else "Double Security disabled"
                            )
                        }
                    )
                    SettingsToggleRow(
                        icon = Icons.Default.Bolt,
                        title = "Fast Mode (Fingerprint Only)",
                        subtitle = "Instant unlock with fingerprint (3 fails -> PIN)",
                        checked = config.fastMode,
                        testTag = "toggle_fast_mode",
                        onCheckedChange = { enabled ->
                            onUpdateSettings(
                                mapOf(
                                    "fastMode" to enabled,
                                    "doubleSecurityMode" to if (enabled) false else config.doubleSecurityMode
                                ),
                                null
                            )
                        }
                    )
                }
            }
        }

        // Disguise & Intruder Protection
        item {
            SectionLabel("Stealth, Decoy & Intruder Defense")
            Card(
                colors = CardDefaults.cardColors(containerColor = VaultSurface),
                border = BorderStroke(1.dp, VaultCardBorder),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column {
                    SettingsToggleRow(
                        icon = Icons.Default.Calculate,
                        title = "Calculator Vault Mode",
                        subtitle = "Launch disguised as Calculator (Code: ${config.calculatorCode}=)",
                        checked = config.calculatorModeEnabled,
                        testTag = "toggle_calculator_mode",
                        onCheckedChange = {
                            onUpdateSettings(
                                mapOf("calculatorModeEnabled" to it),
                                if (it) "Calculator Disguise enabled on next lock" else "Standard lock enabled"
                            )
                        }
                    )
                    SettingsActionRow(
                        icon = Icons.Default.Calculate,
                        title = "Test Calculator Disguise Now",
                        subtitle = "Enter ${config.calculatorCode} then = to re-enter vault",
                        onClick = onPreviewCalculator
                    )
                    SettingsActionRow(
                        icon = Icons.Default.Lock,
                        title = "Change Master PIN & Calculator Code",
                        subtitle = "Update Master PIN or secret calculator sequence",
                        onClick = { showPinEditor = "MASTER" }
                    )
                    SettingsActionRow(
                        icon = Icons.Default.VisibilityOff,
                        title = "Decoy / Fake Vault Password",
                        subtitle = "Opens empty dummy vault (default 0000)",
                        onClick = { showPinEditor = "DECOY" }
                    )
                    SettingsActionRow(
                        icon = Icons.Default.Warning,
                        title = "Panic Password",
                        subtitle = "Instantly shreds all files & hides app (default 9999)",
                        tint = AlertCrimson,
                        onClick = { showPinEditor = "PANIC" }
                    )
                    SettingsActionRow(
                        icon = Icons.Default.CameraFront,
                        title = "Break-In Selfies & Intruder Alerts",
                        subtitle = "$alertsCount logged events • Selfie at 3 & 15, Wipe at 20",
                        tint = ProGold,
                        onClick = onOpenBreakInAlerts
                    )
                    SettingsActionRow(
                        icon = Icons.Default.Email,
                        title = "Enable Camera & Location for Break-In Alerts",
                        subtitle = "Grant runtime permissions for silent front-camera selfie",
                        onClick = onRequestIntruderPermissions
                    )
                }
            }
        }

        // Encrypted Cloud Backup
        item {
            SectionLabel("AES-256 Encrypted Cloud Backup")
            Card(
                colors = CardDefaults.cardColors(containerColor = VaultSurface),
                border = BorderStroke(1.dp, VaultCardBorder),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column {
                    SettingsToggleRow(
                        icon = Icons.Default.Backup,
                        title = "Auto Backup",
                        subtitle = "Automatically flag encrypted vault snapshots",
                        checked = config.autoBackupEnabled,
                        testTag = "toggle_auto_backup",
                        onCheckedChange = {
                            onUpdateSettings(mapOf("autoBackupEnabled" to it), null)
                        }
                    )
                    SettingsActionRow(
                        icon = Icons.Default.CloudUpload,
                        title = "Backup Now (AES-256-GCM)",
                        subtitle = "Encrypts vault index with your Master Password",
                        tint = CyanAccent,
                        onClick = { showBackupDialog = true }
                    )
                    if (backups.isNotEmpty()) {
                        backups.take(3).forEach { bak ->
                            SettingsActionRow(
                                icon = Icons.Default.Restore,
                                title = "Restore: ${bak.label}",
                                subtitle = "${formatBytesReadable(bak.totalBytes)} • Requires Master Password",
                                tint = SafeEmerald,
                                onClick = { restoreTarget = bak }
                            )
                        }
                    }
                }
            }
        }

        // Sign Out
        item {
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sign_out_button"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AlertCrimson.copy(alpha = 0.5f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = AlertCrimson)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", color = AlertCrimson)
            }
        }
    }

    if (showPinEditor != null) {
        var inputValue by remember { mutableStateOf("") }
        var calcValue by remember { mutableStateOf(config.calculatorCode) }
        val mode = showPinEditor!!
        AlertDialog(
            onDismissRequest = { showPinEditor = null },
            containerColor = VaultSurface,
            title = {
                Text(
                    when (mode) {
                        "MASTER" -> "Update Master PIN"
                        "DECOY" -> "Set Fake / Decoy PIN"
                        else -> "Set Panic Wipe PIN"
                    },
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) inputValue = it },
                        label = { Text("New 4-6 Digit PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (mode == "MASTER") {
                        OutlinedTextField(
                            value = calcValue,
                            onValueChange = { if (it.length <= 10 && it.all(Char::isDigit)) calcValue = it },
                            label = { Text("Calculator Secret Code (min 4 digits)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (mode) {
                            "MASTER" -> {
                                if (inputValue.length >= 4) onUpdateMasterPin(inputValue)
                                if (calcValue.length >= 4 && calcValue != config.calculatorCode) {
                                    onUpdateSettings(mapOf("calculatorCode" to calcValue), "Calculator code updated")
                                }
                            }
                            "DECOY" -> if (inputValue.length >= 4) onUpdateDecoyPin(inputValue)
                            "PANIC" -> if (inputValue.length >= 4) onUpdatePanicPin(inputValue)
                        }
                        showPinEditor = null
                    },
                    enabled = inputValue.length >= 4 || (mode == "MASTER" && calcValue.length >= 4),
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinEditor = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showBackupDialog) {
        MasterPasswordPromptDialog(
            title = "Encrypt Cloud Backup",
            description = "Enter your Master Password to derive the AES-256-GCM backup key.",
            confirmLabel = "Encrypt & Upload",
            onDismiss = { showBackupDialog = false },
            onConfirm = { pw ->
                showBackupDialog = false
                onCreateBackup(pw)
            }
        )
    }

    if (restoreTarget != null) {
        val target = restoreTarget!!
        MasterPasswordPromptDialog(
            title = "Restore Encrypted Backup",
            description = "Backup \"${target.label}\" can only be decrypted with your Master Password.",
            confirmLabel = "Decrypt & Restore",
            onDismiss = { restoreTarget = null },
            onConfirm = { pw ->
                restoreTarget = null
                onRestoreBackup(pw, target)
            }
        )
    }
}

@Composable
fun ProUpgradeScreen(
    currentTier: PlanTier,
    currentCycle: ProPlanCycle,
    onSelectPlan: (ProPlanCycle) -> Unit,
    onBack: () -> Unit
) {
    VaultGradientBackground {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Text("Gopon Vault Pro", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF21153F)),
                    border = BorderStroke(1.dp, ProGold.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = ProGold, modifier = Modifier.size(30.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Unlock Maximum Privacy", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                        }
                        ProBenefitRow("Unlimited Encrypted Vault Storage (Free capped at 2 GB)")
                        ProBenefitRow("Zero Ads Experience")
                        ProBenefitRow("Automated AES-256 Cloud Backup")
                        ProBenefitRow("Break-In Selfie + Gmail Alert Escalation")
                        ProBenefitRow("Decoy Vault & Panic Self-Destruct")
                    }
                }
            }

            item {
                PlanOptionCard(
                    title = "Monthly Plan",
                    price = "99 Taka / Month",
                    badge = if (currentTier == PlanTier.PRO && currentCycle == ProPlanCycle.MONTHLY_99) "ACTIVE" else "FLEXIBLE",
                    isSelected = currentCycle == ProPlanCycle.MONTHLY_99,
                    onClick = { onSelectPlan(ProPlanCycle.MONTHLY_99) }
                )
            }

            item {
                PlanOptionCard(
                    title = "Yearly Plan",
                    price = "799 Taka / Year",
                    badge = if (currentTier == PlanTier.PRO && currentCycle == ProPlanCycle.YEARLY_799) "ACTIVE" else "BEST VALUE • SAVE 33%",
                    isSelected = currentCycle == ProPlanCycle.YEARLY_799,
                    onClick = { onSelectPlan(ProPlanCycle.YEARLY_799) }
                )
            }
        }
    }
}

@Composable
fun BreakInAlertsScreen(
    alerts: List<SecurityAlertLog>,
    onSendGmailAlert: (SecurityAlertLog) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    VaultGradientBackground {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Column {
                        Text("Intruder & Break-In Alerts", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                        Text("3 fails: Selfie • 10 fails: Gmail #1 • 15 fails: Gmail #2 • 20 fails: Wipe", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }

            if (alerts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = VaultSurface),
                        border = BorderStroke(1.dp, VaultCardBorder),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Security, null, tint = SafeEmerald, modifier = Modifier.size(44.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("No break-in attempts recorded", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Your vault is completely secure.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }
            } else {
                items(alerts, key = { it.id }) { alert ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = VaultSurface),
                        border = BorderStroke(1.dp, AlertCrimson.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = AlertType.fromRaw(alert.alertType).title,
                                style = MaterialTheme.typography.titleMedium,
                                color = AlertCrimson
                            )
                            Text(alert.detail, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text(
                                text = "Location: ${alert.locationText.ifBlank { "Unavailable" }}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            if (alert.selfieLocalPath.isNotBlank()) {
                                val file = File(context.filesDir, alert.selfieLocalPath)
                                if (file.exists()) {
                                    val bmp = remember(file.absolutePath) {
                                        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                                    }
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp,
                                            contentDescription = "Break-in intruder selfie",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                        )
                                    }
                                }
                            }
                            if (alert.emailAlertTarget.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { onSendGmailAlert(alert) },
                                    border = BorderStroke(1.dp, VaultCardBorder)
                                ) {
                                    Icon(Icons.Default.Email, null, tint = VioletSoft)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Dispatch Gmail Alert (${alert.emailAlertTarget})", color = VioletSoft)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = VioletSoft,
        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = VioletSoft, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = VioletPrimary
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = VioletSoft,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
    }
}

@Composable
private fun ProBenefitRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, null, tint = SafeEmerald, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

@Composable
private fun PlanOptionCard(
    title: String,
    price: String,
    badge: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VaultSurfaceElevated),
        border = BorderStroke(1.5.dp, if (isSelected) ProGold else VaultCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Surface(
                    color = ProGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = ProGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text(price, style = MaterialTheme.typography.headlineMedium, color = VioletSoft)
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) SafeEmerald else VioletPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isSelected) "Active" else "Upgrade")
            }
        }
    }
}

@Composable
private fun MasterPasswordPromptDialog(
    title: String,
    description: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurface,
        title = { Text(title, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(description, color = TextSecondary)
                OutlinedTextField(
                    value = password,
                    onValueChange = { if (it.length <= 16) password = it },
                    label = { Text("Master PIN / Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = password.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
