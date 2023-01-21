val android_compile_sdk: Int by rootProject.extra
val android_min_sdk: Int by rootProject.extra
val android_target_sdk: Int by rootProject.extra

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.android.library)
}

kotlin {
    android {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
    // Kotlin.Native disabled until the common API is stable and the android portion is done
    /*val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }*/

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                // Need to use project.dependencies.platform() instead of platform()
                // due to this bug: https://youtrack.jetbrains.com/issue/KT-40489
                implementation(project.dependencies.platform("org.jetbrains.kotlin:kotlin-bom"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.core)
                implementation(libs.nordic.ble.common)
                implementation(libs.nordic.ble.ktx)
            }
        }
        // val nativeMain by getting
    }
}

android {
    namespace = "org.asteroidos.link"
    compileSdk = android_compile_sdk
    defaultConfig {
        minSdk = android_min_sdk
        targetSdk = android_target_sdk
    }
    sourceSets {
        getByName("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
