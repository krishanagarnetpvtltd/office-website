plugins { id("com.android.application") }

android {
    namespace = "com.krishnanagar.netapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.krishnanagar.netapp"
        minSdk = 21
        targetSdk = 35
        versionCode = 3
        versionName = "3.0.2"
    }

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
