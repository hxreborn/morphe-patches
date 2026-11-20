rootProject.name = "morphe-patches"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/MorpheApp/registry")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
        maven { url = uri("https://jitpack.io") }
    }
}

plugins {
    id("app.morphe.patches") version "2.1.0"
}

include(":extension-tests", ":extension-test-fixtures")

rootDir.resolve("extensions").walkTopDown()
    .onEnter { it.name != "build" && it.name != "node_modules" }
    .filter { it.name == "library.gradle.kts" }
    .forEach { script ->
        include(":extensions:" + script.parentFile.relativeTo(rootDir.resolve("extensions")).invariantSeparatorsPath.replace('/', ':'))
    }
