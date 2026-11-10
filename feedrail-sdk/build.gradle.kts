plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("maven-publish")
}

group = "com.feedrail"
version = "0.8.1"

android {
    namespace = "com.feedrail.sdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    buildFeatures {
        compose = true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    // Expose a single `release` variant for Maven publishing.
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.feedrail"
            artifactId = "feedrail-sdk"
            // Was hardcoded separately from the `version` above and had drifted to
            // 0.1.0 while that one moved to 0.1.1 — JitPack builds its coordinate from
            // the git tag regardless, but a mismatch here is exactly what let the two
            // fall out of sync unnoticed. Single source of truth now.
            version = project.version.toString()
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.vm)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.lifecycle.runtime)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.ext)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.compose.ui.tooling)

    // Networking
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.core)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)

    // Image loading
    implementation(libs.coil.compose)

    // Secure token storage
    implementation(libs.androidx.security.crypto)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
