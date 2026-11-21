val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<com.android.build.api.dsl.LibraryExtension> {
    testFixtures.enable = true
    lint.ignoreTestFixturesSources = true
}

extra["extensionCheckstyleSources"] = files("src/main/java", "src/testFixtures/java")

dependencies {
    add("testImplementation", catalog.findLibrary("assertj-core").get())
}
