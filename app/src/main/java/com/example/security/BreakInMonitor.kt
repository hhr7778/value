package com.example.security

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

data class BreakInEvidence(
    val selfieLocalPath: String,
    val locationText: String
)

/**
 * Handles silent front-camera Break-In Selfie capture (CameraX), location lookup,
 * and Gmail alert intent generation when wrong PIN attempts hit security thresholds (3, 10, 15).
 */
class BreakInMonitor(
    private val context: Context,
    private val cryptoManager: VaultCryptoManager
) {

    suspend fun captureBreakInEvidence(lifecycleOwner: LifecycleOwner?): BreakInEvidence {
        val location = fetchLastLocationOrFallback()
        val selfiePath = if (lifecycleOwner != null) {
            captureFrontSelfieIfPermitted(lifecycleOwner)
        } else {
            ""
        }
        return BreakInEvidence(
            selfieLocalPath = selfiePath,
            locationText = location
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchLastLocationOrFallback(): String {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            return "Location permission not granted"
        }

        return withTimeoutOrNull(2500L) {
            suspendCancellableCoroutine { cont ->
                val client = LocationServices.getFusedLocationProviderClient(context)
                client.lastLocation
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            val formatted = String.format(
                                Locale.US,
                                "%.5f, %.5f",
                                loc.latitude,
                                loc.longitude
                            )
                            if (cont.isActive) cont.resume(formatted)
                        } else {
                            if (cont.isActive) cont.resume("Location unavailable on device")
                        }
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume("Location unavailable")
                    }
            }
        } ?: "Location lookup timed out"
    }

    private suspend fun captureFrontSelfieIfPermitted(lifecycleOwner: LifecycleOwner): String {
        val hasCamera = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCamera) return ""

        return withTimeoutOrNull(4500L) {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { cont ->
                    try {
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            try {
                                val provider = cameraProviderFuture.get()
                                val imageCapture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()

                                val selector = CameraSelector.DEFAULT_FRONT_CAMERA
                                if (!provider.hasCamera(selector)) {
                                    if (cont.isActive) cont.resume("")
                                    return@addListener
                                }

                                provider.unbindAll()
                                provider.bindToLifecycle(lifecycleOwner, selector, imageCapture)

                                val outFile = File(
                                    cryptoManager.intrudersDir,
                                    "intruder_${System.currentTimeMillis()}.jpg"
                                )
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(outFile).build()

                                imageCapture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            provider.unbindAll()
                                            val relPath = "intruders/${outFile.name}"
                                            if (cont.isActive) cont.resume(relPath)
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            provider.unbindAll()
                                            Log.w(TAG, "Front selfie capture failed: ${exception.message}")
                                            if (cont.isActive) cont.resume("")
                                        }
                                    }
                                )
                            } catch (e: Exception) {
                                Log.w(TAG, "CameraX bind error: ${e.message}")
                                if (cont.isActive) cont.resume("")
                            }
                        }, ContextCompat.getMainExecutor(context))
                    } catch (e: Exception) {
                        Log.w(TAG, "CameraProvider error: ${e.message}")
                        if (cont.isActive) cont.resume("")
                    }
                }
            }
        } ?: ""
    }

    /**
     * Creates an email intent targeting the user's Gmail address with break-in details.
     */
    fun buildGmailAlertIntent(
        recipientEmail: String,
        attemptCount: Int,
        locationText: String,
        detail: String
    ): Intent {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val body = buildString {
            appendLine("Gopon Vault Security Alert")
            appendLine("--------------------------")
            appendLine("Timestamp: $timestamp")
            appendLine("Failed Attempts: $attemptCount / 20")
            appendLine("Approximate Location: $locationText")
            appendLine("Event: $detail")
            appendLine()
            appendLine("If this was not you, open Gopon Vault immediately to review intruder photos.")
        }
        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            putExtra(Intent.EXTRA_SUBJECT, "Gopon Vault Alert: $attemptCount Wrong Unlock Attempts")
            putExtra(Intent.EXTRA_TEXT, body)
        }
    }

    companion object {
        private const val TAG = "BreakInMonitor"
    }
}
