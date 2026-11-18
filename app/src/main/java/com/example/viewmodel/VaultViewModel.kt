package com.example.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VaultRepository
import com.example.model.AlertType
import com.example.model.CloudBackupRecord
import com.example.model.PlanTier
import com.example.model.ProPlanCycle
import com.example.model.SecurityAlertLog
import com.example.model.SortOrder
import com.example.model.UiState
import com.example.model.UserSecurityConfig
import com.example.model.VaultCategory
import com.example.model.VaultFileMeta
import com.example.model.VaultFolder
import com.example.security.BiometricAvailability
import com.example.security.BiometricHelper
import com.example.security.BreakInMonitor
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VaultGateState {
    SPLASH,
    ONBOARDING_PIN,
    ONBOARDING_BIOMETRIC,
    BIOMETRIC_LOCK,
    PIN_LOCK,
    CALCULATOR_DISGUISE,
    UNLOCKED_REAL,
    UNLOCKED_DECOY,
    PANIC_WIPED_LOCKDOWN
}

data class VaultUiBanner(
    val message: String,
    val isError: Boolean = false
)

class VaultViewModel(
    private val repository: VaultRepository,
    private val breakInMonitor: BreakInMonitor,
    private val currentUserId: String,
    private val userEmail: String,
    private val userDisplayName: String
) : ViewModel() {

    val securityConfigState: StateFlow<UiState<UserSecurityConfig?>> =
        repository.observeSecurityConfig(currentUserId)
            .map<UserSecurityConfig?, UiState<UserSecurityConfig?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing security config", root)
                emit(UiState.Error(root.message ?: "Failed to load security configuration"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val foldersState: StateFlow<UiState<List<VaultFolder>>> =
        repository.observeFolders(currentUserId)
            .map<List<VaultFolder>, UiState<List<VaultFolder>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing folders", root)
                emit(UiState.Error(root.message ?: "Failed to load folders"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val filesState: StateFlow<UiState<List<VaultFileMeta>>> =
        repository.observeFiles(currentUserId)
            .map<List<VaultFileMeta>, UiState<List<VaultFileMeta>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing files", root)
                emit(UiState.Error(root.message ?: "Failed to load vault files"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val alertsState: StateFlow<UiState<List<SecurityAlertLog>>> =
        repository.observeAlerts(currentUserId)
            .map<List<SecurityAlertLog>, UiState<List<SecurityAlertLog>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                emit(UiState.Error(root.message ?: "Failed to load security alerts"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val backupsState: StateFlow<UiState<List<CloudBackupRecord>>> =
        repository.observeBackups(currentUserId)
            .map<List<CloudBackupRecord>, UiState<List<CloudBackupRecord>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                emit(UiState.Error(root.message ?: "Failed to load backups"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _gateState = MutableStateFlow(VaultGateState.SPLASH)
    val gateState: StateFlow<VaultGateState> = _gateState.asStateFlow()

    private val _pendingMasterPin = MutableStateFlow<String?>(null)
    private val _awaitingSecondFactorPin = MutableStateFlow(false)
    val awaitingSecondFactorPin: StateFlow<Boolean> = _awaitingSecondFactorPin.asStateFlow()

    private val _biometricFailCount = MutableStateFlow(0)
    val biometricFailCount: StateFlow<Int> = _biometricFailCount.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _unlockedFolderIds = MutableStateFlow<Set<String>>(emptySet())
    val unlockedFolderIds: StateFlow<Set<String>> = _unlockedFolderIds.asStateFlow()

    private val _banner = MutableStateFlow<VaultUiBanner?>(null)
    val banner: StateFlow<VaultUiBanner?> = _banner.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _lastInteractionEpochMs = MutableStateFlow(System.currentTimeMillis())

    init {
        startAutoLockTicker()
    }

    fun recordUserInteraction() {
        _lastInteractionEpochMs.value = System.currentTimeMillis()
    }

    /**
     * Inactivity auto-lock loop: locks vault after 1 minute (60s) of inactivity.
     */
    private fun startAutoLockTicker() {
        viewModelScope.launch {
            while (true) {
                delay(5000L)
                val gate = _gateState.value
                val config = (securityConfigState.value as? UiState.Success)?.data
                if ((gate == VaultGateState.UNLOCKED_REAL || gate == VaultGateState.UNLOCKED_DECOY) && config != null) {
                    val elapsedSec = (System.currentTimeMillis() - _lastInteractionEpochMs.value) / 1000L
                    if (elapsedSec >= config.autoLockSeconds) {
                        lockVaultNow(config)
                    }
                }
            }
        }
    }

    fun resolveInitialGate(config: UserSecurityConfig?, hasBiometricHardware: Boolean) {
        if (_gateState.value != VaultGateState.SPLASH) return
        if (config == null) {
            _gateState.value = VaultGateState.ONBOARDING_PIN
            return
        }
        routeToLockScreen(config, hasBiometricHardware)
    }

    fun lockVaultNow(config: UserSecurityConfig? = currentConfigOrNull(), hasBiometricHardware: Boolean = true) {
        _unlockedFolderIds.value = emptySet()
        _awaitingSecondFactorPin.value = false
        _biometricFailCount.value = 0
        _pinError.value = null
        if (config == null) {
            _gateState.value = VaultGateState.ONBOARDING_PIN
        } else {
            routeToLockScreen(config, hasBiometricHardware)
        }
    }

    private fun routeToLockScreen(config: UserSecurityConfig, hasBiometricHardware: Boolean) {
        _gateState.value = when {
            config.calculatorModeEnabled -> VaultGateState.CALCULATOR_DISGUISE
            (config.fingerprintEnabled || config.faceLockEnabled) && hasBiometricHardware ->
                VaultGateState.BIOMETRIC_LOCK
            else -> VaultGateState.PIN_LOCK
        }
    }

    fun saveOnboardingPin(pin: String, biometricAvailable: Boolean) {
        _pendingMasterPin.value = pin
        if (biometricAvailable) {
            _gateState.value = VaultGateState.ONBOARDING_BIOMETRIC
        } else {
            completeOnboarding(fingerprint = false, faceLock = false)
        }
    }

    fun completeOnboarding(fingerprint: Boolean, faceLock: Boolean) {
        val pin = _pendingMasterPin.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.initializeUserVault(
                userId = currentUserId,
                email = userEmail,
                displayName = userDisplayName,
                masterPin = pin,
                fingerprintEnabled = fingerprint,
                faceLockEnabled = faceLock
            )
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    _pendingMasterPin.value = null
                    _lastInteractionEpochMs.value = System.currentTimeMillis()
                    _gateState.value = VaultGateState.UNLOCKED_REAL
                    showBanner("Gopon Vault encrypted and ready.")
                },
                onFailure = { err ->
                    showBanner(err.localizedMessage ?: "Failed to initialize vault", isError = true)
                }
            )
        }
    }

    fun onBiometricUnlockSucceeded() {
        val config = currentConfigOrNull() ?: return
        _biometricFailCount.value = 0
        _pinError.value = null
        if (config.doubleSecurityMode) {
            _awaitingSecondFactorPin.value = true
            _gateState.value = VaultGateState.PIN_LOCK
        } else {
            _lastInteractionEpochMs.value = System.currentTimeMillis()
            _gateState.value = VaultGateState.UNLOCKED_REAL
        }
    }

    fun onBiometricFailedAttempt() {
        val next = _biometricFailCount.value + 1
        _biometricFailCount.value = next
        if (next >= 3) {
            _pinError.value = "3 failed biometric attempts. Enter Master PIN."
            _gateState.value = VaultGateState.PIN_LOCK
        }
    }

    fun switchToPinUnlock() {
        _pinError.value = null
        _gateState.value = VaultGateState.PIN_LOCK
    }

    fun switchToBiometricUnlock() {
        _biometricFailCount.value = 0
        _pinError.value = null
        _gateState.value = VaultGateState.BIOMETRIC_LOCK
    }

    fun enterCalculatorModePreview() {
        _unlockedFolderIds.value = emptySet()
        _gateState.value = VaultGateState.CALCULATOR_DISGUISE
    }

    fun submitCalculatorExpression(expression: String): Boolean {
        val config = currentConfigOrNull() ?: return false
        val cleaned = expression.trim()
        if (cleaned == config.calculatorCode || repository.crypto.verifySecret(cleaned, config.masterPinHash)) {
            _lastInteractionEpochMs.value = System.currentTimeMillis()
            _gateState.value = VaultGateState.UNLOCKED_REAL
            return true
        }
        if (config.decoyPinHash.isNotBlank() && repository.crypto.verifySecret(cleaned, config.decoyPinHash)) {
            _lastInteractionEpochMs.value = System.currentTimeMillis()
            _gateState.value = VaultGateState.UNLOCKED_DECOY
            return true
        }
        return false
    }

    /**
     * Verifies PIN against Master, Decoy, and Panic hashes and runs escalation at 3, 10, 15, and 20 wrong attempts.
     */
    fun submitPinAttempt(pin: String, lifecycleOwner: LifecycleOwner?) {
        val config = currentConfigOrNull() ?: return
        viewModelScope.launch {
            // 1. Check Panic Password first (instantly deletes all files and hides vault)
            if (config.panicPinHash.isNotBlank() && repository.crypto.verifySecret(pin, config.panicPinHash)) {
                val files = (filesState.value as? UiState.Success)?.data.orEmpty()
                repository.wipeAllRealVaultFiles(currentUserId, files)
                repository.logSecurityAlert(
                    userId = currentUserId,
                    alertType = AlertType.PANIC_WIPE,
                    attemptCount = 0,
                    detail = "Panic Password entered. All real vault files permanently shredded and disguised.",
                    locationText = "Hidden",
                    selfieLocalPath = "",
                    emailTarget = config.email
                )
                repository.updateSecurityFields(
                    currentUserId,
                    mapOf("calculatorModeEnabled" to true, "wrongAttempts" to 0L)
                )
                _gateState.value = VaultGateState.PANIC_WIPED_LOCKDOWN
                return@launch
            }

            // 2. Check Fake / Decoy Vault PIN
            if (!_awaitingSecondFactorPin.value &&
                config.decoyPinHash.isNotBlank() &&
                repository.crypto.verifySecret(pin, config.decoyPinHash)
            ) {
                _pinError.value = null
                _lastInteractionEpochMs.value = System.currentTimeMillis()
                _gateState.value = VaultGateState.UNLOCKED_DECOY
                return@launch
            }

            // 3. Check Real Master PIN
            if (repository.crypto.verifySecret(pin, config.masterPinHash)) {
                _pinError.value = null
                _awaitingSecondFactorPin.value = false
                if (config.wrongAttempts > 0L) {
                    repository.updateSecurityFields(currentUserId, mapOf("wrongAttempts" to 0L))
                }
                _lastInteractionEpochMs.value = System.currentTimeMillis()
                _gateState.value = VaultGateState.UNLOCKED_REAL
                return@launch
            }

            // 4. Wrong attempt escalation (3 -> selfie+location, 10 -> warning 1, 15 -> warning 2 + selfie, 20 -> permanent delete)
            val nextAttempts = (config.wrongAttempts + 1L).coerceAtMost(20L).toInt()
            repository.updateSecurityFields(currentUserId, mapOf("wrongAttempts" to nextAttempts.toLong()))

            when (nextAttempts) {
                3 -> {
                    val evidence = breakInMonitor.captureBreakInEvidence(lifecycleOwner)
                    repository.logSecurityAlert(
                        userId = currentUserId,
                        alertType = AlertType.BREAK_IN_SELFIE,
                        attemptCount = 3,
                        detail = "3 wrong PIN attempts. Break-in selfie & location recorded for ${config.email}.",
                        locationText = evidence.locationText,
                        selfieLocalPath = evidence.selfieLocalPath,
                        emailTarget = config.email
                    )
                    _pinError.value = "Incorrect password (3/20) • Intruder snapshot logged"
                }
                10 -> {
                    repository.logSecurityAlert(
                        userId = currentUserId,
                        alertType = AlertType.WARNING_EMAIL_10,
                        attemptCount = 10,
                        detail = "10 failed attempts. Security warning prepared for ${config.email}.",
                        locationText = "Check Intruder Log",
                        selfieLocalPath = "",
                        emailTarget = config.email
                    )
                    _pinError.value = "Incorrect password (10/20) • Warning sent to Gmail"
                }
                15 -> {
                    val evidence = breakInMonitor.captureBreakInEvidence(lifecycleOwner)
                    repository.logSecurityAlert(
                        userId = currentUserId,
                        alertType = AlertType.CRITICAL_WARNING_15,
                        attemptCount = 15,
                        detail = "15 failed attempts! Second warning + break-in selfie recorded.",
                        locationText = evidence.locationText,
                        selfieLocalPath = evidence.selfieLocalPath,
                        emailTarget = config.email
                    )
                    _pinError.value = "Incorrect password (15/20) • 5 tries before permanent wipe!"
                }
                in 20..Int.MAX_VALUE -> {
                    val files = (filesState.value as? UiState.Success)?.data.orEmpty()
                    repository.wipeAllRealVaultFiles(currentUserId, files)
                    repository.logSecurityAlert(
                        userId = currentUserId,
                        alertType = AlertType.VAULT_WIPED_20,
                        attemptCount = 20,
                        detail = "20 wrong attempts reached. All vault files permanently deleted.",
                        locationText = "Self-Destruct Complete",
                        selfieLocalPath = "",
                        emailTarget = config.email
                    )
                    repository.updateSecurityFields(currentUserId, mapOf("wrongAttempts" to 0L))
                    _pinError.value = "20 wrong attempts reached. Vault permanently wiped."
                }
                else -> {
                    _pinError.value = "Incorrect password ($nextAttempts/20)"
                }
            }
        }
    }

    fun verifyMasterPassword(candidate: String): Boolean {
        val config = currentConfigOrNull() ?: return false
        return repository.crypto.verifySecret(candidate, config.masterPinHash)
    }

    fun verifyFolderPin(folder: VaultFolder, candidate: String): Boolean {
        if (!folder.locked || folder.folderPinHash.isBlank()) return true
        val ok = repository.crypto.verifySecret(candidate, folder.folderPinHash)
        if (ok) {
            _unlockedFolderIds.update { it + folder.id }
        }
        return ok
    }

    fun markFolderUnlockedViaBiometric(folderId: String) {
        _unlockedFolderIds.update { it + folderId }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun toggleGridView() {
        _isGridView.update { !it }
    }

    fun importFile(uri: Uri, targetFolderId: String? = null) {
        val config = currentConfigOrNull() ?: return
        val files = (filesState.value as? UiState.Success)?.data.orEmpty()
        val usedBytes = files.filter { !it.decoyItem }.sumOf { it.sizeBytes }
        val tier = PlanTier.fromRaw(config.planTier)

        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.importFromUri(
                userId = currentUserId,
                uri = uri,
                explicitFolderId = targetFolderId,
                currentUsedBytes = usedBytes,
                planTier = tier
            )
            _isBusy.value = false
            result.fold(
                onSuccess = { meta ->
                    showBanner("Encrypted & hidden in ${VaultCategory.fromRaw(meta.category).label}")
                    if (config.autoBackupEnabled) {
                        val folders = (foldersState.value as? UiState.Success)?.data.orEmpty()
                        // Auto backup index metadata update note
                        Log.d(TAG, "Auto-backup enabled; ${folders.size} folders ready")
                    }
                },
                onFailure = { err ->
                    showBanner(err.localizedMessage ?: "Import failed", isError = true)
                }
            )
        }
    }

    fun createFolder(name: String, colorHex: String, iconKey: String, folderPin: String?) {
        viewModelScope.launch {
            val res = repository.createCustomFolder(currentUserId, name, colorHex, iconKey, folderPin)
            res.fold(
                onSuccess = { showBanner("Folder \"$name\" created.") },
                onFailure = { showBanner(it.localizedMessage ?: "Could not create folder", isError = true) }
            )
        }
    }

    fun toggleFolderLock(folder: VaultFolder, locked: Boolean, pin: String?) {
        viewModelScope.launch {
            val res = repository.updateFolderLock(currentUserId, folder.id, locked, pin)
            res.fold(
                onSuccess = {
                    if (!locked) _unlockedFolderIds.update { it - folder.id }
                    showBanner(if (locked) "${folder.name} locked" else "${folder.name} unlocked")
                },
                onFailure = { showBanner(it.localizedMessage ?: "Update failed", isError = true) }
            )
        }
    }

    fun deleteFile(fileMeta: VaultFileMeta) {
        viewModelScope.launch {
            val res = repository.deleteFile(currentUserId, fileMeta)
            res.fold(
                onSuccess = { showBanner("Permanently deleted ${fileMeta.name}") },
                onFailure = { showBanner(it.localizedMessage ?: "Delete failed", isError = true) }
            )
        }
    }

    suspend fun prepareDecryptedCacheFile(fileMeta: VaultFileMeta) =
        repository.decryptToPrivateCacheFile(fileMeta)

    suspend fun decryptFileBytes(fileMeta: VaultFileMeta) =
        repository.decryptToBytes(fileMeta)

    fun updateSettings(updates: Map<String, Any>, toast: String? = null) {
        viewModelScope.launch {
            val res = repository.updateSecurityFields(currentUserId, updates)
            res.fold(
                onSuccess = { if (toast != null) showBanner(toast) },
                onFailure = { showBanner(it.localizedMessage ?: "Failed to save settings", isError = true) }
            )
        }
    }

    fun updateMasterPin(newPin: String) {
        val hash = repository.crypto.hashSecret(newPin)
        updateSettings(mapOf("masterPinHash" to hash), "Master PIN updated.")
    }

    fun updateDecoyPin(newPin: String) {
        val hash = if (newPin.isBlank()) "" else repository.crypto.hashSecret(newPin)
        updateSettings(mapOf("decoyPinHash" to hash), "Decoy Vault PIN updated.")
    }

    fun updatePanicPin(newPin: String) {
        val hash = if (newPin.isBlank()) "" else repository.crypto.hashSecret(newPin)
        updateSettings(mapOf("panicPinHash" to hash), "Panic Password updated.")
    }

    fun createEncryptedBackup(masterPassword: String) {
        if (!verifyMasterPassword(masterPassword)) {
            showBanner("Incorrect Master Password. Backup cancelled.", isError = true)
            return
        }
        val files = (filesState.value as? UiState.Success)?.data.orEmpty()
        val folders = (foldersState.value as? UiState.Success)?.data.orEmpty()
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createCloudBackup(currentUserId, masterPassword, files, folders)
            _isBusy.value = false
            res.fold(
                onSuccess = { showBanner("AES-256 encrypted backup uploaded (${it.itemCount} items).") },
                onFailure = { showBanner(it.localizedMessage ?: "Backup failed", isError = true) }
            )
        }
    }

    fun restoreFromBackup(masterPassword: String, record: CloudBackupRecord) {
        if (!verifyMasterPassword(masterPassword)) {
            showBanner("Master Password required to decrypt backup.", isError = true)
            return
        }
        val res = repository.decryptBackupRecord(masterPassword, record)
        res.fold(
            onSuccess = {
                showBanner("Backup \"${record.label}\" decrypted & verified (${record.itemCount} items).")
            },
            onFailure = {
                showBanner("Decryption failed: Invalid Master Password or corrupted cipher.", isError = true)
            }
        )
    }

    fun activateProPlan(cycle: ProPlanCycle) {
        updateSettings(
            mapOf(
                "planTier" to PlanTier.PRO.name,
                "proPlanCycle" to cycle.name
            ),
            "Gopon Pro (${cycle.priceText}) activated!"
        )
    }

    fun clearBanner() {
        _banner.value = null
    }

    fun showBanner(message: String, isError: Boolean = false) {
        _banner.value = VaultUiBanner(message, isError)
    }

    private fun currentConfigOrNull(): UserSecurityConfig? =
        (securityConfigState.value as? UiState.Success)?.data

    fun checkBiometricAvailability(context: android.content.Context): Boolean {
        return BiometricHelper.checkCapabilities(context).availability == BiometricAvailability.AVAILABLE
    }

    companion object {
        private const val TAG = "VaultVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
