import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "tg.goddivor.jobcalender"
    compileSdk = 37

    defaultConfig {
        applicationId = "tg.goddivor.jobcalender"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // The config key is compiled in, never committed: it lives in local.properties, which is
        // gitignored. Without it the app simply has no sync, which is a working offline app.
        val local = Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
        }
        buildConfigField(
            "String",
            "SYNC_CONFIG_URL",
            "\"${local.getProperty("SYNC_CONFIG_URL", "")}\"",
        )
        buildConfigField(
            "String",
            "SYNC_CONFIG_KEY",
            "\"${local.getProperty("SYNC_CONFIG_KEY", "")}\"",
        )
    }

    // Release signing reads the CI environment first, then local.properties. Neither the keystore
    // nor its password is ever committed: an unsigned release APK cannot be installed, so the
    // signing config simply stays absent when the material is not there, and assembleRelease then
    // produces an unsigned artifact rather than failing in a confusing way.
    val keystoreFile = (System.getenv("KEYSTORE_PATH")
        ?: Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
        }.getProperty("KEYSTORE_PATH"))?.let(::file)

    signingConfigs {
        if (keystoreFile?.exists() == true) {
            create("release") {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                    ?: Properties().apply {
                        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
                    }.getProperty("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS") ?: "jobcalender"
                keyPassword = System.getenv("KEY_PASSWORD")
                    ?: Properties().apply {
                        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
                    }.getProperty("KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Exported schemas are committed so a future migration stays reviewable in a diff.
// The extension is named `room3`, not `room`, and it is a project extension rather than a section
// of the `android` block. Verified in RoomGradlePlugin's bytecode, 3.0.1.
room3 {
    schemaDirectory("$projectDir/schemas")
}

// Pinned because the machine's newest JVM (25) is a JRE with no compiler, and Gradle otherwise
// picks the highest it finds. 17 is also AGP 9's minimum.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

// The Hilt plugin creates its own JavaCompile task, which does not inherit the toolchain above and
// falls back to the daemon's JVM. Pin every JavaCompile rather than only the ones AGP declares.
tasks.withType<JavaCompile>().configureEach {
    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
