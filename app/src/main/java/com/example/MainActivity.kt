package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.GoogleAuthHelper
import com.example.auth.authStateFlow
import com.example.data.VaultRepository
import com.example.model.PlanTier
import com.example.model.ProPlanCycle
import com.example.model.UiState
import com.example.model.VaultCategory
import com.example.model.VaultFileMeta
import com.example.model.VaultFolder
import com.example.security.BiometricAvailability
import com.example.security.BiometricHelper
import com.example.security.BreakInMonitor
import com.example.security.VaultCryptoManager
import com.example.ui.components.VaultGradientBackground
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BiometricSetupScreen
import com.example.ui.screens.BreakInAlertsScreen
import com.example.ui.screens.CalculatorDisguiseScreen
import com.example.ui.screens.CategoryVaultBrowserScreen
import com.example.ui.screens.FingerprintUnlockScreen
import com.example.ui.screens.HomeVaultScreen
import com.example.ui.screens.InAppViewerScreen
import com.example.ui.screens.MasterPinSetupScreen
import com.example.ui.screens.PinUnlockScreen
import com.example.ui.screens.ProUpgradeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.GoponVaultTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultObsidian
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VioletGlow
import com.example.ui.theme.VioletPrimary
import com.example.ui.theme.VioletSoft
import com.example.viewmodel.VaultGateState
import com.example.viewmodel.VaultViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoponVaultTheme {
                GoponVaultAppRoot()
            }
        }
    }
}

private enum class VaultTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    PHOTOS("Photos", Icons.Filled.Photo, Icons.Outlined.Photo, "nav_photos"),
    VIDEOS("Videos", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary, "nav_videos"),
    DOCUMENTS("Docs", Icons.Filled.Description, Icons.Outlined.Description, "nav_documents"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
}

@Composable
fun GoponVaultAppRoot(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var splashDelayDone by remember { mutableStateOf(false) }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        GoogleAuthHelper.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
        delay(950L)
        splashDelayDone = true
    }

    if (!splashDelayDone) {
        SplashScreen()
        return
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedVaultHost(
                userId = user.uid,
                userEmail = user.email.orEmpty(),
                userDisplayName = user.displayName.orEmpty(),
                onSignOut = {
                    GoogleAuthHelper.signOut(credentialManager = credentialManager, scope = scope)
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedVaultHost(
    userId: String,
    userEmail: String,
    userDisplayName: String,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val vaultViewModel: VaultViewModel = viewModel(
        key = userId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val crypto = VaultCryptoManager(app)
                val repo = VaultRepository(db = db, appContext = app, crypto = crypto)
                val monitor = BreakInMonitor(app, crypto)
                VaultViewModel(
                    repository = repo,
                    breakInMonitor = monitor,
                    currentUserId = userId,
                    userEmail = userEmail,
                    userDisplayName = userDisplayName
                )
            }
        }
    )

    val configState by vaultViewModel.securityConfigState.collectAsStateWithLifecycle()
    val foldersState by vaultViewModel.foldersState.collectAsStateWithLifecycle()
    val filesState by vaultViewModel.filesState.collectAsStateWithLifecycle()
    val alertsState by vaultViewModel.alertsState.collectAsStateWithLifecycle()
    val backupsState by vaultViewModel.backupsState.collectAsStateWithLifecycle()

    val gateState by vaultViewModel.gateState.collectAsStateWithLifecycle()
    val awaitingSecondFactor by vaultViewModel.awaitingSecondFactorPin.collectAsStateWithLifecycle()
    val biometricFailCount by vaultViewModel.biometricFailCount.collectAsStateWithLifecycle()
    val pinError by vaultViewModel.pinError.collectAsStateWithLifecycle()
    val searchQuery by vaultViewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOrder by vaultViewModel.sortOrder.collectAsStateWithLifecycle()
    val isGridView by vaultViewModel.isGridView.collectAsStateWithLifecycle()
    val unlockedFolderIds by vaultViewModel.unlockedFolderIds.collectAsStateWithLifecycle()
    val banner by vaultViewModel.banner.collectAsStateWithLifecycle()
    val isBusy by vaultViewModel.isBusy.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(banner) {
        val current = banner ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(current.message)
        vaultViewModel.clearBanner()
    }

    // Resolve initial lock gate once Firestore config loads
    val biometricHardwareReady = remember(context) {
        BiometricHelper.checkCapabilities(context).availability == BiometricAvailability.AVAILABLE
    }
    LaunchedEffect(configState) {
        if (configState is UiState.Success) {
            val cfg = (configState as UiState.Success).data
            vaultViewModel.resolveInitialGate(cfg, biometricHardwareReady)
        }
    }

    // Auto-lock when app goes to background
    DisposableEffect(lifecycleOwner, configState) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val cfg = (configState as? UiState.Success)?.data
                if (cfg != null &&
                    (gateState == VaultGateState.UNLOCKED_REAL || gateState == VaultGateState.UNLOCKED_DECOY)
                ) {
                    vaultViewModel.lockVaultNow(cfg, biometricHardwareReady)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var activeTab by remember { mutableStateOf(VaultTab.HOME) }
    var activeFolder by remember { mutableStateOf<VaultFolder?>(null) }
    var viewingFile by remember { mutableStateOf<VaultFileMeta?>(null) }
    var showProScreen by remember { mutableStateOf(false) }
    var showBreakInScreen by remember { mutableStateOf(false) }
    var pendingTargetFolderId by remember { mutableStateOf<String?>(null) }
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }

    // Zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        uris.forEach { uri ->
            vaultViewModel.importFile(uri, pendingTargetFolderId)
        }
        pendingTargetFolderId = null
    }

    // File Manager & Google Drive picker via Storage Access Framework
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            vaultViewModel.importFile(uri, pendingTargetFolderId)
        }
        pendingTargetFolderId = null
    }

    // Private Camera Capture (stores to private cache, encrypts immediately, never goes to Gallery)
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { saved ->
        val uri = cameraTempUri
        if (saved && uri != null) {
            vaultViewModel.importFile(uri, pendingTargetFolderId)
        }
        cameraTempUri = null
        pendingTargetFolderId = null
    }

    val cameraImportPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "vault_cam_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraTempUri = uri
            takePictureLauncher.launch(uri)
        } else {
            vaultViewModel.showBanner("Camera permission required to take private vault photos.", isError = true)
        }
    }

    val intruderPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val camOk = results[Manifest.permission.CAMERA] == true
        if (camOk) {
            vaultViewModel.showBanner("Break-in selfie & location permissions enabled.")
        } else {
            vaultViewModel.showBanner("Camera permission not granted; selfies skipped.", isError = true)
        }
    }

    fun launchGalleryImport(folderId: String? = null) {
        pendingTargetFolderId = folderId
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        )
    }

    fun launchCameraImport(folderId: String? = null) {
        pendingTargetFolderId = folderId
        cameraImportPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun launchFilesOrDriveImport(folderId: String? = null) {
        pendingTargetFolderId = folderId
        documentPickerLauncher.launch(arrayOf("*/*"))
    }

    /**
     * Fingerprint-gated action helper: To share or delete any file, fingerprint auth is required
     * (falls back to direct execution only if device lacks biometric hardware).
     */
    fun runWithBiometricGate(actionLabel: String, action: () -> Unit) {
        val act = activity
        if (act != null && biometricHardwareReady) {
            BiometricHelper.showBiometricPrompt(
                activity = act,
                title = "Fingerprint Required",
                subtitle = "Confirm identity to $actionLabel",
                negativeButtonText = "Cancel",
                onSuccess = action,
                onErrorOrFallback = { msg ->
                    vaultViewModel.showBanner("Authentication cancelled: $msg", isError = true)
                }
            )
        } else {
            action()
        }
    }

    fun shareDecryptedFile(fileMeta: VaultFileMeta) {
        runWithBiometricGate("export & share ${fileMeta.name}") {
            scope.launch {
                vaultViewModel.prepareDecryptedCacheFile(fileMeta).fold(
                    onSuccess = { cacheFile ->
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            cacheFile
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = fileMeta.mimeType
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share ${fileMeta.name}"))
                    },
                    onFailure = {
                        vaultViewModel.showBanner("Could not decrypt file for sharing.", isError = true)
                    }
                )
            }
        }
    }

    fun deleteFileSecurely(fileMeta: VaultFileMeta) {
        runWithBiometricGate("permanently delete ${fileMeta.name}") {
            if (viewingFile?.id == fileMeta.id) viewingFile = null
            vaultViewModel.deleteFile(fileMeta)
        }
    }

    if (configState is UiState.Loading || gateState == VaultGateState.SPLASH) {
        SplashScreen()
        return
    }

    if (configState is UiState.Error) {
        VaultGradientBackground {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = (configState as UiState.Error).message,
                    color = TextPrimary,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
        return
    }

    val config = (configState as UiState.Success).data

    when (gateState) {
        VaultGateState.SPLASH -> SplashScreen()

        VaultGateState.ONBOARDING_PIN -> MasterPinSetupScreen(
            userEmail = userEmail,
            onPinConfirmed = { pin ->
                vaultViewModel.saveOnboardingPin(pin, biometricHardwareReady)
            }
        )

        VaultGateState.ONBOARDING_BIOMETRIC -> BiometricSetupScreen(
            isBusy = isBusy,
            onEnableBiometric = { fp, face ->
                vaultViewModel.completeOnboarding(fingerprint = fp, faceLock = face)
            },
            onSkip = {
                vaultViewModel.completeOnboarding(fingerprint = false, faceLock = false)
            }
        )

        VaultGateState.BIOMETRIC_LOCK -> FingerprintUnlockScreen(
            failedAttempts = biometricFailCount,
            doubleSecurityEnabled = config?.doubleSecurityMode == true,
            onBiometricSuccess = { vaultViewModel.onBiometricUnlockSucceeded() },
            onBiometricFailedAttempt = { vaultViewModel.onBiometricFailedAttempt() },
            onUseMasterPin = { vaultViewModel.switchToPinUnlock() }
        )

        VaultGateState.PIN_LOCK -> PinUnlockScreen(
            errorText = pinError,
            awaitingSecondFactor = awaitingSecondFactor,
            biometricAvailable = biometricHardwareReady && config?.fingerprintEnabled == true,
            wrongAttempts = (config?.wrongAttempts ?: 0L).toInt(),
            onSubmitPin = { pin, owner -> vaultViewModel.submitPinAttempt(pin, owner) },
            onUseBiometric = { vaultViewModel.switchToBiometricUnlock() }
        )

        VaultGateState.CALCULATOR_DISGUISE -> CalculatorDisguiseScreen(
            onSecretCodeEntered = { code -> vaultViewModel.submitCalculatorExpression(code) },
            isPanicLocked = false
        )

        VaultGateState.PANIC_WIPED_LOCKDOWN -> CalculatorDisguiseScreen(
            onSecretCodeEntered = { false },
            isPanicLocked = true
        )

        VaultGateState.UNLOCKED_REAL, VaultGateState.UNLOCKED_DECOY -> {
            val isDecoy = gateState == VaultGateState.UNLOCKED_DECOY
            val currentConfig = config ?: return
            val folders = (foldersState as? UiState.Success)?.data.orEmpty()
            val files = (filesState as? UiState.Success)?.data.orEmpty()
            val alerts = (alertsState as? UiState.Success)?.data.orEmpty()
            val backups = (backupsState as? UiState.Success)?.data.orEmpty()

            // Secondary full-screen overlays with proper BackHandler support
            when {
                viewingFile != null -> {
                    BackHandler { viewingFile = null }
                    InAppViewerScreen(
                        file = viewingFile!!,
                        decryptBytes = { vaultViewModel.decryptFileBytes(it) },
                        decryptCacheFile = { vaultViewModel.prepareDecryptedCacheFile(it) },
                        onShareWithBiometric = { shareDecryptedFile(it) },
                        onDeleteWithBiometric = { deleteFileSecurely(it) },
                        onClose = { viewingFile = null }
                    )
                }
                showProScreen -> {
                    BackHandler { showProScreen = false }
                    ProUpgradeScreen(
                        currentTier = PlanTier.fromRaw(currentConfig.planTier),
                        currentCycle = ProPlanCycle.fromRaw(currentConfig.proPlanCycle),
                        onSelectPlan = { cycle ->
                            vaultViewModel.activateProPlan(cycle)
                            showProScreen = false
                        },
                        onBack = { showProScreen = false }
                    )
                }
                showBreakInScreen -> {
                    BackHandler { showBreakInScreen = false }
                    BreakInAlertsScreen(
                        alerts = alerts,
                        onSendGmailAlert = { alert ->
                            val monitor = BreakInMonitor(context, VaultCryptoManager(context))
                            val mailIntent = monitor.buildGmailAlertIntent(
                                recipientEmail = alert.emailAlertTarget,
                                attemptCount = alert.attemptCount.toInt(),
                                locationText = alert.locationText,
                                detail = alert.detail
                            )
                            runCatching { context.startActivity(mailIntent) }
                                .onFailure {
                                    vaultViewModel.showBanner("No email app installed to open Gmail draft.", isError = true)
                                }
                        },
                        onBack = { showBreakInScreen = false }
                    )
                }
                else -> {
                    if (activeFolder != null) {
                        BackHandler { activeFolder = null }
                    } else if (activeTab != VaultTab.HOME) {
                        BackHandler { activeTab = VaultTab.HOME }
                    }

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        awaitPointerEvent()
                                        vaultViewModel.recordUserInteraction()
                                    }
                                }
                            },
                        containerColor = VaultObsidian,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        bottomBar = {
                            if (activeFolder == null) {
                                NavigationBar(
                                    containerColor = VaultSurface,
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    VaultTab.entries.forEach { tab ->
                                        val selected = activeTab == tab
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
                                                vaultViewModel.recordUserInteraction()
                                                activeTab = tab
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.label
                                                )
                                            },
                                            label = { Text(tab.label) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color.White,
                                                selectedTextColor = VioletSoft,
                                                indicatorColor = VioletPrimary,
                                                unselectedIconColor = TextSecondary,
                                                unselectedTextColor = TextSecondary
                                            ),
                                            modifier = Modifier.testTag(tab.testTag)
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        VaultGradientBackground(
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            val folder = activeFolder
                            if (folder != null) {
                                CategoryVaultBrowserScreen(
                                    title = folder.name,
                                    files = files,
                                    searchQuery = searchQuery,
                                    sortOrder = sortOrder,
                                    isGridView = isGridView,
                                    activeFolder = folder,
                                    isFolderUnlocked = !folder.locked || unlockedFolderIds.contains(folder.id),
                                    isDecoyMode = isDecoy,
                                    onSearchChange = vaultViewModel::setSearchQuery,
                                    onSortChange = vaultViewModel::setSortOrder,
                                    onToggleViewMode = vaultViewModel::toggleGridView,
                                    onBack = { activeFolder = null },
                                    onOpenFile = { viewingFile = it },
                                    onShareFile = { shareDecryptedFile(it) },
                                    onDeleteFile = { deleteFileSecurely(it) },
                                    onImportClick = { launchFilesOrDriveImport(folder.id) },
                                    onVerifyFolderPin = { f, p -> vaultViewModel.verifyFolderPin(f, p) },
                                    onUnlockFolderBiometric = { id -> vaultViewModel.markFolderUnlockedViaBiometric(id) },
                                    onToggleFolderLock = { f, lock, pin ->
                                        vaultViewModel.toggleFolderLock(f, lock, pin)
                                        activeFolder = f.copy(locked = lock)
                                    }
                                )
                            } else {
                                when (activeTab) {
                                    VaultTab.HOME -> HomeVaultScreen(
                                        config = currentConfig,
                                        folders = folders,
                                        files = files,
                                        isDecoyMode = isDecoy,
                                        onFolderClick = { clicked -> activeFolder = clicked },
                                        onOpenFile = { viewingFile = it },
                                        onShareFile = { shareDecryptedFile(it) },
                                        onDeleteFile = { deleteFileSecurely(it) },
                                        onImportFromGallery = { launchGalleryImport(null) },
                                        onImportFromCamera = { launchCameraImport(null) },
                                        onImportFromFilesOrDrive = { launchFilesOrDriveImport(null) },
                                        onCreateCustomFolder = vaultViewModel::createFolder,
                                        onUpgradePro = { showProScreen = true },
                                        onLockNow = {
                                            vaultViewModel.lockVaultNow(currentConfig, biometricHardwareReady)
                                        },
                                        onPreviewCalculator = {
                                            vaultViewModel.enterCalculatorModePreview()
                                        }
                                    )

                                    VaultTab.PHOTOS -> CategoryVaultBrowserScreen(
                                        title = "Photos Vault",
                                        files = files.filter {
                                            VaultCategory.fromRaw(it.category) == VaultCategory.PHOTOS
                                        },
                                        searchQuery = searchQuery,
                                        sortOrder = sortOrder,
                                        isGridView = isGridView,
                                        isDecoyMode = isDecoy,
                                        onSearchChange = vaultViewModel::setSearchQuery,
                                        onSortChange = vaultViewModel::setSortOrder,
                                        onToggleViewMode = vaultViewModel::toggleGridView,
                                        onOpenFile = { viewingFile = it },
                                        onShareFile = { shareDecryptedFile(it) },
                                        onDeleteFile = { deleteFileSecurely(it) },
                                        onImportClick = { launchGalleryImport("folder_photos") }
                                    )

                                    VaultTab.VIDEOS -> CategoryVaultBrowserScreen(
                                        title = "Videos Vault",
                                        files = files.filter {
                                            VaultCategory.fromRaw(it.category) == VaultCategory.VIDEOS
                                        },
                                        searchQuery = searchQuery,
                                        sortOrder = sortOrder,
                                        isGridView = isGridView,
                                        isDecoyMode = isDecoy,
                                        onSearchChange = vaultViewModel::setSearchQuery,
                                        onSortChange = vaultViewModel::setSortOrder,
                                        onToggleViewMode = vaultViewModel::toggleGridView,
                                        onOpenFile = { viewingFile = it },
                                        onShareFile = { shareDecryptedFile(it) },
                                        onDeleteFile = { deleteFileSecurely(it) },
                                        onImportClick = { launchGalleryImport("folder_videos") }
                                    )

                                    VaultTab.DOCUMENTS -> CategoryVaultBrowserScreen(
                                        title = "Documents & Audio",
                                        files = files,
                                        searchQuery = searchQuery,
                                        sortOrder = sortOrder,
                                        isGridView = isGridView,
                                        isDocumentsHub = true,
                                        isDecoyMode = isDecoy,
                                        onSearchChange = vaultViewModel::setSearchQuery,
                                        onSortChange = vaultViewModel::setSortOrder,
                                        onToggleViewMode = vaultViewModel::toggleGridView,
                                        onOpenFile = { viewingFile = it },
                                        onShareFile = { shareDecryptedFile(it) },
                                        onDeleteFile = { deleteFileSecurely(it) },
                                        onImportClick = { launchFilesOrDriveImport(null) }
                                    )

                                    VaultTab.SETTINGS -> SettingsScreen(
                                        config = currentConfig,
                                        backups = backups,
                                        alertsCount = alerts.size,
                                        onUpdateSettings = vaultViewModel::updateSettings,
                                        onUpdateMasterPin = vaultViewModel::updateMasterPin,
                                        onUpdateDecoyPin = vaultViewModel::updateDecoyPin,
                                        onUpdatePanicPin = vaultViewModel::updatePanicPin,
                                        onCreateBackup = vaultViewModel::createEncryptedBackup,
                                        onRestoreBackup = vaultViewModel::restoreFromBackup,
                                        onOpenBreakInAlerts = { showBreakInScreen = true },
                                        onOpenProUpgrade = { showProScreen = true },
                                        onPreviewCalculator = {
                                            vaultViewModel.enterCalculatorModePreview()
                                        },
                                        onRequestIntruderPermissions = {
                                            intruderPermissionsLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.CAMERA,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        },
                                        onSignOut = onSignOut
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
