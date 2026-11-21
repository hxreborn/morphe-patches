extra["extensionCheckstyleSources"] = files()

extensions.configure<com.android.build.api.dsl.LibraryExtension> {
    lint.baseline = file("lint-baseline.xml")
}
