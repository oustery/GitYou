pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // Google-hosted mirror of Maven Central; used first because
        // repo.maven.apache.org rate-limits some CI networks.
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            // The Hilt plugin marker is not published to every repository;
            // resolve the plugin by its Maven coordinates directly.
            if (requested.id.id == "com.google.dagger.hilt.android") {
                useModule("com.google.dagger:hilt-android-gradle-plugin:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Same mirror rationale as above for library dependencies.
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
    }
}

rootProject.name = "GitYou"
include(":app")
