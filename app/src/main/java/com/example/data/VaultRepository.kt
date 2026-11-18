package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.AlertType
import com.example.model.CloudBackupRecord
import com.example.model.FREE_TIER_LIMIT_BYTES
import com.example.model.PlanTier
import com.example.model.ProPlanCycle
import com.example.model.SecurityAlertLog
import com.example.model.UserSecurityConfig
import com.example.model.VaultCategory
import com.example.model.VaultFileMeta
import com.example.model.VaultFolder
import com.example.security.VaultCryptoManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Repository coordinating Firestore metadata (strictly scoped to /users/{uid}/...)
 * and private AES-256-GCM encrypted file storage on device.
 */
class VaultRepository(
    private val db: FirebaseFirestore,
    private val appContext: Context,
    val crypto: VaultCryptoManager = VaultCryptoManager(appContext),
    private val auth: FirebaseAuth = Firebase.auth
) {

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing vault.")
    }

    fun observeSecurityConfig(userId: String): Flow<UserSecurityConfig?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User ID mismatch" }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        UserSecurityConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeFolders(userId: String): Flow<List<VaultFolder>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User ID mismatch" }
        val path = "users/$userId/folders"
        emitAll(
            db.collection("users").document(userId).collection("folders")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VaultFolder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeFiles(userId: String): Flow<List<VaultFileMeta>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User ID mismatch" }
        val path = "users/$userId/files"
        emitAll(
            db.collection("users").document(userId).collection("files")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VaultFileMeta::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAlerts(userId: String): Flow<List<SecurityAlertLog>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User ID mismatch" }
        val path = "users/$userId/securityAlerts"
        emitAll(
            db.collection("users").document(userId).collection("securityAlerts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SecurityAlertLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeBackups(userId: String): Flow<List<CloudBackupRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User ID mismatch" }
        val path = "users/$userId/backups"
        emitAll(
            db.collection("users").document(userId).collection("backups")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudBackupRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Initializes the user's security config, auto-sorted + custom starter folders,
     * and encrypted sample files so first-time users have an immediately usable vault.
     */
    suspend fun initializeUserVault(
        userId: String,
        email: String,
        displayName: String,
        masterPin: String,
        fingerprintEnabled: Boolean,
        faceLockEnabled: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val path = "users/$userId"
        try {
            val uid = requireUserId()
            check(uid == userId)
            val masterHash = crypto.hashSecret(masterPin)
            val decoyHash = crypto.hashSecret("0000")
            val panicHash = crypto.hashSecret("9999")

            val configMap = mapOf(
                "userId" to uid,
                "email" to email.take(200),
                "displayName" to displayName.ifBlank { "Vault Owner" }.take(100),
                "masterPinHash" to masterHash,
                "decoyPinHash" to decoyHash,
                "panicPinHash" to panicHash,
                "calculatorCode" to "7788",
                "fingerprintEnabled" to fingerprintEnabled,
                "faceLockEnabled" to faceLockEnabled,
                "doubleSecurityMode" to false,
                "fastMode" to fingerprintEnabled,
                "calculatorModeEnabled" to false,
                "autoBackupEnabled" to false,
                "autoLockSeconds" to 60L,
                "wrongAttempts" to 0L,
                "planTier" to PlanTier.FREE.name,
                "proPlanCycle" to ProPlanCycle.NONE.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).set(configMap).await()

            seedStarterFoldersAndSamples(uid, displayName.ifBlank { "Vault Owner" })
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private suspend fun seedStarterFoldersAndSamples(uid: String, ownerName: String) {
        val batch = db.batch()
        val foldersCol = db.collection("users").document(uid).collection("folders")
        val filesCol = db.collection("users").document(uid).collection("files")

        val starterFolders = listOf(
            Triple("folder_photos", "Photos", Triple(VaultCategory.PHOTOS.name, "#8B5CF6", "photo")) to true,
            Triple("folder_videos", "Videos", Triple(VaultCategory.VIDEOS.name, "#EC4899", "video")) to true,
            Triple("folder_music", "Music", Triple(VaultCategory.MUSIC.name, "#22D3EE", "music")) to true,
            Triple("folder_pdfs", "PDFs", Triple(VaultCategory.PDFS.name, "#F59E0B", "pdf")) to true,
            Triple("folder_documents", "Documents", Triple(VaultCategory.DOCUMENTS.name, "#10B981", "doc")) to true,
            Triple("folder_others", "Others", Triple(VaultCategory.OTHERS.name, "#6366F1", "archive")) to true,
            Triple("folder_bank", "Bank Documents", Triple("CUSTOM", "#F59E0B", "bank")) to false,
            Triple("folder_personal", "Personal", Triple("CUSTOM", "#EC4899", "personal")) to false,
            Triple("folder_certificates", "Certificates", Triple("CUSTOM", "#22D3EE", "cert")) to false,
            Triple("folder_decoy_notes", "Public Notes", Triple(VaultCategory.DOCUMENTS.name, "#6366F1", "doc")) to false
        )

        starterFolders.forEach { (triple, isSystem) ->
            val (folderId, name, meta) = triple
            val (category, colorHex, iconKey) = meta
            val isDecoy = folderId == "folder_decoy_notes"
            batch.set(
                foldersCol.document(folderId),
                mapOf(
                    "userId" to uid,
                    "name" to name,
                    "category" to category,
                    "colorHex" to colorHex,
                    "iconKey" to iconKey,
                    "systemFolder" to isSystem,
                    "locked" to false,
                    "folderPinHash" to "",
                    "decoyOnly" to isDecoy,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        // Seed encrypted sample files so in-app viewers work immediately
        val sampleSpecs = listOf(
            SampleSeedSpec(
                id = "sample_vault_card",
                folderId = "folder_photos",
                name = "Vault_Pass_Card.png",
                ext = "png",
                mime = "image/png",
                category = VaultCategory.PHOTOS.name,
                durationMs = 0L,
                decoy = false,
                bytes = VaultSampleSeeder.createSampleVaultCardPng(
                    "Private Identity Card",
                    "Owner: $ownerName",
                    android.graphics.Color.parseColor("#8B5CF6")
                )
            ),
            SampleSeedSpec(
                id = "sample_cert_pdf",
                folderId = "folder_certificates",
                name = "Security_Certificate.pdf",
                ext = "pdf",
                mime = "application/pdf",
                category = VaultCategory.PDFS.name,
                durationMs = 0L,
                decoy = false,
                bytes = VaultSampleSeeder.createSampleCertificatePdf(ownerName)
            ),
            SampleSeedSpec(
                id = "sample_audio_memo",
                folderId = "folder_music",
                name = "Encrypted_Voice_Memo.wav",
                ext = "wav",
                mime = "audio/wav",
                category = VaultCategory.MUSIC.name,
                durationMs = 3000L,
                decoy = false,
                bytes = VaultSampleSeeder.createSampleVoiceNoteWav(3)
            ),
            SampleSeedSpec(
                id = "sample_bank_txt",
                folderId = "folder_bank",
                name = "Account_Recovery_Notes.txt",
                ext = "txt",
                mime = "text/plain",
                category = VaultCategory.DOCUMENTS.name,
                durationMs = 0L,
                decoy = false,
                bytes = (
                    "Gopon Vault — Sample Encrypted Note\n" +
                        "------------------------------------\n" +
                        "This file is encrypted with AES-256-GCM inside app-private storage.\n" +
                        "Default Decoy PIN: 0000\n" +
                        "Default Panic PIN: 9999\n" +
                        "Default Calculator Code: 7788=\n"
                    ).toByteArray(Charsets.UTF_8)
            ),
            SampleSeedSpec(
                id = "decoy_sample_txt",
                folderId = "folder_decoy_notes",
                name = "Grocery_List.txt",
                ext = "txt",
                mime = "text/plain",
                category = VaultCategory.DOCUMENTS.name,
                durationMs = 0L,
                decoy = true,
                bytes = "Grocery List\n- Milk\n- Whole Wheat Bread\n- Coffee Beans\n".toByteArray(Charsets.UTF_8)
            )
        )

        sampleSpecs.forEach { spec ->
            val blobPath = crypto.encryptAndStoreBlob(spec.id, spec.bytes)
            val sha = crypto.sha256Hex(spec.bytes)
            batch.set(
                filesCol.document(spec.id),
                mapOf(
                    "userId" to uid,
                    "folderId" to spec.folderId,
                    "name" to spec.name,
                    "extension" to spec.ext,
                    "mimeType" to spec.mime,
                    "category" to spec.category,
                    "sizeBytes" to spec.bytes.size.toLong(),
                    "durationMs" to spec.durationMs,
                    "encryptedBlobPath" to blobPath,
                    "sha256Checksum" to sha,
                    "backedUp" to false,
                    "decoyItem" to spec.decoy,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        batch.commit().await()
    }

    /**
     * Imports a file from Photo Picker, Camera, File Manager, or Google Drive,
     * enforces Free-tier 2GB quota, auto-sorts by extension, and encrypts with AES-256-GCM.
     */
    suspend fun importFromUri(
        userId: String,
        uri: Uri,
        explicitFolderId: String? = null,
        currentUsedBytes: Long,
        planTier: PlanTier
    ): Result<VaultFileMeta> = withContext(Dispatchers.IO) {
        val fileId = "vf_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val path = "users/$userId/files/$fileId"
        try {
            val uid = requireUserId()
            check(uid == userId)

            val rawName = resolveDisplayName(uri) ?: "vault_item_${System.currentTimeMillis()}"
            val safeName = rawName.take(150)
            val ext = safeName.substringAfterLast('.', "bin").lowercase().take(12)
            val mime = appContext.contentResolver.getType(uri) ?: guessMimeType(ext)
            val category = VaultCategory.autoSortForExtension(ext)

            val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Unable to read selected file.")

            if (planTier == PlanTier.FREE && currentUsedBytes + bytes.size > FREE_TIER_LIMIT_BYTES) {
                throw IllegalStateException("Free 2 GB vault quota exceeded. Upgrade to Gopon Pro for unlimited storage.")
            }

            val folderId = explicitFolderId ?: defaultFolderIdFor(category)
            val blobPath = crypto.encryptAndStoreBlob(fileId, bytes)
            val checksum = crypto.sha256Hex(bytes)

            val payload = mapOf(
                "userId" to uid,
                "folderId" to folderId,
                "name" to safeName,
                "extension" to ext,
                "mimeType" to mime.take(100),
                "category" to category.name,
                "sizeBytes" to bytes.size.toLong(),
                "durationMs" to 0L,
                "encryptedBlobPath" to blobPath,
                "sha256Checksum" to checksum,
                "backedUp" to false,
                "decoyItem" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("files").document(fileId)
                .set(payload).await()

            Result.success(
                VaultFileMeta(
                    id = fileId,
                    userId = uid,
                    folderId = folderId,
                    name = safeName,
                    extension = ext,
                    mimeType = mime,
                    category = category.name,
                    sizeBytes = bytes.size.toLong(),
                    encryptedBlobPath = blobPath,
                    sha256Checksum = checksum
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Decrypts a vault file into a private temporary file in cacheDir for Media3 / PdfRenderer / Share.
     */
    suspend fun decryptToPrivateCacheFile(fileMeta: VaultFileMeta): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                val plainBytes = crypto.decryptBlob(fileMeta.encryptedBlobPath)
                val safeExt = fileMeta.extension.ifBlank { "bin" }
                val outFile = File(appContext.cacheDir, "vault_view_${fileMeta.id}.$safeExt")
                outFile.writeBytes(plainBytes)
                Result.success(outFile)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun decryptToBytes(fileMeta: VaultFileMeta): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(crypto.decryptBlob(fileMeta.encryptedBlobPath))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun createCustomFolder(
        userId: String,
        name: String,
        colorHex: String,
        iconKey: String,
        folderPin: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val folderId = "fld_${UUID.randomUUID().toString().replace("-", "").take(18)}"
        val path = "users/$userId/folders/$folderId"
        try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanName = name.trim().take(60)
            require(cleanName.isNotEmpty()) { "Folder name cannot be empty." }

            val pinHash = if (!folderPin.isNullOrBlank()) crypto.hashSecret(folderPin) else ""
            val payload = mapOf(
                "userId" to uid,
                "name" to cleanName,
                "category" to "CUSTOM",
                "colorHex" to colorHex.take(16),
                "iconKey" to iconKey.take(30),
                "systemFolder" to false,
                "locked" to pinHash.isNotEmpty(),
                "folderPinHash" to pinHash,
                "decoyOnly" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("folders").document(folderId)
                .set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateFolderLock(
        userId: String,
        folderId: String,
        locked: Boolean,
        newPin: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val path = "users/$userId/folders/$folderId"
        try {
            val uid = requireUserId()
            check(uid == userId)
            val hash = if (locked && !newPin.isNullOrBlank()) crypto.hashSecret(newPin) else ""
            db.collection("users").document(uid).collection("folders").document(folderId)
                .update(
                    mapOf(
                        "locked" to locked,
                        "folderPinHash" to hash,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteFile(userId: String, fileMeta: VaultFileMeta): Result<Unit> =
        withContext(Dispatchers.IO) {
            val path = "users/$userId/files/${fileMeta.id}"
            try {
                val uid = requireUserId()
                check(uid == userId)
                crypto.deleteBlob(fileMeta.encryptedBlobPath)
                db.collection("users").document(uid).collection("files").document(fileMeta.id)
                    .delete().await()
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, path)
                Result.failure(e)
            }
        }

    suspend fun updateSecurityFields(
        userId: String,
        updates: Map<String, Any>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val path = "users/$userId"
        try {
            val uid = requireUserId()
            check(uid == userId)
            val mutable = updates.toMutableMap()
            mutable["updatedAt"] = FieldValue.serverTimestamp()
            db.collection("users").document(uid).update(mutable).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun logSecurityAlert(
        userId: String,
        alertType: AlertType,
        attemptCount: Int,
        detail: String,
        locationText: String,
        selfieLocalPath: String,
        emailTarget: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val alertId = "alt_${UUID.randomUUID().toString().replace("-", "").take(18)}"
        val path = "users/$userId/securityAlerts/$alertId"
        try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("securityAlerts").document(alertId)
                .set(
                    mapOf(
                        "userId" to uid,
                        "alertType" to alertType.name,
                        "attemptCount" to attemptCount.toLong(),
                        "detail" to detail.take(400),
                        "locationText" to locationText.take(200),
                        "selfieLocalPath" to selfieLocalPath.take(300),
                        "emailAlertTarget" to emailTarget.take(200),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Permanently deletes all real vault files both on device and in Firestore.
     * Used for 20-wrong-attempt self-destruct or Panic Password instant wipe.
     */
    suspend fun wipeAllRealVaultFiles(
        userId: String,
        files: List<VaultFileMeta>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val path = "users/$userId/files"
        try {
            val uid = requireUserId()
            check(uid == userId)
            crypto.shredAllVaultFiles()
            val batch = db.batch()
            val filesCol = db.collection("users").document(uid).collection("files")
            files.filter { !it.decoyItem }.forEach { meta ->
                batch.delete(filesCol.document(meta.id))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    /**
     * Creates an AES-256-GCM encrypted backup snapshot encrypted with the user's Master Password
     * and stores it in Firestore /users/{uid}/backups.
     */
    suspend fun createCloudBackup(
        userId: String,
        masterPassword: String,
        files: List<VaultFileMeta>,
        folders: List<VaultFolder>
    ): Result<CloudBackupRecord> = withContext(Dispatchers.IO) {
        val backupId = "bak_${UUID.randomUUID().toString().replace("-", "").take(18)}"
        val path = "users/$userId/backups/$backupId"
        try {
            val uid = requireUserId()
            check(uid == userId)
            val realFiles = files.filter { !it.decoyItem }
            val manifest = JSONObject().apply {
                put("version", 1)
                put("createdEpochMs", System.currentTimeMillis())
                put("folders", JSONArray().apply {
                    folders.filter { !it.decoyOnly }.forEach { f ->
                        put(JSONObject().apply {
                            put("id", f.id)
                            put("name", f.name)
                            put("category", f.category)
                            put("colorHex", f.colorHex)
                            put("iconKey", f.iconKey)
                        })
                    }
                })
                put("files", JSONArray().apply {
                    realFiles.forEach { file ->
                        put(JSONObject().apply {
                            put("id", file.id)
                            put("name", file.name)
                            put("category", file.category)
                            put("sizeBytes", file.sizeBytes)
                            put("sha256", file.sha256Checksum)
                        })
                    }
                })
            }.toString()

            val (saltB64, ivB64, cipherB64) = crypto.encryptBackupWithMasterPassword(
                masterPassword = masterPassword,
                plainJson = manifest
            )

            val totalBytes = realFiles.sumOf { it.sizeBytes }
            val label = "Vault Backup (${realFiles.size} files)"

            db.collection("users").document(uid).collection("backups").document(backupId)
                .set(
                    mapOf(
                        "userId" to uid,
                        "label" to label,
                        "itemCount" to realFiles.size.toLong(),
                        "totalBytes" to totalBytes,
                        "encryptionAlgo" to "AES-256-GCM-PBKDF2",
                        "saltBase64" to saltB64,
                        "ivBase64" to ivB64,
                        "encryptedManifestBase64" to cipherB64,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            // Mark files as backedUp
            val filesCol = db.collection("users").document(uid).collection("files")
            realFiles.filter { !it.backedUp }.forEach { file ->
                filesCol.document(file.id).update(
                    mapOf(
                        "backedUp" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(
                CloudBackupRecord(
                    id = backupId,
                    userId = uid,
                    label = label,
                    itemCount = realFiles.size.toLong(),
                    totalBytes = totalBytes,
                    saltBase64 = saltB64,
                    ivBase64 = ivB64,
                    encryptedManifestBase64 = cipherB64
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Decrypts a CloudBackupRecord using the user's Master Password to verify integrity and restore summary.
     */
    fun decryptBackupRecord(masterPassword: String, record: CloudBackupRecord): Result<String> {
        return try {
            val plainJson = crypto.decryptBackupWithMasterPassword(
                masterPassword = masterPassword,
                saltBase64 = record.saltBase64,
                ivBase64 = record.ivBase64,
                cipherBase64 = record.encryptedManifestBase64
            )
            Result.success(plainJson)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun defaultFolderIdFor(category: VaultCategory): String = when (category) {
        VaultCategory.PHOTOS -> "folder_photos"
        VaultCategory.VIDEOS -> "folder_videos"
        VaultCategory.MUSIC -> "folder_music"
        VaultCategory.PDFS -> "folder_pdfs"
        VaultCategory.DOCUMENTS -> "folder_documents"
        VaultCategory.OTHERS -> "folder_others"
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return runCatching {
            appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        }.getOrNull()
    }

    private fun guessMimeType(ext: String): String = when (ext) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "heic" -> "image/heic"
        "mp4" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "mov" -> "video/quicktime"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "m4a" -> "audio/mp4"
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "zip" -> "application/zip"
        "apk" -> "application/vnd.android.package-archive"
        else -> "application/octet-stream"
    }

    private data class SampleSeedSpec(
        val id: String,
        val folderId: String,
        val name: String,
        val ext: String,
        val mime: String,
        val category: String,
        val durationMs: Long,
        val decoy: Boolean,
        val bytes: ByteArray
    )
}
