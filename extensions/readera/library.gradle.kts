extensions.configure<com.android.build.api.dsl.LibraryExtension> {
    defaultConfig {
        buildConfigField("String", "BUNDLE_VERSION", "\"${project.version}\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    add("compileOnly", project(":extensions:shared:library"))
    add("testImplementation", project(":extensions:shared:library"))
}
