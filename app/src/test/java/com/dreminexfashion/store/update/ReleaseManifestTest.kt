package com.dreminexfashion.store.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseManifestTest {
    @Test fun acceptsOnlyTheConfiguredHttpsReleaseAsset() {
        assertTrue(isTrustedReleaseUrl("https://github.com/anime68/Dreminex-Fashion-store-release-/releases/download/v1.2.3/dreminex-fashion-release.apk", "v1.2.3", APK_ASSET))
        assertFalse(isTrustedReleaseUrl("http://github.com/anime68/Dreminex-Fashion-store-release-/releases/download/v1.2.3/dreminex-fashion-release.apk", "v1.2.3", APK_ASSET))
        assertFalse(isTrustedReleaseUrl("https://example.com/anime68/Dreminex-Fashion-store-release-/releases/download/v1.2.3/dreminex-fashion-release.apk", "v1.2.3", APK_ASSET))
        assertFalse(isTrustedReleaseUrl("https://github.com/attacker/repo/releases/download/v1.2.3/dreminex-fashion-release.apk", "v1.2.3", APK_ASSET))
        assertFalse(isTrustedReleaseUrl("https://github.com/anime68/Dreminex-Fashion-store-release-/releases/download/v1.2.3/other.apk", "v1.2.3", APK_ASSET))
        assertFalse(isTrustedReleaseUrl("https://github.com/anime68/Dreminex-Fashion-store-release-/releases/download/v1.2.3/dreminex-fashion-release.apk?redirect=evil", "v1.2.3", APK_ASSET))
        assertFalse(isTrustedReleaseUrl("https://github.com/anime68/Dreminex-Fashion-store/releases/download/v1.2.3/dreminex-fashion-release.apk", "v1.2.3", APK_ASSET))
    }

    @Test fun onlyStrictlyHigherVersionCodesAreOffered() {
        assertTrue(isNewerRelease(2, 1))
        assertFalse(isNewerRelease(1, 1))
        assertFalse(isNewerRelease(1, 2))
    }

    @Test fun semanticReleaseVersionsProduceMonotonicAndroidCodes() {
        assertEquals(1_000_000L, semanticVersionCode("1.0.0"))
        assertEquals(1_002_003L, semanticVersionCode("1.2.3"))
        assertNull(semanticVersionCode("01.2.3"))
        assertNull(semanticVersionCode("2101.0.0"))
    }

    @Test fun manifestRequiresMatchingSemanticVersionCode() {
        val matching = """{"schema_version":1,"channel":"stable","version_name":"1.2.3","version_code":1002003,"package_name":"com.dreminexfashion.store","min_sdk":26,"apk":{"browser_download_url":"https://github.com/anime68/Dreminex-Fashion-store-release-/releases/download/v1.2.3/dreminex-fashion-release.apk","sha256":"${"a".repeat(64)}","size_bytes":1000}}"""
        assertEquals(1002003L, ReleaseManifest.parse(matching).versionCode)

        val mismatch = matching.replace("1002003", "1002004")
        try {
            ReleaseManifest.parse(mismatch)
            fail("A mismatched version code must be rejected.")
        } catch (_: IllegalArgumentException) {
            // Expected: the release tag and APK version cannot drift.
        }
    }
}
