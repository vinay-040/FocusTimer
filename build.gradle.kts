plugins {
    // The following plugins are declared here and applied in the app-level build.gradle.kts file
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}
