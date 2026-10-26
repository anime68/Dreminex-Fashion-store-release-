# Dreminex Fashion Android Store

Standalone customer app for `https://dreminexfashion.store/`. The store pages, live products, pricing, cart, account and checkout remain on the website in a Trusted Web Activity (TWA), with a Custom Tab/browser fallback. There is no privileged JavaScript bridge and no WordPress, WooCommerce or MCP administrator credential in the app.

## App identity

- Label: Dreminex Fashion
- Package: `com.dreminexfashion.store` (`.debug` suffix in debug builds)
- Canonical development source: private `anime68/Dreminex-Fashion-store`
- Public release channel: `anime68/Dreminex-Fashion-store-release-` (a source mirror builds and publishes signed releases)
- APK asset: `dreminex-fashion-release.apk`
- Pinned release certificate SHA-256 is public in Gradle configuration and `docs/assetlinks.json`.

The separate Store signing key is never committed. Future release builds use a GitHub environment named `dreminex-fashion-release` in the public release repository, isolated from Nexus signing credentials. Add these environment secrets there:

- `DREMINEX_FASHION_KEYSTORE_B64` — base64 encoding of the Store JKS file
- `DREMINEX_FASHION_STORE_PASSWORD`
- `DREMINEX_FASHION_KEY_ALIAS`
- `DREMINEX_FASHION_KEY_PASSWORD`

The first v1.0.1 release uses the verified prebuilt APK, so those secrets are not needed to publish that bootstrap release.

## Updater behavior

The app checks the public GitHub latest stable release at most once every 24 hours on launch and on a user-requested check. It ignores prereleases, selects exact expected assets from the pinned repository, validates the release manifest, version code, package, minimum Android version and SHA-256, then checks the downloaded APK signer against both the installed package and the pinned certificate. Installation is always handed to Android's user-confirmed package installer. If GitHub is missing/unavailable, browsing and checkout continue normally.

The tag-triggered workflow publishes the signed APK and manifest as a stable release only after release tests pass. The first release can use a prebuilt APK signed by the protected Store certificate. Later releases are built from source using the protected release environment. Before the first release exists, the GitHub API may return 404; the app presents that as an update-check status and does not interrupt shopping.

## Build and test

```sh
./gradlew testDebugUnitTest assembleDebug
```

For a local signed release build, export `DREMINEX_FASHION_KEYSTORE_PATH`, `DREMINEX_FASHION_STORE_PASSWORD`, `DREMINEX_FASHION_KEY_ALIAS`, and `DREMINEX_FASHION_KEY_PASSWORD` from a protected secret manager, then run:

```sh
./gradlew testReleaseUnitTest assembleRelease
```

Do not put signing material into this repository or artifacts. A stable release must also pass real-device fresh-install and same-signer upgrade tests; a debug/emulator build is not proof of that gate.

## TWA domain association

`docs/assetlinks.json` contains the public Store certificate association to be hosted at `https://dreminexfashion.store/.well-known/assetlinks.json` with `application/json`. The file is a release handoff, not a live website edit. Until it is deployed and verified, browsers may show their normal Custom Tab toolbar.

## Privacy

This app does not collect app telemetry. The storefront uses the website's own session and privacy policy. Update checks contact GitHub's public API and, only after a stable release is available, download the public APK. Android itself handles the install permission and confirmation.
