val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("compileOnly", catalog.findLibrary("androidx-webkit").get())
}
