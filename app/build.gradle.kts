plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

fun versionCodeFor(versionName: String): Int {
    val match = Regex("^(0|[1-9][0-9]{0,3})\\.(0|[1-9][0-9]{0,2})\\.(0|[1-9][0-9]{0,2})$").matchEntire(versionName)
        ?: error("Store version must use MAJOR.MINOR.PATCH without leading zeroes.")
    val code = match.groupValues[1].toLong() * 1_000_000L + match.groupValues[2].toLong() * 1_000L + match.groupValues[3].toLong()
    require(code in 1L..2_100_000_000L) { "Store version is outside Android's supported versionCode range." }
    return code.toInt()
}

val storeVersionName = providers.gradleProperty("storeVersionName").orNull?.takeIf(String::isNotBlank) ?: "1.1.0"
val expectedStoreVersionCode = versionCodeFor(storeVersionName)
val storeVersionCode = providers.gradleProperty("storeVersionCode").orNull?.toIntOrNull() ?: expectedStoreVersionCode
require(storeVersionCode == expectedStoreVersionCode) { "storeVersionCode must match the semantic release tag." }

val storeFilePath = System.getenv("DREMINEX_FASHION_KEYSTORE_PATH").orEmpty()
val releaseStorePassword = System.getenv("DREMINEX_FASHION_STORE_PASSWORD").orEmpty()
val releaseKeyAlias = System.getenv("DREMINEX_FASHION_KEY_ALIAS").orEmpty()
val releaseKeyPassword = System.getenv("DREMINEX_FASHION_KEY_PASSWORD").orEmpty()
val hasReleaseKey = listOf(storeFilePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all(String::isNotBlank)
val packagingRelease = gradle.startParameter.taskNames.any {
    it.substringAfterLast(':') in setOf("assembleRelease", "bundleRelease")
}
if (packagingRelease && !hasReleaseKey) error("A Store release package requires the four DREMINEX_FASHION_* signing environment variables.")

android {
    namespace = "com.dreminexfashion.store"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "com.dreminexfashion.store"
        minSdk = 26
        targetSdk = 35
        versionCode = storeVersionCode
        versionName = storeVersionName
        buildConfigField("String", "STORE_URL", "\"https://dreminexfashion.store/\"")
        buildConfigField("String", "RELEASE_OWNER", "\"anime68\"")
        buildConfigField("String", "RELEASE_REPOSITORY", "\"Dreminex-Fashion-store-release-\"")
        buildConfigField("String", "RELEASE_CERT_SHA256", "\"27db86423dd867633070257c7483c65c9df06ff6b02e971f5703a6736436b10d\"")
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        if (hasReleaseKey) create("dreminexFashionRelease") {
            storeFile = file(storeFilePath)
            this.storePassword = releaseStorePassword
            this.keyAlias = releaseKeyAlias
            this.keyPassword = releaseKeyPassword
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-debug" }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseKey) signingConfig = signingConfigs.getByName("dreminexFashionRelease")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
