package com.dreminexfashion.store.update

import org.json.JSONObject
import java.net.URI

data class ReleaseManifest(
    val channel: String,
    val versionName: String,
    val versionCode: Long,
    val packageName: String,
    val minSdk: Int,
    val apkUrl: String,
    val apkSha256: String,
    val apkSizeBytes: Long,
    val releaseNotes: String
) {
    companion object {
        fun parse(json: String): ReleaseManifest {
            val root = JSONObject(json)
            require(root.optInt("schema_version") == 1) { "Unsupported update manifest." }
            val apk = root.getJSONObject("apk")
            val manifest = ReleaseManifest(
                channel = root.getString("channel"),
                versionName = root.getString("version_name"),
                versionCode = root.getLong("version_code"),
                packageName = root.getString("package_name"),
                minSdk = root.getInt("min_sdk"),
                apkUrl = apk.getString("browser_download_url"),
                apkSha256 = apk.getString("sha256").lowercase(),
                apkSizeBytes = apk.getLong("size_bytes"),
                releaseNotes = root.optString("release_notes", "").take(4000)
            )
            require(manifest.channel == "stable") { "Only stable releases can update this app." }
            require(manifest.versionName.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+"))) { "Invalid release version." }
            require(manifest.versionCode > 0) { "Invalid release version code." }
            require(semanticVersionCode(manifest.versionName) == manifest.versionCode) { "The release version name and code disagree." }
            require(manifest.packageName == EXPECTED_PACKAGE) { "The release belongs to a different app." }
            require(manifest.minSdk in 21..100) { "Invalid Android requirement." }
            require(manifest.apkSha256.matches(Regex("[a-f0-9]{64}"))) { "Invalid release checksum." }
            require(manifest.apkSizeBytes in 1..MAX_APK_SIZE_BYTES) { "Invalid release size." }
            require(isTrustedReleaseUrl(manifest.apkUrl, releaseTag = null, expectedAsset = APK_ASSET)) {
                "The APK download location is not trusted."
            }
            return manifest
        }
    }
}

const val EXPECTED_PACKAGE = "com.dreminexfashion.store"
const val APK_ASSET = "dreminex-fashion-release.apk"
const val MANIFEST_ASSET = "update-manifest.json"
const val MAX_APK_SIZE_BYTES = 150L * 1024L * 1024L

fun isTrustedReleaseUrl(url: String, releaseTag: String?, expectedAsset: String): Boolean {
    return try {
        val uri = URI(url)
        if (uri.scheme != "https" || uri.host != "github.com" || uri.userInfo != null || uri.port !in listOf(-1, 443)) return false
        val segments = uri.path.split('/').filter(String::isNotEmpty)
        if (segments.size != 6) return false
        if (segments[0] != "anime68" || segments[1] != "Dreminex-Fashion-store-release-" || segments[2] != "releases" || segments[3] != "download") return false
        val tag = segments[4]
        if (!tag.matches(Regex("v[0-9]+\\.[0-9]+\\.[0-9]+"))) return false
        if (releaseTag != null && releaseTag != tag) return false
        segments[5] == expectedAsset && uri.query == null && uri.fragment == null
    } catch (_: Exception) {
        false
    }
}

fun isNewerRelease(remoteVersionCode: Long, installedVersionCode: Long): Boolean = remoteVersionCode > installedVersionCode

fun semanticVersionCode(versionName: String): Long? {
    val match = Regex("^(0|[1-9][0-9]{0,3})\\.(0|[1-9][0-9]{0,2})\\.(0|[1-9][0-9]{0,2})$").matchEntire(versionName) ?: return null
    val code = match.groupValues[1].toLong() * 1_000_000L + match.groupValues[2].toLong() * 1_000L + match.groupValues[3].toLong()
    return code.takeIf { it in 1L..2_100_000_000L }
}
