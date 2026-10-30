package com.dreminexfashion.store.network

import com.dreminexfashion.store.data.Branding
import com.dreminexfashion.store.data.StoreManifest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestContractTest {
    @Test fun manifestKeepsRuntimeSiteAndBranding() {
        val raw = """{"site_url":"https://dreminexfashion.store/","branding":{"name":"Dreminex Fashion","logo_url":"https://cdn.example/logo.png"},"store_app":{"version":"0.1.0","download_url":"https://github.com/example/app.apk","sha256":"abc"}}"""
        val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<StoreManifest>(raw)
        assertEquals("https://dreminexfashion.store/", parsed.site_url)
        assertEquals("Dreminex Fashion", parsed.branding.name)
        assertTrue(parsed.store_app.download_url.startsWith("https://"))
    }

    @Test fun defaultsHaveSafeBranding() {
        val branding = Branding()
        assertEquals("Dreminex Fashion", branding.name)
        assertTrue(branding.theme_color.startsWith("#"))
    }
}
