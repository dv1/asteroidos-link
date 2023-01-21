val android_compile_sdk: Int by rootProject.extra
val android_min_sdk: Int by rootProject.extra
val android_target_sdk: Int by rootProject.extra

plugins {
    kotlin("android")
    alias(libs.plugins.android.application)
}

android {
    namespace = "org.asteroidos.link.testapp"
    compileSdk = android_compile_sdk

    defaultConfig {
        applicationId = "org.asteroidos.link.testapp"
        minSdk = android_min_sdk
        targetSdk = android_target_sdk
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("src/main/AndroidManifest.xml")
            java.srcDir("src/main/kotlin") // TODO: not "kotlin.srcDirs" ?
            res.srcDirs(file("src/main/res"))
        }
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(testAppLibs.androidx.lifecycle)
    implementation(testAppLibs.androidx.activity)
    implementation(testAppLibs.androidx.activity.compose)
    implementation(testAppLibs.androidx.compose.ui)
    implementation(testAppLibs.androidx.compose.ui.tooling)
    implementation(testAppLibs.androidx.compose.material)
    implementation(testAppLibs.androidx.compose.foundation)
    implementation(testAppLibs.androidx.navigation.runtime)
    implementation(testAppLibs.androidx.navigation.compose)
    // Need to use project.dependencies.platform() instead of platform()
    // due to this bug: https://youtrack.jetbrains.com/issue/KT-40489
    implementation(project.dependencies.platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(project(":asteroidos-link"))
}
