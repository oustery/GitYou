plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // kapt is only used by the prism4j-bundler annotation processor (markdown
    // syntax highlighting); Hilt itself runs through KSP.
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Base version, defined in gradle.properties (single source of truth).
// CI overrides the effective version per build (see .github/workflows/build.yml).
val baseVersionName = project.property("gityouBaseVersionName").toString().trim()
val baseVersionCode = project.property("gityouBaseVersionCode").toString().trim().toInt()

android {
    namespace = "com.gityou.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gityou.app"
        minSdk = 26
        targetSdk = 36
        // CI overrides both so an APK built there reports exactly the version
        // of its release tag, e.g. "1.2.0.21" for tag v1.2.0.21; the in-app
        // updater then only offers releases that are strictly newer than the
        // installed one. Local builds keep the base version.
        versionCode = project.findProperty("GITYOU_VERSION_CODE")?.toString()?.trim()?.toIntOrNull() ?: baseVersionCode
        versionName = project.findProperty("GITYOU_VERSION_NAME")?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: baseVersionName

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
        // Restored per audit F-027: lint errors fail the build instead of
        // being silently ignored; CI reports warnings in the logs.
        abortOnError = true
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

tasks.withType<Test>().configureEach {
    // mockk's inline instrumentation agent plus coroutine-based ViewModel
    // tests need more than the 512m default heap.
    maxHeapSize = "2g"
    // The inline agent's transformed-class cache grows with every test class
    // executed in the same JVM (OOM by the second class at 1g, and a blown
    // heap also breaks the agent so later final-class mocks NPE). A fresh
    // JVM per test class keeps memory bounded and the agent state clean.
    forkEvery = 1
}

dependencies {
    // prism4j (pulled in by markwon-syntax-highlight) depends on the legacy
    // org.jetbrains:annotations-java5 jar whose classes are fully contained
    // in the modern org.jetbrains:annotations; keeping both breaks the
    // duplicate-classes check.
    configurations.configureEach {
        exclude(group = "org.jetbrains", module = "annotations-java5")
    }

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    // NavigationSuiteScaffold: adaptive navigation bar/rail/drawer selection.
    // Aligned with the material3 override so both live in the same alpha wave.
    implementation(libs.compose.material3.adaptive.navigation.suite)
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
    implementation(libs.markwon.image.svg)
    implementation(libs.markwon.image.gif)
    // Fenced-code syntax highlighting (Prism4j-based, F-011).
    implementation(libs.markwon.syntax.highlight)
    // Annotation processor that generates the prism4j GrammarLocator with all
    // bundled languages; consumed by the kapt configuration below.
    kapt(libs.prism4j.bundler)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
}
