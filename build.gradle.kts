group = "org.asteroidos.link"
version = "0.0.1"

buildscript {
    val android_compile_sdk by extra { 33 }
    val android_min_sdk by extra { 28 }
    val android_target_sdk by extra { 32 }
}

plugins {
    kotlin("multiplatform") version libs.versions.kotlin.get() apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

ktlint {
    debug.set(true)
    verbose.set(true)
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}
