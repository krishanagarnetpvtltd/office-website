plugins { id("com.android.application") }

android {
    namespace = "com.krishnanagar.netapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.krishnanagar.netapp"
        minSdk = 23
        targetSdk = 34
        versionCode = 7
        versionName = "4.0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
