plugins { id("com.android.application") }

android {
    namespace = "com.krishnanagar.netapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.krishnanagar.netapp"
        minSdk = 23
        targetSdk = 33
        versionCode = 3
        versionName = "3.0.3"
    }

    signingConfigs {
        create("ciRelease") {
            val store = rootProject.file("ci-release.jks")
            storeFile = store
            storePassword = providers.gradleProperty("ciStorePassword").orNull ?: "KrishnanagarNetBuild2026"
            keyAlias = providers.gradleProperty("ciKeyAlias").orNull ?: "krishnanagar"
            keyPassword = providers.gradleProperty("ciKeyPassword").orNull ?: "KrishnanagarNetBuild2026"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("ciRelease")
        }
    }
}
