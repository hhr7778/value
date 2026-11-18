package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.GoogleAuthHelper
import com.example.security.BiometricAvailability
import com.example.security.BiometricHelper
import com.example.ui.components.PinPadKeypad
import com.example.ui.components.VaultGradientBackground
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VioletGlow
import com.example.ui.theme.VioletPrimary
import com.example.ui.theme.VioletSoft

@Composable
fun SplashScreen() {
    val transition = rememberInfiniteTransition(label = "splash_pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    VaultGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .scale(pulse)
                    .clip(RoundedCornerShape(32.dp))
                    .border(2.dp, VioletPrimary.copy(alpha = 0.6f), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.vault_launcher_icon_1794988887039),
                    contentDescription = "Gopon Vault Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Gopon Vault",
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "AES-256 Zero-Knowledge Personal Vault",
                style = MaterialTheme.typography.bodyLarge,
                color = VioletSoft
            )
            Spacer(modifier = Modifier.height(28.dp))
            CircularProgressIndicator(
                color = VioletGlow,
                strokeWidth = 3.dp,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    VaultGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Image(
                        painter = painterResource(id = R.drawable.img_vault_banner_1794988908018),
                        contentDescription = "Gopon Vault Shield Artwork",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, VaultCardBorder, RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Gopon Vault",
                        style = MaterialTheme.typography.displayLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Lock photos, videos, audio, PDFs & private documents behind AES-256 encryption, biometrics, decoy vault & break-in protection.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    SecurityHighlightCard(
                        icon = Icons.Default.EnhancedEncryption,
                        title = "100% Private Local Encryption",
                        subtitle = "Files never appear in your phone gallery"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SecurityHighlightCard(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric + Break-In Selfie",
                        subtitle = "Fingerprint, Face Lock, Decoy Vault & Calculator Mode"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SecurityHighlightCard(
                        icon = Icons.Default.Shield,
                        title = "Encrypted Gmail Cloud Sync",
                        subtitle = "Restorable only with your Master Password"
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = AlertCrimson.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AlertCrimson.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFCA5A5),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            GoogleAuthHelper.onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("google_sign_in_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign in with Google (Gmail)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SecurityHighlightCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VaultSurface.copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, VaultCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VioletPrimary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = VioletSoft, modifier = Modifier.size(20.dp))
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
fun MasterPinSetupScreen(
    userEmail: String,
    onPinConfirmed: (String) -> Unit
) {
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirmStep by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val activePin = if (isConfirmStep) confirmPin else firstPin

    VaultGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = VioletPrimary.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = userEmail.ifBlank { "Signed In" },
                        style = MaterialTheme.typography.labelMedium,
                        color = VioletSoft,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = VioletGlow,
                    modifier = Modifier.size(38.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isConfirmStep) "Confirm Master PIN" else "Create Master PIN",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isConfirmStep) "Re-enter your 4-6 digit Master PIN" else "Choose a 4-6 digit Master PIN to encrypt your vault",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(22.dp))

                // 6-slot PIN indicator dots
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (i in 0 until 6) {
                        val filled = i < activePin.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (filled) VioletGlow else Color(0xFF2B2147))
                        )
                    }
                }
                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMsg ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AlertCrimson
                    )
                }
            }

            PinPadKeypad(
                onDigit = { digit ->
                    errorMsg = null
                    if (activePin.length < 6) {
                        if (isConfirmStep) confirmPin += digit else firstPin += digit
                    }
                },
                onBackspace = {
                    errorMsg = null
                    if (isConfirmStep && confirmPin.isNotEmpty()) {
                        confirmPin = confirmPin.dropLast(1)
                    } else if (!isConfirmStep && firstPin.isNotEmpty()) {
                        firstPin = firstPin.dropLast(1)
                    }
                },
                onClear = {
                    errorMsg = null
                    if (isConfirmStep) confirmPin = "" else firstPin = ""
                }
            )

            Row(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isConfirmStep) {
                    OutlinedButton(
                        onClick = {
                            isConfirmStep = false
                            confirmPin = ""
                            errorMsg = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Back")
                    }
                }
                Button(
                    onClick = {
                        if (!isConfirmStep) {
                            if (firstPin.length < 4) {
                                errorMsg = "Enter at least 4 digits"
                            } else {
                                isConfirmStep = true
                            }
                        } else {
                            if (confirmPin != firstPin) {
                                errorMsg = "PINs do not match. Try again."
                                confirmPin = ""
                            } else {
                                onPinConfirmed(firstPin)
                            }
                        }
                    },
                    enabled = activePin.length >= 4,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("confirm_master_pin_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
                ) {
                    Text(if (isConfirmStep) "Save Master PIN" else "Continue")
                }
            }
        }
    }
}

@Composable
fun BiometricSetupScreen(
    isBusy: Boolean,
    onEnableBiometric: (fingerprint: Boolean, faceLock: Boolean) -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val caps = remember(context) { BiometricHelper.checkCapabilities(context) }
    val available = caps.availability == BiometricAvailability.AVAILABLE

    VaultGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.widthIn(max = 480.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .clip(CircleShape)
                        .background(VioletPrimary.copy(alpha = 0.16f))
                        .border(2.dp, VioletGlow.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric setup icon",
                        tint = VioletGlow,
                        modifier = Modifier.size(64.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Fingerprint & Face Lock",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (available) {
                        "Enable biometric authentication to unlock Gopon Vault faster and protect exports & deletions."
                    } else {
                        "No biometric hardware enrolled on this device. Gopon Vault will automatically fall back to your Master PIN."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = VaultSurface),
                        border = BorderStroke(1.dp, VaultCardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Fingerprint, null, tint = SafeEmerald)
                            Spacer(Modifier.height(6.dp))
                            Text("Fingerprint", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (caps.hasFingerprintHardware || available) "Supported" else "PIN Fallback",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = VaultSurface),
                        border = BorderStroke(1.dp, VaultCardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Face, null, tint = CyanAccent)
                            Spacer(Modifier.height(6.dp))
                            Text("Face Lock", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (caps.hasFaceHardware) "Supported" else "Auto / PIN",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        onEnableBiometric(available, available && caps.hasFaceHardware)
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("enable_biometric_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            text = if (available) "Enable Biometric Lock" else "Continue with Master PIN",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = onSkip,
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("skip_biometric_button")
                ) {
                    Text(
                        text = "Skip for now",
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
