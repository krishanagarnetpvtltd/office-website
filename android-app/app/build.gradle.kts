plugins { id("com.android.application") }

android {
    namespace = "com.krishnanagarnet.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.krishnanagarnet.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "3.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}
