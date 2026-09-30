plugins { id("com.android.application") }

android {
    namespace = "com.krishnanagarnet.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.krishnanagarnet.app"
        minSdk = 21
        targetSdk = 35
        versionCode = 2
        versionName = "3.0.1"
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
