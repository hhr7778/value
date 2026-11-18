package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Supported vault file categories for auto-sorting and browsing.
 */
enum class VaultCategory(val label: String) {
    PHOTOS("Photos"),
    VIDEOS("Videos"),
    MUSIC("Music"),
    PDFS("PDFs"),
    DOCUMENTS("Documents"),
    OTHERS("Others");

    companion object {
        fun fromRaw(raw: String?): VaultCategory =
            entries.firstOrNull { it.name == raw } ?: OTHERS

        /**
         * Automatically assigns a vault category based on file extension.
         * Supports: JPG/PNG/HEIC, MP4/MKV/MOV, MP3/WAV/M4A, PDF, DOC/DOCX/XLS/XLSX/TXT, ZIP/APK.
         */
        fun autoSortForExtension(ext: String): VaultCategory {
            return when (ext.lowercase().trimStart('.')) {
                "jpg", "jpeg", "png", "heic", "heif", "webp", "gif", "bmp" -> PHOTOS
                "mp4", "mkv", "mov", "avi", "webm", "3gp" -> VIDEOS
                "mp3", "wav", "m4a", "aac", "flac", "ogg" -> MUSIC
                "pdf" -> PDFS
                "doc", "docx", "xls", "xlsx", "txt", "csv", "ppt", "pptx", "rtf" -> DOCUMENTS
                else -> OTHERS // ZIP, APK, etc.
            }
        }
    }
}

enum class PlanTier {
    FREE, PRO;

    companion object {
        fun fromRaw(raw: String?): PlanTier =
            entries.firstOrNull { it.name == raw } ?: FREE
    }
}

enum class ProPlanCycle(val label: String, val priceText: String) {
    NONE("Free Plan", "0 Taka"),
    MONTHLY_99("Pro Monthly", "99 Taka / Month"),
    YEARLY_799("Pro Yearly", "799 Taka / Year");

    companion object {
        fun fromRaw(raw: String?): ProPlanCycle =
            entries.firstOrNull { it.name == raw } ?: NONE
    }
}

enum class AlertType(val title: String) {
    BREAK_IN_SELFIE("Break-in Selfie Captured"),
    WARNING_EMAIL_10("Security Warning (10 Attempts)"),
    CRITICAL_WARNING_15("Critical Warning + Selfie (15 Attempts)"),
    VAULT_WIPED_20("Vault Auto-Wiped (20 Attempts)"),
    PANIC_WIPE("Panic Password Activated");

    companion object {
        fun fromRaw(raw: String?): AlertType =
            entries.firstOrNull { it.name == raw } ?: BREAK_IN_SELFIE
    }
}

enum class SortOrder(val label: String) {
    DATE_DESC("Newest First"),
    NAME_ASC("Name (A-Z)"),
    SIZE_DESC("Largest First")
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

/**
 * Stored at /users/{userId} in Firestore.
 * Note: Only salted PBKDF2-HMAC-SHA256 hashes are stored; raw PINs are never persisted.
 */
@Keep
@IgnoreExtraProperties
data class UserSecurityConfig(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val masterPinHash: String = "",
    val decoyPinHash: String = "",
    val panicPinHash: String = "",
    val calculatorCode: String = "7788",
    val fingerprintEnabled: Boolean = true,
    val faceLockEnabled: Boolean = false,
    val doubleSecurityMode: Boolean = false,
    val fastMode: Boolean = true,
    val calculatorModeEnabled: Boolean = false,
    val autoBackupEnabled: Boolean = false,
    val autoLockSeconds: Long = 60L,
    val wrongAttempts: Long = 0L,
    val planTier: String = PlanTier.FREE.name,
    val proPlanCycle: String = ProPlanCycle.NONE.name,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

/**
 * Stored at /users/{userId}/folders/{folderId} in Firestore.
 */
@Keep
@IgnoreExtraProperties
data class VaultFolder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = VaultCategory.OTHERS.name,
    val colorHex: String = "#8B5CF6",
    val iconKey: String = "folder",
    val systemFolder: Boolean = false,
    val locked: Boolean = false,
    val folderPinHash: String = "",
    val decoyOnly: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

/**
 * Stored at /users/{userId}/files/{fileId} in Firestore.
 * Encrypted file bytes live privately on device inside filesDir/vault_blobs/.
 */
@Keep
@IgnoreExtraProperties
data class VaultFileMeta(
    @DocumentId val id: String = "",
    val userId: String = "",
    val folderId: String = "",
    val name: String = "",
    val extension: String = "",
    val mimeType: String = "application/octet-stream",
    val category: String = VaultCategory.OTHERS.name,
    val sizeBytes: Long = 0L,
    val durationMs: Long = 0L,
    val encryptedBlobPath: String = "",
    val sha256Checksum: String = "",
    val backedUp: Boolean = false,
    val decoyItem: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

/**
 * Stored at /users/{userId}/securityAlerts/{alertId} in Firestore.
 */
@Keep
@IgnoreExtraProperties
data class SecurityAlertLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val alertType: String = AlertType.BREAK_IN_SELFIE.name,
    val attemptCount: Long = 0L,
    val detail: String = "",
    val locationText: String = "",
    val selfieLocalPath: String = "",
    val emailAlertTarget: String = "",
    val createdAt: Timestamp? = null
)

/**
 * Stored at /users/{userId}/backups/{backupId} in Firestore.
 */
@Keep
@IgnoreExtraProperties
data class CloudBackupRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "",
    val itemCount: Long = 0L,
    val totalBytes: Long = 0L,
    val encryptionAlgo: String = "AES-256-GCM-PBKDF2",
    val saltBase64: String = "",
    val ivBase64: String = "",
    val encryptedManifestBase64: String = "",
    val createdAt: Timestamp? = null
)

const val FREE_TIER_LIMIT_BYTES: Long = 2L * 1024L * 1024L * 1024L // 2 GB

fun formatBytesReadable(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> String.format(java.util.Locale.US, "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(java.util.Locale.US, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(java.util.Locale.US, "%.1f KB", bytes / kb)
        else -> "$bytes B"
    }
}
