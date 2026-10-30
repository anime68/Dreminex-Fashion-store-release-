# Dreminex Fashion native store app

This private repository is the canonical source for the customer-facing **native** Android storefront. It uses Jetpack Compose and the WordPress store-app API; it does not use a WebView, PWA wrapper, Trusted Web Activity or Custom Tab for the store UI.

## Repository split

- **Private source:** `anime68/Dreminex-Fashion-store` — native Kotlin source, tests, design assets and CI validation.
- **Public release:** `anime68/Dreminex-Fashion-store-release-` — release mirror and signed APK workflow. APK bytes and update manifests are published there, never bundled into the WordPress plugin.
- **Website bridge:** the lightweight `Dreminex Native Store App` WordPress plugin lives with the admin plugin source. It stores site branding and the two release-channel manifest URLs, then renders the footer `Install Dreminex App` button shown in the Dreminex Fashion layout.

The plugin's default production manifest is:

`https://github.com/anime68/Dreminex-Fashion-store-release-/releases/latest/download/update-manifest.json`

## Native storefront behavior

The app reads `/wp-json/dreminex/v1/store-app/manifest`, `/catalog` and `/categories` from the configured site. Products, sale/regular prices, stock, images and category changes refresh on launch/foreground. Product cards use the regular price in red crossed text and the current selling price in the normal accent color. Home, search, account, favourites and cart are native Compose screens.

The website owns the only first-install button. The app does not render an install button. The website claim endpoint is one-per-logged-in-account and returns the existing signed APK URL on a repeat request.

The app also checks the public release repository for a stable signed update, validates the manifest, package, version code, SHA-256 and signing certificate, then hands installation to Android's confirmation screen. Signing secrets stay in the protected release environment.

## Validation

```sh
./gradlew --no-daemon testDebugUnitTest testReleaseUnitTest assembleDebug
```

A signed release requires the four `DREMINEX_FASHION_*` environment secrets described in the release workflow. Do not commit a keystore, password, APK or token.
