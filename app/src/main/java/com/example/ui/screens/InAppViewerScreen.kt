package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.model.VaultCategory
import com.example.model.VaultFileMeta
import com.example.model.formatBytesReadable
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
import kotlinx.coroutines.delay
import java.io.File

/**
 * Private In-App Viewer for encrypted photos, videos, audio, PDFs, and text documents.
 * Files are decrypted strictly in memory or app-private cache and never exposed to device gallery.
 */
@Composable
fun InAppViewerScreen(
    file: VaultFileMeta,
    decryptBytes: suspend (VaultFileMeta) -> Result<ByteArray>,
    decryptCacheFile: suspend (VaultFileMeta) -> Result<File>,
    onShareWithBiometric: (VaultFileMeta) -> Unit,
    onDeleteWithBiometric: (VaultFileMeta) -> Unit,
    onClose: () -> Unit
) {
    val category = VaultCategory.fromRaw(file.category)
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var decryptedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var tempFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(file.id) {
        isLoading = true
        errorMessage = null
        when (category) {
            VaultCategory.PHOTOS, VaultCategory.DOCUMENTS, VaultCategory.OTHERS -> {
                decryptBytes(file).fold(
                    onSuccess = { decryptedBytes = it },
                    onFailure = { errorMessage = it.localizedMessage ?: "Decryption failed" }
                )
            }
            VaultCategory.VIDEOS, VaultCategory.MUSIC, VaultCategory.PDFS -> {
                decryptCacheFile(file).fold(
                    onSuccess = { tempFile = it },
                    onFailure = { errorMessage = it.localizedMessage ?: "Decryption failed" }
                )
            }
        }
        isLoading = false
    }

    DisposableEffect(file.id) {
        onDispose {
            // Wipe decrypted temp cache file immediately when leaving viewer
            tempFile?.let { runCatching { it.delete() } }
        }
    }

    VaultGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            // Viewer Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("viewer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close viewer",
                            tint = TextPrimary
                        )
                    }
                    Column {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "In-App Private View • ${formatBytesReadable(file.sizeBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SafeEmerald
                        )
                    }
                }
                Row {
                    IconButton(
                        onClick = { onShareWithBiometric(file) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("viewer_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share with Fingerprint", tint = VioletSoft)
                    }
                    IconButton(
                        onClick = { onDeleteWithBiometric(file) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("viewer_delete_button")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete with Fingerprint", tint = AlertCrimson)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> CircularProgressIndicator(color = VioletGlow)
                    errorMessage != null -> Text(
                        text = errorMessage ?: "Error",
                        color = AlertCrimson,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    category == VaultCategory.PHOTOS && decryptedBytes != null -> {
                        ZoomableImageViewer(bytes = decryptedBytes!!)
                    }
                    category == VaultCategory.VIDEOS && tempFile != null -> {
                        InAppVideoPlayer(file = tempFile!!)
                    }
                    category == VaultCategory.MUSIC && tempFile != null -> {
                        InAppAudioPlayer(file = tempFile!!, fileName = file.name)
                    }
                    category == VaultCategory.PDFS && tempFile != null -> {
                        InAppPdfViewer(file = tempFile!!)
                    }
                    decryptedBytes != null -> {
                        DocumentOrBinaryPreview(file = file, bytes = decryptedBytes!!)
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableImageViewer(bytes: ByteArray) {
    val bitmap = remember(bytes) {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    if (bitmap == null) {
        Text("Unable to decode image.", color = TextSecondary)
        return
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 4f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = "Encrypted vault photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )
    }
}

@Composable
private fun InAppVideoPlayer(file: File) {
    val context = LocalContext.current
    val exoPlayer = remember(file.absolutePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
    )
}

@Composable
private fun InAppAudioPlayer(file: File, fileName: String) {
    val context = LocalContext.current
    val exoPlayer = remember(file.absolutePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()
        }
    }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(1L) }

    LaunchedEffect(exoPlayer) {
        while (true) {
            isPlaying = exoPlayer.isPlaying
            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            durationMs = exoPlayer.duration.coerceAtLeast(1L)
            delay(250L)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VaultSurface),
        border = BorderStroke(1.dp, VaultCardBorder),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(CyanAccent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AudioFile, null, tint = CyanAccent, modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(fileName, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Encrypted In-App Audio Player",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(20.dp))
            Slider(
                value = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
                onValueChange = { ratio ->
                    exoPlayer.seekTo((ratio * durationMs).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = VioletGlow,
                    activeTrackColor = VioletPrimary
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${positionMs / 1000}s", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text("${durationMs / 1000}s", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }
        }
    }
}

@Composable
private fun InAppPdfViewer(file: File) {
    var pageIndex by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(1) }
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    DisposableEffect(file.absolutePath, pageIndex) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            pageCount = renderer.pageCount.coerceAtLeast(1)
            val safeIdx = pageIndex.coerceIn(0, pageCount - 1)
            renderer.openPage(safeIdx).use { page ->
                val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                renderedBitmap = bmp
            }
        } catch (_: Exception) {
            renderedBitmap = null
        }
        onDispose {
            runCatching { renderer?.close() }
            runCatching { pfd?.close() }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF140E24)),
            contentAlignment = Alignment.Center
        ) {
            val bmp = renderedBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "PDF Page ${pageIndex + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("Unable to render PDF page.", color = TextSecondary)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            color = VaultSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, VaultCardBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (pageIndex > 0) pageIndex-- },
                    enabled = pageIndex > 0
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous page")
                }
                Text(
                    text = "Page ${pageIndex + 1} / $pageCount",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                IconButton(
                    onClick = { if (pageIndex < pageCount - 1) pageIndex++ },
                    enabled = pageIndex < pageCount - 1
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next page")
                }
            }
        }
    }
}

@Composable
private fun DocumentOrBinaryPreview(file: VaultFileMeta, bytes: ByteArray) {
    val isReadableText = file.extension.lowercase() in setOf("txt", "csv", "json", "md", "log")
    val previewText = remember(bytes, isReadableText) {
        if (isReadableText) String(bytes.take(32_000).toByteArray(), Charsets.UTF_8) else ""
    }

    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = VaultSurface),
        border = BorderStroke(1.dp, VaultCardBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EnhancedEncryption, null, tint = SafeEmerald)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "AES-256-GCM Decrypted Preview",
                    style = MaterialTheme.typography.titleMedium,
                    color = SafeEmerald
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (isReadableText) {
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            } else {
                Text(
                    text = "File: ${file.name}\nType: ${file.mimeType}\nSize: ${formatBytesReadable(file.sizeBytes)}\nSHA-256: ${file.sha256Checksum}\n\nThis archive/document is encrypted inside Gopon Vault. Tap Share (Fingerprint) above to export securely.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
        }
    }
}
