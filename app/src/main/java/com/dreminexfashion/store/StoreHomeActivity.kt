package com.dreminexfashion.store

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.trusted.TrustedWebActivityIntentBuilder
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.dreminexfashion.store.update.UpdateCheckResult
import com.dreminexfashion.store.update.UpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class StoreHomeActivity : ComponentActivity() {
    private val repository = UpdateRepository()
    private var updateCheck: UpdateCheckResult? = null
    private var downloadedApk: File? = null
    private var checking by androidx.compose.runtime.mutableStateOf(false)
    private var downloading by androidx.compose.runtime.mutableStateOf(false)
    private var downloadProgress by androidx.compose.runtime.mutableFloatStateOf(0f)
    private var message by androidx.compose.runtime.mutableStateOf("Checking app status…")
    private var messageIsError by androidx.compose.runtime.mutableStateOf(false)
    private var activeTab by androidx.compose.runtime.mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(245, 249, 249)
        window.navigationBarColor = android.graphics.Color.rgb(235, 244, 244)
        if (Build.VERSION.SDK_INT >= 23) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        setContent {
            StoreHomeScreen(
                activeTab = activeTab,
                update = updateCheck,
                checking = checking,
                downloading = downloading,
                downloadProgress = downloadProgress,
                message = message,
                messageIsError = messageIsError,
                installedVersion = BuildConfig.VERSION_NAME,
                onTabChange = { activeTab = it },
                onOpenStore = { openStoreLink(safeStoreUri(it)) },
                onCheckUpdates = { checkForUpdates(true) },
                onDownload = { downloadUpdate() },
                onInstall = { installDownloadedUpdate() }
            )
        }
        intent?.data?.let { uri ->
            safeStoreUri(uri.toString())?.let(::openStoreLink)
        } ?: checkForUpdates(false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.toString()?.let(::safeStoreUri)?.let(::openStoreLink)
    }

    private fun checkForUpdates(manual: Boolean) {
        val prefs = getSharedPreferences("store_updater", MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (!manual && now - prefs.getLong("last_check_ms", 0L) < 24L * 60L * 60L * 1000L) {
            message = "Updates are checked automatically once a day. You can check any time."
            return
        }
        if (checking) return
        checking = true
        messageIsError = false
        message = "Checking for a verified release…"
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.check(BuildConfig.VERSION_CODE.toLong(), Build.VERSION.SDK_INT)
                }
                updateCheck = result
                downloadedApk = null
                message = if (result == null) "You're using the latest version." else "Version ${result.release.versionName} is ready when you are."
                prefs.edit().putLong("last_check_ms", now).apply()
            } catch (error: Exception) {
                messageIsError = true
                message = error.message?.take(180) ?: "Update check failed. Try again when you are online."
            } finally {
                checking = false
            }
        }
    }

    private fun downloadUpdate() {
        val selected = updateCheck ?: return
        if (downloading) return
        downloading = true
        downloadProgress = 0f
        messageIsError = false
        message = "Downloading and checking the signed app…"
        val target = File(cacheDir, "updates/dreminex-fashion-release.apk")
        target.delete()
        lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    repository.downloadAndVerify(selected.release, target) { current, total ->
                        if (total > 0) downloadProgress = (current.toFloat() / total).coerceIn(0f, 1f)
                    }
                }
                withContext(Dispatchers.IO) { verifyApk(file, selected) }
                downloadedApk = file
                message = "Download verified. Android will ask you to confirm installation."
            } catch (error: Exception) {
                target.delete()
                downloadedApk = null
                messageIsError = true
                message = error.message?.take(180) ?: "The update could not be verified. The installed app is unchanged."
            } finally {
                downloading = false
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun verifyApk(file: File, selected: UpdateCheckResult) {
        val archiveInfo = if (Build.VERSION.SDK_INT >= 28) {
            packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
        } else {
            packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNATURES)
        } ?: error("The downloaded file is not a valid Android app.")
        require(archiveInfo.packageName == BuildConfig.APPLICATION_ID) { "This update is for a different app." }
        val archiveCode = if (Build.VERSION.SDK_INT >= 28) archiveInfo.longVersionCode else archiveInfo.versionCode.toLong()
        require(archiveCode == selected.release.versionCode && archiveCode > BuildConfig.VERSION_CODE) { "The app version does not match the verified release." }
        val currentInfo = if (Build.VERSION.SDK_INT >= 28) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
        } else {
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        }
        val expectedCertificate = BuildConfig.RELEASE_CERT_SHA256.lowercase().filter(Char::isLetterOrDigit)
        val currentCertificates = signerFingerprints(currentInfo)
        val archiveCertificates = signerFingerprints(archiveInfo)
        require(currentCertificates.contains(expectedCertificate)) { "The installed app signing identity is not recognized." }
        require(archiveCertificates.contains(expectedCertificate) && archiveCertificates == currentCertificates) { "The update was signed with a different certificate." }
    }

    @Suppress("DEPRECATION")
    private fun signerFingerprints(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= 28) {
            info.signingInfo?.apkContentsSigners?.toList().orEmpty()
        } else {
            info.signatures?.toList().orEmpty()
        }
        return signatures.map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    private fun installDownloadedUpdate() {
        val apk = downloadedApk ?: return
        if (!apk.exists()) {
            messageIsError = true
            message = "The verified update file is no longer available. Download it again."
            downloadedApk = null
            return
        }
        if (Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
            message = "Allow Dreminex Fashion to install this update in Android settings, then return and tap Install."
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            return
        }
        try {
            val contentUri = FileProvider.getUriForFile(this, "$packageName.updates", apk)
            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }
            startActivity(installIntent)
        } catch (error: Exception) {
            messageIsError = true
            message = error.message?.take(160) ?: "Android could not open the installer. The current app is unchanged."
        }
    }

    private fun openStoreLink(uri: Uri?) {
        if (uri == null) return
        val packageName = CustomTabsClient.getPackageName(this, null)
        if (packageName.isNullOrBlank()) {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            return
        }
        val serviceConnection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                try {
                    client.warmup(0L)
                    val session = client.newSession(null)
                    if (session == null) {
                        fallbackToBrowser(uri)
                    } else {
                        val twa = TrustedWebActivityIntentBuilder(uri)
                            .setToolbarColor(android.graphics.Color.rgb(8, 126, 139))
                            .setNavigationBarColor(android.graphics.Color.rgb(8, 126, 139))
                            .build(session)
                        twa.launchTrustedWebActivity(this@StoreHomeActivity)
                    }
                } catch (_: Exception) {
                    fallbackToBrowser(uri)
                } finally {
                    try { unbindService(this) } catch (_: Exception) { }
                }
            }

            override fun onServiceDisconnected(name: ComponentName) = Unit
        }
        try {
            if (!CustomTabsClient.bindCustomTabsService(this, packageName, serviceConnection)) fallbackToBrowser(uri)
        } catch (_: Exception) {
            fallbackToBrowser(uri)
        }
    }

    private fun fallbackToBrowser(uri: Uri) {
        try {
            androidx.browser.customtabs.CustomTabsIntent.Builder().build().launchUrl(this, uri)
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun safeStoreUri(raw: String?): Uri? {
        if (raw.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(raw)
            if (uri.scheme == "https" && uri.host.equals("dreminexfashion.store", true) && (uri.port == -1 || uri.port == 443) && uri.userInfo == null) uri else null
        } catch (_: Exception) { null }
    }
}
