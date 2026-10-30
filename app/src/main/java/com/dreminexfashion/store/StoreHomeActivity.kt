package com.dreminexfashion.store

import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dreminexfashion.store.update.UpdateCheckResult
import com.dreminexfashion.store.update.UpdateRepository
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.dreminexfashion.store.data.PriceFormatting
import com.dreminexfashion.store.data.StoreProduct

private val Ink = Color(0xFF111820)
private val Aqua = Color(0xFF08B6D3)
private val AquaDeep = Color(0xFF087E92)
private val Cloud = Color(0xFFF7FAFB)
private val Coral = Color(0xFFFF665B)
private val Gold = Color(0xFFFFB000)

class StoreHomeActivity : ComponentActivity() {
    private val model: StoreViewModel by viewModels { StoreViewModel.Factory(BuildConfig.STORE_URL) }
    private val updateRepository = UpdateRepository()
    private var updateCheck by androidx.compose.runtime.mutableStateOf<UpdateCheckResult?>(null)
    private var downloadedApk: File? = null
    private var checkingUpdate by androidx.compose.runtime.mutableStateOf(false)
    private var downloadingUpdate by androidx.compose.runtime.mutableStateOf(false)
    private var updateProgress by androidx.compose.runtime.mutableFloatStateOf(0f)
    private var updateMessage by androidx.compose.runtime.mutableStateOf("Updates are checked from the public Store release repository.")
    private var updateError by androidx.compose.runtime.mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DreminexFashionTheme {
                StoreApp(
                    model = model,
                    update = updateCheck,
                    checkingUpdate = checkingUpdate,
                    downloadingUpdate = downloadingUpdate,
                    updateProgress = updateProgress,
                    updateMessage = updateMessage,
                    updateError = updateError,
                    onCheckUpdates = { checkForUpdates(true) },
                    onDownloadUpdate = { downloadUpdate() },
                    onInstallUpdate = { installDownloadedUpdate() },
                )
            }
        }
        checkForUpdates(false)
    }

    private fun checkForUpdates(manual: Boolean) {
        val prefs = getSharedPreferences("store_updater", MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (!manual && now - prefs.getLong("last_check_ms", 0L) < 24L * 60L * 60L * 1000L) return
        if (checkingUpdate) return
        checkingUpdate = true; updateError = false; updateMessage = "Checking for a verified Store release…"
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { updateRepository.check(BuildConfig.VERSION_CODE.toLong(), android.os.Build.VERSION.SDK_INT) }
                updateCheck = result; downloadedApk = null
                updateMessage = if (result == null) "You are using the latest store app version." else "Version ${result.release.versionName} is ready when you are."
                prefs.edit().putLong("last_check_ms", now).apply()
            } catch (error: Exception) {
                updateError = true; updateMessage = error.message?.take(180) ?: "Update check failed. Your current app is unchanged."
            } finally { checkingUpdate = false }
        }
    }

    private fun downloadUpdate() {
        val selected = updateCheck ?: return
        if (downloadingUpdate) return
        downloadingUpdate = true; updateProgress = 0f; updateError = false; updateMessage = "Downloading and checking the signed Store app…"
        val target = File(cacheDir, "updates/dreminex-fashion-release.apk"); target.delete()
        lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { updateRepository.downloadAndVerify(selected.release, target) { current, total -> if (total > 0) updateProgress = (current.toFloat() / total).coerceIn(0f, 1f) } }
                withContext(Dispatchers.IO) { verifyApk(file, selected) }
                downloadedApk = file; updateMessage = "Download verified. Android will ask you to confirm the update."
            } catch (error: Exception) { target.delete(); downloadedApk = null; updateError = true; updateMessage = error.message?.take(180) ?: "The update could not be verified." }
            finally { downloadingUpdate = false }
        }
    }

    @Suppress("DEPRECATION")
    private fun verifyApk(file: File, selected: UpdateCheckResult) {
        val archiveInfo = (if (android.os.Build.VERSION.SDK_INT >= 28) {
            packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
        } else {
            packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNATURES)
        }) ?: error("The downloaded file is not a valid Android app.")
        require(archiveInfo.packageName == BuildConfig.APPLICATION_ID) { "This update is for a different app." }
        val archiveCode = if (android.os.Build.VERSION.SDK_INT >= 28) archiveInfo.longVersionCode else archiveInfo.versionCode.toLong()
        require(archiveCode == selected.release.versionCode && archiveCode > BuildConfig.VERSION_CODE) { "The app version does not match the verified release." }
        val currentInfo = if (android.os.Build.VERSION.SDK_INT >= 28) packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())) else packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        val expected = BuildConfig.RELEASE_CERT_SHA256.lowercase().filter(Char::isLetterOrDigit)
        val current = signerFingerprints(currentInfo); val archive = signerFingerprints(archiveInfo)
        require(current.contains(expected) && archive.contains(expected) && archive == current) { "The update signing identity is not recognized." }
    }

    @Suppress("DEPRECATION")
    private fun signerFingerprints(info: PackageInfo): Set<String> {
        val signatures = if (android.os.Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners?.toList().orEmpty() else info.signatures?.toList().orEmpty()
        return signatures.map { signature -> MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) } }.toSet()
    }

    private fun installDownloadedUpdate() {
        val apk = downloadedApk ?: return
        if (!apk.exists()) { updateError = true; updateMessage = "The verified update file is gone. Download it again."; downloadedApk = null; return }
        if (android.os.Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) { updateMessage = "Allow Dreminex Fashion to install this verified update, then return here."; startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))); return }
        try {
            val contentUri = FileProvider.getUriForFile(this, "$packageName.updates", apk)
            startActivity(Intent(Intent.ACTION_INSTALL_PACKAGE).apply { setDataAndType(contentUri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); putExtra(Intent.EXTRA_RETURN_RESULT, true) })
        } catch (error: Exception) { updateError = true; updateMessage = error.message?.take(160) ?: "Android could not open the installer." }
    }
}

@Composable
private fun DreminexFashionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = AquaDeep,
            secondary = Aqua,
            background = Cloud,
            surface = Color.White,
            onBackground = Ink,
            onSurface = Ink,
        ),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreApp(
    model: StoreViewModel,
    update: UpdateCheckResult?,
    checkingUpdate: Boolean,
    downloadingUpdate: Boolean,
    updateProgress: Float,
    updateMessage: String,
    updateError: Boolean,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
) {
    val state by model.state.collectAsState()
    var selected by remember { mutableStateOf<StoreProduct?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) model.refresh() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Scaffold(
        containerColor = Cloud,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppLogo(state.manifest.branding.logo_url, Modifier.size(44.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(state.manifest.branding.name, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(state.manifest.site_name, fontSize = 10.sp, color = AquaDeep, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = { model.selectTab(StoreTab.HOME) }) { Icon(Icons.Rounded.Menu, "Menu") } },
                actions = { IconButton(onClick = model::refresh) { Icon(Icons.Rounded.Refresh, "Refresh store") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        bottomBar = { StoreBottomBar(state, model) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingScreen()
                state.selectedTab == StoreTab.SEARCH -> SearchScreen(state, model)
                state.selectedTab == StoreTab.ACCOUNT -> AccountScreen(state, update, checkingUpdate, downloadingUpdate, updateProgress, updateMessage, updateError, onCheckUpdates, onDownloadUpdate, onInstallUpdate)
                state.selectedTab == StoreTab.FAVORITES -> EmptyScreen("Your favourites", "Save products here when the wishlist API is enabled.", Icons.Rounded.FavoriteBorder)
                state.selectedTab == StoreTab.CART -> CartScreen(state, model)
                else -> HomeScreen(state, model, onProduct = { selected = it })
            }
            if (state.message.isNotBlank()) {
                Surface(Modifier.align(Alignment.BottomCenter).padding(14.dp), shape = RoundedCornerShape(14.dp), color = Color(0xFFFFF0E8)) {
                    Text(state.message, Modifier.padding(14.dp), color = Coral, fontSize = 12.sp)
                }
            }
        }
    }
    selected?.let { product -> ProductDialog(product, onDismiss = { selected = null }, onAdd = { model.add(product); selected = null }) }
}

@Composable
private fun StoreBottomBar(state: StoreUiState, model: StoreViewModel) {
    val items = listOf(
        StoreTab.HOME to ("Home" to Icons.Rounded.Home),
        StoreTab.SEARCH to ("Search" to Icons.Rounded.Search),
        StoreTab.ACCOUNT to ("Account" to Icons.Rounded.PersonOutline),
        StoreTab.FAVORITES to ("Saved" to Icons.Rounded.FavoriteBorder),
        StoreTab.CART to ("Cart" to Icons.Rounded.ShoppingCart),
    )
    NavigationBar(containerColor = Ink, contentColor = Aqua) {
        items.forEach { (tab, item) ->
            val selected = state.selectedTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { model.selectTab(tab) },
                icon = { Icon(item.second, item.first, tint = if (selected) Aqua else Color.White) },
                label = { Text(item.first, fontSize = 10.sp, color = if (selected) Aqua else Color.White) },
            )
        }
    }
}

@Composable
private fun HomeScreen(state: StoreUiState, model: StoreViewModel, onProduct: (StoreProduct) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Surface(shape = RoundedCornerShape(26.dp), color = Color.Transparent, modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Ink, AquaDeep)), RoundedCornerShape(26.dp))) {
                Column(Modifier.padding(22.dp)) {
                    Text("Style for every day", color = Color.White.copy(alpha = .78f), fontSize = 13.sp)
                    Text("Discover your next favourite", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.LocalOffer, null, tint = Gold); Spacer(Modifier.width(7.dp)); Text(if (state.manifest.features.offers) "Fresh offers and new arrivals" else "New arrivals from our store", color = Color.White, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
        item {
            Text("Categories", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { CategoryChip("All", state.category.isBlank()) { model.chooseCategory("") } }
                items(state.categories) { category -> CategoryChip(category.name, state.category == category.slug) { model.chooseCategory(category.slug) } }
            }
        }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Featured products", fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("${state.products.size}", color = AquaDeep, fontWeight = FontWeight.Bold) } }
        items(state.products.chunked(2)) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { ProductCard(it, Modifier.weight(1f), onClick = { onProduct(it) }, onAdd = { model.add(it) }) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (state.products.isEmpty()) item { EmptyScreen("No products found", "Refresh the store or choose another category.", Icons.Rounded.GridView) }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CategoryChip(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(22.dp), color = if (active) Color(0xFFE9D9FF) else Color.White, border = if (active) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB1B8BD))) {
        Text(label, Modifier.padding(horizontal = 18.dp, vertical = 10.dp), fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun ProductCard(product: StoreProduct, modifier: Modifier, onClick: () -> Unit, onAdd: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column {
            Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)).background(Color(0xFFF1F4F5))) {
                AppProductImage(product.images.firstOrNull(), Modifier.fillMaxSize())
                if (product.on_sale) Surface(Modifier.align(Alignment.TopStart).padding(8.dp), color = Coral, shape = RoundedCornerShape(8.dp)) { Text("Sale", Modifier.padding(horizontal = 7.dp, vertical = 4.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
            Column(Modifier.padding(12.dp)) {
                Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Star, null, tint = Gold, modifier = Modifier.size(14.dp)); Text(" ${product.average_rating}", fontSize = 11.sp, color = Color.Gray) }
                Spacer(Modifier.height(4.dp))
                PriceBlock(product)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onAdd, enabled = product.in_stock, modifier = Modifier.fillMaxWidth().height(36.dp), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = AquaDeep)) { Icon(Icons.Rounded.Add, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(3.dp)); Text(if (product.in_stock) "Add" else "Out", fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun PriceBlock(product: StoreProduct) {
    Column {
        PriceFormatting.regularCrossed(product)?.let { Text(it, color = Coral, fontSize = 11.sp, textDecoration = TextDecoration.LineThrough) }
        Text(PriceFormatting.display(product), color = AquaDeep, fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
}

@Composable
private fun SearchScreen(state: StoreUiState, model: StoreViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Search the store", fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = state.search, onValueChange = model::search, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Search products") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, trailingIcon = { IconButton(onClick = model::applySearch) { Icon(Icons.Rounded.Search, "Search") } })
        Spacer(Modifier.height(18.dp))
        if (state.search.isBlank()) Text("Type a product name, then tap search.", color = Color.Gray)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(state.products) { product -> SearchResult(product, model) } }
    }
}

@Composable
private fun SearchResult(product: StoreProduct, model: StoreViewModel) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { AppProductImage(product.images.firstOrNull(), Modifier.size(64.dp).clip(RoundedCornerShape(10.dp))); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 2); PriceBlock(product) }; IconButton(onClick = { model.add(product) }) { Icon(Icons.Rounded.Add, "Add") } }
    }
}

@Composable
private fun CartScreen(state: StoreUiState, model: StoreViewModel) {
    val lines = model.cartLines()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Your cart", fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        if (lines.isEmpty()) {
            EmptyScreen("Cart is empty", "Add products from the home screen.", Icons.Rounded.ShoppingCart)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(lines) { line ->
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { AppProductImage(line.product.images.firstOrNull(), Modifier.size(68.dp).clip(RoundedCornerShape(10.dp))); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(line.product.name, fontWeight = FontWeight.SemiBold, maxLines = 2); Text(PriceFormatting.display(line.product), color = AquaDeep); Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { model.remove(line.product) }) { Icon(Icons.Rounded.Remove, "Remove") }; Text(line.quantity.toString(), fontWeight = FontWeight.Bold); IconButton(onClick = { model.add(line.product) }) { Icon(Icons.Rounded.Add, "Add") } } } }
                }
            }
        }
        }
        Divider()
        Spacer(Modifier.height(12.dp))
        Text("Checkout is prepared through the site's native store API in the next release.", color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
private fun AccountScreen(
    state: StoreUiState,
    update: UpdateCheckResult?,
    checking: Boolean,
    downloading: Boolean,
    progress: Float,
    updateMessage: String,
    updateError: Boolean,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("My account", fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("Sign in to keep orders, offers and notification preferences synced with ${state.manifest.site_name}.", color = Color.Gray)
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Live store connection", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(state.manifest.site_url, color = AquaDeep, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(12.dp))
                Text(if (state.manifest.features.push) "Notifications are available for this site." else "Notifications are not enabled for this site yet.", color = Color.Gray, fontSize = 13.sp)
            }
        }
        StoreUpdateCard(update, checking, downloading, progress, updateMessage, updateError, onCheck, onDownload, onInstall)
    }
}

@Composable
private fun StoreUpdateCard(
    update: UpdateCheckResult?, checking: Boolean, downloading: Boolean, progress: Float,
    message: String, error: Boolean, onCheck: () -> Unit, onDownload: () -> Unit, onInstall: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SystemUpdate, null, tint = AquaDeep, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp)); Text("App updates", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (checking) { Spacer(Modifier.weight(1f)); CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = AquaDeep) }
            }
            if (update != null) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF27865B), modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Version ${update.release.versionName} is available", fontWeight = FontWeight.SemiBold) }
            }
            Text(message, color = if (error) Coral else Color.Gray, fontSize = 12.sp)
            if (downloading) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = AquaDeep)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (update == null) {
                    OutlinedButton(onClick = onCheck, enabled = !checking && !downloading, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("Check updates", maxLines = 1) }
                } else if (downloading) {
                    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Downloading…") }
                } else {
                    Button(onClick = onDownload, enabled = !checking, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Rounded.Download, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("Download update") }
                    if (message.startsWith("Download verified")) OutlinedButton(onClick = onInstall, shape = RoundedCornerShape(12.dp)) { Text("Install") }
                }
            }
        }
    }
}

@Composable
private fun ProductDialog(product: StoreProduct, onDismiss: () -> Unit, onAdd: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(product.name, fontWeight = FontWeight.Black) }, text = { Column { AppProductImage(product.images.firstOrNull(), Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(14.dp))); Spacer(Modifier.height(12.dp)); PriceBlock(product); Spacer(Modifier.height(8.dp)); Text(product.description.ifBlank { "Product details are synced from the live store." }, maxLines = 5, overflow = TextOverflow.Ellipsis) } }, confirmButton = { Button(onClick = onAdd, enabled = product.in_stock) { Text("Add to cart") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable
private fun AppLogo(url: String, modifier: Modifier) { if (url.isBlank()) Image(painterResource(com.dreminexfashion.store.R.drawable.store_logo), "Dreminex Fashion", modifier, contentScale = ContentScale.Fit) else AsyncImage(model = url, contentDescription = "Dreminex Fashion", modifier = modifier, contentScale = ContentScale.Fit) }

@Composable
private fun AppProductImage(url: String?, modifier: Modifier) { if (url.isNullOrBlank()) Box(modifier.background(Color(0xFFE8EEF0))) else AsyncImage(model = url, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop) }

@Composable
private fun LoadingScreen() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AquaDeep) } }

@Composable
private fun EmptyScreen(title: String, message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) { Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = AquaDeep, modifier = Modifier.size(52.dp)); Spacer(Modifier.height(10.dp)); Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black); Text(message, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center) } }
