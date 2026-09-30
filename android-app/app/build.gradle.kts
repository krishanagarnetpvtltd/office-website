plugins { id("com.android.application") }

android {
    namespace = "com.krishnanagar.netapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.krishnanagar.netapp"
        minSdk = 23
        targetSdk = 34
        versionCode = 5
        versionName = "3.0.5"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
