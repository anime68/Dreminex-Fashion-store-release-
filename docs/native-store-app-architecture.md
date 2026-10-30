# Native Store App architecture

## Request flow

```text
Website footer button
  -> WordPress /wp-json/dreminex/v1/store-app/claim (one logged-in account claim)
  -> public GitHub release APK
  -> Android native Compose app
  -> WordPress manifest/catalog/categories API
```

The plugin only holds app name, short name, logo, colors, release channel and trusted GitHub manifest URLs. The APK is never included in the plugin archive. When the native plugin is activated, it deactivates the old Dreminex PWA plugin and preserves the PWA option/data so the two install systems cannot compete.

## Release split

The private repository owns source and tests. The public release repository owns the signed release workflow, `dreminex-fashion-release.apk`, `update-manifest.json` and checksums. A release manifest uses the existing updater schema:

```json
{
  "schema_version": 1,
  "channel": "stable",
  "version_name": "1.1.0",
  "version_code": 1001000,
  "package_name": "com.dreminexfashion.store",
  "min_sdk": 26,
  "apk": {
    "browser_download_url": "https://github.com/anime68/Dreminex-Fashion-store-release-/releases/download/v1.1.0/dreminex-fashion-release.apk",
    "sha256": "64-lowercase-hex-digest",
    "size_bytes": 12345678
  }
}
```

The plugin normalizes this release manifest into its `store_app` contract. This keeps the WordPress ZIP small and lets the app update from the public release repository without changing the site plugin.
