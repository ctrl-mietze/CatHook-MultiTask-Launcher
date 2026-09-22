plugins { id("com.android.application") }

android {
    namespace = "com.catcore.ctrlmietze.multitask"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.catcore.ctrlmietze.multitask"
        minSdk = 28
        targetSdk = 36
        versionCode = 20007
        versionName = "2.0.0.0-dev8"
    }

    signingConfigs {
        create("release") {
            val signingStore = System.getenv("CATHOOK_STORE_FILE")
            if (!signingStore.isNullOrBlank()) {
                storeFile = file(signingStore)
                storePassword = System.getenv("CATHOOK_STORE_PASSWORD")
                keyAlias = System.getenv("CATHOOK_KEY_ALIAS")
                keyPassword = System.getenv("CATHOOK_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.core:core:1.16.0")
    compileOnly("de.robv.android.xposed:api:82") {
        isTransitive = false
    }
}

configurations.configureEach {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
}
