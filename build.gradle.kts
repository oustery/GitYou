// Root build configuration. Module-specific logic lives in app/build.gradle.kts.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // kapt is only used by the prism4j-bundler annotation processor (markdown
    // syntax highlighting); declaring the version here once avoids the
    // "already on the classpath" plugin-resolution conflict in app.
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    // Static analysis runs from the root project over the whole codebase;
    // the `detekt` task is wired into CI (F-027).
    alias(libs.plugins.detekt)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    source.setFrom(files("app/src/main/java", "app/src/test/java"))
    parallel = true
}
