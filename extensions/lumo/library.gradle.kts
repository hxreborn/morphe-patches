val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("compileOnly", catalog.findLibrary("morphe-extensions-library").get())
    add("testImplementation", catalog.findLibrary("morphe-extensions-library").get())
}
