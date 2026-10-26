package com.dreminexfashion.store

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreminexfashion.store.update.UpdateCheckResult

private val Ink = Color(0xFF17292D)
private val Teal = Color(0xFF087E8B)
private val Canvas = Color(0xFFF3F8F8)

@Composable
fun StoreHomeScreen(
    activeTab: Int,
    update: UpdateCheckResult?,
    checking: Boolean,
    downloading: Boolean,
    downloadProgress: Float,
    message: String,
    messageIsError: Boolean,
    installedVersion: String,
    onTabChange: (Int) -> Unit,
    onOpenStore: (String) -> Unit,
    onCheckUpdates: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit
) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Teal, onPrimary = Color.White, background = Canvas, onBackground = Ink, surface = Color.White, onSurface = Ink)) {
        Scaffold(
            containerColor = Canvas,
            bottomBar = {
                NavigationBar(containerColor = Color.White, modifier = Modifier.navigationBarsPadding()) {
                    NavigationBarItem(selected = activeTab == 0, onClick = { onTabChange(0) }, icon = { androidx.compose.material3.Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                    NavigationBarItem(selected = activeTab == 1, onClick = { onTabChange(1) }, icon = { androidx.compose.material3.Icon(Icons.Default.SystemUpdateAlt, null) }, label = { Text("App updates") })
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                AppHeader(installedVersion)
                if (activeTab == 0) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { StoreHero(onOpenStore) }
                        item { TrustRow() }
                        item { UpdateCard(update, checking, downloading, downloadProgress, message, messageIsError, installedVersion, onCheckUpdates, onDownload, onInstall, compact = true) }
                        item { HelpCard() }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { UpdateCard(update, checking, downloading, downloadProgress, message, messageIsError, installedVersion, onCheckUpdates, onDownload, onInstall, compact = false) }
                        item { PrivacyCard() }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppHeader(version: String) {
    Surface(color = Color.White, shadowElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.df_monogram), contentDescription = "Dreminex Fashion", modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Dreminex Fashion", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("dreminexfashion.store", color = Teal, fontSize = 12.sp)
            }
            Text("v$version", color = Color(0xFF557176), fontSize = 12.sp)
        }
    }
}

@Composable
private fun StoreHero(onOpenStore: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(Color(0xFF0C8793), Color(0xFF13515C)))).padding(24.dp)
    ) {
        Text("DREMINEX  ·  FASHION", color = Color(0xFFBDECF0), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(13.dp))
        Text("Find your next\nfavourite look.", color = Color.White, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Text("Explore the live collection, new arrivals and offers from Dreminex Fashion.", color = Color(0xFFE4F5F5), fontSize = 14.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { onOpenStore("https://dreminexfashion.store/") },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Teal),
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            androidx.compose.material3.Icon(Icons.Default.Shop, null)
            Spacer(Modifier.width(9.dp))
            Text("Explore the store", fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            androidx.compose.material3.Icon(Icons.Default.ArrowOutward, null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TrustRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        TrustPill("Live catalog", Icons.Default.Shop, Modifier.weight(1f))
        TrustPill("Secure checkout", Icons.Default.Security, Modifier.weight(1f))
    }
}

@Composable
private fun TrustPill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Surface(modifier = modifier, color = Color.White, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            androidx.compose.material3.Icon(icon, null, tint = Teal, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(text, color = Ink, fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun UpdateCard(
    update: UpdateCheckResult?, checking: Boolean, downloading: Boolean, progress: Float, message: String,
    messageIsError: Boolean, installedVersion: String, onCheck: () -> Unit, onDownload: () -> Unit,
    onInstall: () -> Unit, compact: Boolean
) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE6F5F5)), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(Icons.Default.SystemUpdateAlt, null, tint = Teal)
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("App updates", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Installed version $installedVersion", fontSize = 12.sp, color = Color(0xFF738286))
                }
                if (checking) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Teal)
            }
            if (update != null) {
                Surface(color = Color(0xFFEAF7F1), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        androidx.compose.material3.Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF27865B), modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Version ${update.release.versionName} is available", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            if (update.release.releaseNotes.isNotBlank()) Text(update.release.releaseNotes, color = Color(0xFF58716D), fontSize = 12.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Text(message, color = if (messageIsError) Color(0xFFB34040) else Color(0xFF637478), fontSize = 13.sp, lineHeight = 19.sp)
            if (downloading) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape), color = Teal, trackColor = Color(0xFFE4EEEE))
            if (downloading) Text("${(progress * 100).toInt()}% downloaded", color = Color(0xFF637478), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                if (update == null) {
                    OutlinedButton(onClick = onCheck, enabled = !checking && !downloading, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        androidx.compose.material3.Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Check for updates", maxLines = 1)
                    }
                } else if (downloading) {
                    OutlinedButton(onClick = onCheck, enabled = false, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Downloading…") }
                } else {
                    Button(onClick = if (compact) onDownload else if (message.startsWith("Download verified")) onInstall else onDownload, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        androidx.compose.material3.Icon(if (message.startsWith("Download verified") && !compact) Icons.Default.CheckCircle else Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if (message.startsWith("Download verified") && !compact) "Install update" else "Download update", maxLines = 1)
                    }
                    if (!compact) OutlinedButton(onClick = onCheck, enabled = !checking, shape = RoundedCornerShape(14.dp)) { Text("Check again") }
                }
            }
            if (update != null && compact) OutlinedButton(onClick = onCheck, enabled = !checking && !downloading, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Review update settings") }
        }
    }
}

@Composable
private fun HelpCard() {
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(Icons.Default.Security, null, tint = Teal, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Your shopping stays on our website", fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 14.sp)
                Text("Prices, products, cart and checkout are loaded live from dreminexfashion.store.", color = Color(0xFF718084), fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun PrivacyCard() {
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Safe, customer-first updates", fontWeight = FontWeight.Bold, color = Ink, fontSize = 17.sp)
            Text("The app checks public stable releases from its configured GitHub project. Downloads are checked for their SHA-256 and Dreminex Fashion signing certificate before Android opens its own install confirmation.", color = Color(0xFF657579), fontSize = 13.sp, lineHeight = 20.sp)
            Text("Updates are optional. Shopping continues if GitHub is unavailable.", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
