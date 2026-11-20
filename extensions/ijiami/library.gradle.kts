val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<com.android.build.api.dsl.LibraryExtension> {
    defaultConfig.minSdk = 28
    lint.baseline = file("lint-baseline.xml")
}

extra["extensionCheckstyleSources"] = files("src/test/java")

dependencies {
    add("testImplementation", catalog.findLibrary("assertj-core").get())
}
