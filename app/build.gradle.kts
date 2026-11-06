plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.gityou.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gityou.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // OAuth Device Flow client id; can be overridden with
        // -PGITYOU_CLIENT_ID=... or directly inside the app.
        buildConfigField(
            "String",
            "GITYOU_CLIENT_ID",
            "\"${project.findProperty("GITYOU_CLIENT_ID")?.toString()?.trim() ?: ""}\"",
        )
    }

    signingConfigs {
        create("release") {
            // Signing is driven by CI secrets (see .github/workflows/build.yml).
            // When the environment variables are absent the release build
            // simply stays unsigned, so local builds keep working.
            val storeFilePath = System.getenv("GITYOU_KEYSTORE_FILE")
            val storePassword = System.getenv("GITYOU_KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("GITYOU_KEY_ALIAS")
            val keyPassword = System.getenv("GITYOU_KEY_PASSWORD")
            if (storeFilePath != null && storePassword != null && keyAlias != null && keyPassword != null) {
                storeFile = File(storeFilePath)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            // R8 passes are intentionally kept off for the first releases;
            // enable together with tuned keep rules and verify on real usage.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val releaseSigning = signingConfigs.findByName("release")
            if (releaseSigning?.storeFile != null) {
                signingConfig = releaseSigning
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // One APK per ABI, as required: armeabi-v7a / arm64-v8a / x86 / x86_64.
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = false
        }
    }

    lint {
        // Vital lint for release keeps running; non-fatal issues must not
        // break artifact production, CI reports them in the logs instead.
        abortOnError = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window.size)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.markwon.core)
    implementation(libs.markwon.html)
    implementation(libs.markwon.ext.tables)
    implementation(libs.markwon.ext.tasklist)
    implementation(libs.markwon.ext.strikethrough)
    implementation(libs.markwon.linkify)
    implementation(libs.markwon.image)

    testImplementation(libs.junit)
}
