plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android { namespace = "com.ironlog.app"; compileSdk = 35
    defaultConfig { applicationId = "com.ironlog.app"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.0" }
}

kotlin { jvmToolchain(21) }

android.buildFeatures { compose = true }

dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(bom); androidTestImplementation(bom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
