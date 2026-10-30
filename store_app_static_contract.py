#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parent
main = (ROOT / "app/src/main/java/com/dreminexfashion/store/StoreHomeActivity.kt").read_text()
api = (ROOT / "app/src/main/java/com/dreminexfashion/store/network/StoreApi.kt").read_text()
models = (ROOT / "app/src/main/java/com/dreminexfashion/store/data/Models.kt").read_text()
gradle = (ROOT / "app/build.gradle.kts").read_text()
manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
assert "wp-json/dreminex/v1/store-app" in api
assert "site_url" in models and "branding" in models and "store_app" in models
assert "TextDecoration.LineThrough" in main
assert "StoreUpdateCard" in main
assert "UpdateRepository" in main
assert "androidx.browser" not in gradle
assert "CustomTabs" not in main and "TrustedWebActivity" not in main
assert "android:usesCleartextTraffic=\"false\"" in manifest
assert "android.permission.INTERNET" in manifest
assert not (ROOT / "app/src/main/java/com/dreminexfashion/store/StoreHomeScreen.kt").exists()
print("Dreminex Fashion native store source contracts: PASS")
