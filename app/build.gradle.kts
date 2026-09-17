plugins { id("com.android.application") }
android {
    namespace = "com.catcore.ctrlmietze.multitask"
    compileSdk = 36
    defaultConfig { applicationId = "com.catcore.ctrlmietze.multitask"; minSdk = 28; targetSdk = 36; versionCode = 1; versionName = "0.1.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
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
