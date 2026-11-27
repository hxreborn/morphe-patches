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
    id("app.morphe.patches") version "2.1.2"
}

val extensionsDirectory = rootDir.resolve("extensions")
val payloadBuildFile = rootDir.resolve("config/extension/payload.gradle.kts")

extensionsDirectory.listFiles().orEmpty().filter { it.isDirectory }.sortedBy { it.name }.forEach { module ->
    if (module.resolve("library.gradle.kts").isFile) {
        include(":extensions:${module.name}")
    }
    module.listFiles().orEmpty().filter {
        it.resolve("classes.list").isFile || it.resolve("payload-options.gradle.kts").isFile
    }.sortedBy { it.name }.forEach { group ->
        val path = ":extensions:${module.name}:${group.name}"
        include(path)
        project(path).buildFileName = payloadBuildFile.relativeTo(group).invariantSeparatorsPath
    }
}
