val android_compile_sdk: Int by rootProject.extra
val android_min_sdk: Int by rootProject.extra
val android_target_sdk: Int by rootProject.extra

plugins {
    kotlin("android")
    alias(libs.plugins.android.application)
}

android {
    namespace = "org.asteroidos.link.androidapp"
    compileSdk = android_compile_sdk

    defaultConfig {
    	applicationId = "org.asteroidos.link.androidapp"
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
    implementation(androidAppLibs.androidx.lifecycle)
    implementation(androidAppLibs.androidx.activity)
    implementation(androidAppLibs.androidx.activity.compose)
    implementation(androidAppLibs.androidx.compose.ui)
    implementation(androidAppLibs.androidx.compose.ui.tooling)
    implementation(androidAppLibs.androidx.compose.material)
    implementation(androidAppLibs.androidx.compose.foundation)
    implementation(androidAppLibs.androidx.navigation.runtime)
    implementation(androidAppLibs.androidx.navigation.compose)
    // Need to use project.dependencies.platform() instead of platform()
    // due to this bug: https://youtrack.jetbrains.com/issue/KT-40489
    implementation(project.dependencies.platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(project(":asteroidos-link"))
}
