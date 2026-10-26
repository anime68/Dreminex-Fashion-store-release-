package com.dreminexfashion.store.update

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class UpdateCheckResult(val release: ReleaseManifest, val releaseTag: String)

class UpdateRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(false)
        .build()

    fun check(installedVersionCode: Long, installedMinSdk: Int): UpdateCheckResult? {
        val releaseUrl = "https://api.github.com/repos/anime68/Dreminex-Fashion-store-release-/releases/latest"
        val releaseBody = getText(releaseUrl, 256 * 1024)
        val releaseJson = JSONObject(releaseBody)
        require(!releaseJson.optBoolean("draft", true) && !releaseJson.optBoolean("prerelease", true)) { "No stable app release is available." }
        val tag = releaseJson.getString("tag_name")
        require(tag.matches(Regex("v[0-9]+\\.[0-9]+\\.[0-9]+"))) { "The release tag is invalid." }
        val assets = releaseJson.getJSONArray("assets")
        var apkApiDigest = ""
        var apkUrl = ""
        var manifestUrl = ""
        for (index in 0 until assets.length()) {
            val asset = assets.getJSONObject(index)
            when (asset.optString("name")) {
                APK_ASSET -> {
                    apkUrl = asset.getString("browser_download_url")
                    apkApiDigest = asset.optString("digest").removePrefix("sha256:").lowercase()
                }
                MANIFEST_ASSET -> manifestUrl = asset.getString("browser_download_url")
            }
        }
        require(isTrustedReleaseUrl(apkUrl, tag, APK_ASSET) && isTrustedReleaseUrl(manifestUrl, tag, MANIFEST_ASSET)) {
            "The release is missing the expected signed app assets."
        }
        val manifest = ReleaseManifest.parse(getText(manifestUrl, 64 * 1024))
        require(manifest.apkUrl == apkUrl) { "The update manifest does not match the release asset." }
        if (apkApiDigest.matches(Regex("[a-f0-9]{64}"))) require(manifest.apkSha256 == apkApiDigest) { "The release checksum metadata does not match." }
        require(manifest.versionName == tag.removePrefix("v")) { "The release tag and version disagree." }
        require(manifest.minSdk <= installedMinSdk) { "This update requires a newer Android version." }
        if (!isNewerRelease(manifest.versionCode, installedVersionCode)) return null
        return UpdateCheckResult(manifest, tag)
    }

    fun downloadAndVerify(manifest: ReleaseManifest, destination: File, onProgress: (Long, Long) -> Unit = { _, _ -> }): File {
        require(isTrustedReleaseUrl(manifest.apkUrl, null, APK_ASSET)) { "The APK download location is not trusted." }
        destination.parentFile?.mkdirs()
        val request = Request.Builder().url(manifest.apkUrl).header("Accept", "application/octet-stream").build()
        client.newCall(request).execute().use { response ->
            require(response.isSuccessful) { "APK download failed with HTTP ${response.code}." }
            require(response.request.url.isHttps && response.request.url.host in RELEASE_ASSET_HOSTS) { "APK download was redirected to an untrusted host." }
            val body = response.body ?: error("The release asset was empty.")
            val contentLength = body.contentLength()
            require(contentLength <= MAX_APK_SIZE_BYTES && (contentLength < 0 || contentLength == manifest.apkSizeBytes)) { "The APK size does not match the release manifest." }
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            try {
                body.byteStream().use { input ->
                    FileOutputStream(destination).use { output ->
                        val buffer = ByteArray(32 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                            require(total <= MAX_APK_SIZE_BYTES) { "The APK is larger than allowed." }
                            digest.update(buffer, 0, read)
                            output.write(buffer, 0, read)
                            onProgress(total, if (contentLength > 0) contentLength else manifest.apkSizeBytes)
                        }
                        output.fd.sync()
                    }
                }
                require(total == manifest.apkSizeBytes) { "The downloaded APK size does not match." }
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                require(actual == manifest.apkSha256) { "The APK checksum verification failed." }
                return destination
            } catch (error: Exception) {
                destination.delete()
                throw error
            }
        }
    }

    private fun getText(url: String, maxBytes: Int): String {
        val request = Request.Builder().url(url).header("Accept", "application/vnd.github+json").header("X-GitHub-Api-Version", "2022-11-28").build()
        client.newCall(request).execute().use { response ->
            val expectedFinalHosts = if (url.startsWith("https://api.github.com/")) setOf("api.github.com") else RELEASE_ASSET_HOSTS
            require(response.request.url.isHttps && response.request.url.host in expectedFinalHosts) { "GitHub redirected the update check to an untrusted host." }
            require(response.isSuccessful) {
                when (response.code) {
                    404 -> "No stable app release is configured yet (HTTP 404)."
                    403 -> "GitHub limited update checks for now (HTTP 403). Try again later."
                    else -> "Update check failed with HTTP ${response.code}."
                }
            }
            val body = response.body ?: error("GitHub returned an empty response.")
            require(body.contentLength() <= maxBytes) { "The update response is too large." }
            val bytes = body.bytes()
            require(bytes.size <= maxBytes) { "The update response is too large." }
            return bytes.toString(Charsets.UTF_8)
        }
    }
}

private val RELEASE_ASSET_HOSTS = setOf("github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com")
