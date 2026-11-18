package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.security.BiometricAvailability
import com.example.security.BiometricHelper
import com.example.ui.components.PinPadKeypad
import com.example.ui.components.VaultGradientBackground
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ProGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultObsidian
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.theme.VioletGlow
import com.example.ui.theme.VioletPrimary
import com.example.ui.theme.VioletSoft

/**
 * Dedicated Fingerprint Unlock Page:
 * Big glowing fingerprint icon in center with pulsing scan animation,
 * "Place your finger" prompt, and automatic fallback to Master PIN after 3 failed attempts.
 */
@Composable
fun FingerprintUnlockScreen(
    failedAttempts: Int,
    doubleSecurityEnabled: Boolean,
    onBiometricSuccess: () -> Unit,
    onBiometricFailedAttempt: () -> Unit,
    onUseMasterPin: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val transition = rememberInfiniteTransition(label = "fp_scan")
    val ringScale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_scale"
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha"
    )

    fun triggerPrompt() {
        val act = activity ?: return
        val caps = BiometricHelper.checkCapabilities(act)
        if (caps.availability != BiometricAvailability.AVAILABLE) {
            onUseMasterPin()
            return
        }
        BiometricHelper.showBiometricPrompt(
            activity = act,
            title = "Unlock Gopon Vault",
            subtitle = "Place your finger or look at camera",
            negativeButtonText = "Use Master PIN",
            onSuccess = onBiometricSuccess,
            onFailedAttempt = onBiometricFailedAttempt,
            onErrorOrFallback = { _ -> }
        )
    }

    LaunchedEffect(Unit) {
        triggerPrompt()
    }

    VaultGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = VaultSurfaceElevated,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, VaultCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = VioletSoft,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (doubleSecurityEnabled) "Double Security Active (1/2)" else "Gopon Vault Locked",
                            style = MaterialTheme.typography.labelMedium,
                            color = VioletSoft
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .clickable { triggerPrompt() }
                        .testTag("fingerprint_scan_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = VioletGlow.copy(alpha = ringAlpha),
                            radius = (size.minDimension / 2f) * ringScale * 0.85f,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawCircle(
                            color = VioletPrimary.copy(alpha = 0.25f),
                            radius = size.minDimension * 0.34f,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(116.dp)
                            .clip(CircleShape)
                            .background(VioletPrimary.copy(alpha = 0.2f))
                            .border(2.dp, VioletGlow, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Place your finger",
                            tint = VioletSoft,
                            modifier = Modifier.size(68.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Place your finger",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (failedAttempts > 0) {
                        "Attempt $failedAttempts/3 failed • Tap sensor to retry"
                    } else {
                        "Tap the scanner to authenticate with Fingerprint or Face Lock"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (failedAttempts > 0) AlertCrimson else TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            OutlinedButton(
                onClick = onUseMasterPin,
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("switch_to_pin_button"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, VaultCardBorder)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = TextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use Master Password / PIN", color = TextPrimary)
            }
        }
    }
}

/**
 * Dark obsidian privacy PIN unlock screen inspired by the user's attached reference.
 */
@Composable
fun PinUnlockScreen(
    errorText: String?,
    awaitingSecondFactor: Boolean,
    biometricAvailable: Boolean,
    wrongAttempts: Int,
    onSubmitPin: (String, androidx.lifecycle.LifecycleOwner?) -> Unit,
    onUseBiometric: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var enteredPin by remember { mutableStateOf("") }
    var showForgotDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF06050A))
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 440.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = if (awaitingSecondFactor) {
                        "Double Security: Enter Master PIN"
                    } else {
                        "Enter privacy password"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorText ?: if (wrongAttempts > 0) {
                        "Incorrect attempts: $wrongAttempts / 20"
                    } else {
                        "Enter 4-6 digit Master, Decoy, or Panic PIN"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (errorText != null) AlertCrimson else TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("pin_error_text")
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (i in 0 until 6) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i < enteredPin.length) VioletGlow else Color(0xFF3A344B)
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = Color(0xFF1F1C2A),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { showForgotDialog = true }
                            .testTag("forgot_password_chip")
                    ) {
                        Text(
                            text = "Forgot Password",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    if (biometricAvailable && !awaitingSecondFactor) {
                        Surface(
                            color = VioletPrimary.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(onClick = onUseBiometric)
                                .testTag("use_biometric_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Use Fingerprint",
                                    tint = VioletSoft,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Fingerprint",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = VioletSoft
                                )
                            }
                        }
                    }
                }
            }

            PinPadKeypad(
                onDigit = { d ->
                    if (enteredPin.length < 6) {
                        enteredPin += d
                    }
                },
                onBackspace = {
                    if (enteredPin.isNotEmpty()) {
                        enteredPin = enteredPin.dropLast(1)
                    }
                },
                onClear = { enteredPin = "" }
            )

            Button(
                onClick = {
                    val candidate = enteredPin
                    enteredPin = ""
                    onSubmitPin(candidate, lifecycleOwner)
                },
                enabled = enteredPin.length >= 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("unlock_pin_submit_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
            ) {
                Text("Unlock Vault", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            containerColor = VaultSurface,
            title = { Text("Zero-Knowledge Recovery", color = TextPrimary) },
            text = {
                Text(
                    "Your Master PIN is stored as a salted PBKDF2-HMAC-SHA256 hash. To avoid bypassing security, unlock via Fingerprint (if enabled) or restore an AES-256 Cloud Backup using your original Master Password.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("OK", color = VioletSoft)
                }
            }
        )
    }
}

/**
 * Calculator Disguise Mode:
 * Looks and operates like a real calculator. Typing the secret Calculator Code + '=' opens Gopon Vault!
 */
@Composable
fun CalculatorDisguiseScreen(
    onSecretCodeEntered: (String) -> Boolean,
    isPanicLocked: Boolean = false
) {
    var display by remember { mutableStateOf("0") }
    var firstOperand by remember { mutableStateOf<Double?>(null) }
    var pendingOp by remember { mutableStateOf<String?>(null) }
    var resetOnNextDigit by remember { mutableStateOf(false) }

    fun pressDigit(d: String) {
        if (resetOnNextDigit || display == "0") {
            display = d
            resetOnNextDigit = false
        } else if (display.length < 12) {
            display += d
        }
    }

    fun pressOp(op: String) {
        firstOperand = display.toDoubleOrNull() ?: 0.0
        pendingOp = op
        resetOnNextDigit = true
    }

    fun pressEquals() {
        // Check secret vault code first (unless permanently panic-wiped)
        if (!isPanicLocked && pendingOp == null && onSecretCodeEntered(display)) {
            return
        }
        val a = firstOperand
        val b = display.toDoubleOrNull()
        if (a != null && b != null && pendingOp != null) {
            val res = when (pendingOp) {
                "+" -> a + b
                "-" -> a - b
                "×" -> a * b
                "÷" -> if (b == 0.0) Double.NaN else a / b
                else -> b
            }
            display = if (res.isNaN()) "Error"
            else if (res % 1.0 == 0.0 && kotlin.math.abs(res) < 1e12) res.toLong().toString()
            else String.format(java.util.Locale.US, "%.4f", res).trimEnd('0').trimEnd('.')
            firstOperand = null
            pendingOp = null
            resetOnNextDigit = true
        }
    }

    val buttonRows = listOf(
        listOf("AC", "±", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "⌫", "=")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08080C))
            .safeDrawingPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            if (isPanicLocked) {
                Surface(
                    color = Color(0xFF1A1A22),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Standard Mode", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            if (pendingOp != null && firstOperand != null) {
                Text(
                    text = "${firstOperand?.toString()?.removeSuffix(".0")} $pendingOp",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextSecondary
                )
            }
            Text(
                text = display,
                fontSize = 54.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("calculator_display")
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            buttonRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { key ->
                        val isOperator = key in listOf("÷", "×", "-", "+", "=")
                        val isTopAction = key in listOf("AC", "±", "%")
                        val bg = when {
                            key == "=" -> VioletPrimary
                            isOperator -> Color(0xFF2E2154)
                            isTopAction -> Color(0xFF23232E)
                            else -> Color(0xFF15151C)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(bg)
                                .clickable {
                                    when (key) {
                                        "AC" -> {
                                            display = "0"
                                            firstOperand = null
                                            pendingOp = null
                                            resetOnNextDigit = false
                                        }
                                        "⌫" -> {
                                            display = if (display.length <= 1) "0" else display.dropLast(1)
                                        }
                                        "±" -> {
                                            display = if (display.startsWith("-")) display.drop(1)
                                            else if (display != "0") "-$display" else display
                                        }
                                        "%" -> {
                                            val v = display.toDoubleOrNull() ?: 0.0
                                            display = (v / 100.0).toString()
                                        }
                                        "." -> {
                                            if (!display.contains(".")) display += "."
                                        }
                                        "÷", "×", "-", "+" -> pressOp(key)
                                        "=" -> pressEquals()
                                        else -> pressDigit(key)
                                    }
                                }
                                .testTag("calc_btn_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isOperator || key == "=") Color.White else if (isTopAction) CyanAccent else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
