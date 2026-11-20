plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.xmaitrader.pro"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.xmaitrader.pro"
        minSdk = 23
        targetSdk = 28
        versionCode = 1
        versionName = "0.1"
    }
}
