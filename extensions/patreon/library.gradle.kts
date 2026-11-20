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
    add("compileOnly", catalog.findLibrary("androidx-annotation").get())
    add("testImplementation", project(":extensions:shared"))
}
