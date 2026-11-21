val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<com.android.build.api.dsl.LibraryExtension> {
    defaultConfig {
        buildConfigField("String", "BUNDLE_VERSION", "\"${project.version}\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    add("compileOnly", project(":extensions:shared"))
    add("compileOnly", catalog.findLibrary("morphe-extensions-library").get())
    add("compileOnly", catalog.findLibrary("okhttp").get())
    add("testImplementation", project(":extensions:shared"))
    add("testImplementation", catalog.findLibrary("morphe-extensions-library").get())
}

tasks.matching { it.name.startsWith("check") && it.name.endsWith("AarMetadata") }.configureEach {
    enabled = false
}
