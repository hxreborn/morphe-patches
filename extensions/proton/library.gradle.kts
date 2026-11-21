val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("compileOnly", project(":extensions:shared"))
    add("compileOnly", catalog.findLibrary("morphe-extensions-library").get())
    add("compileOnly", catalog.findLibrary("androidx-annotation").get())
    add("compileOnly", catalog.findLibrary("androidx-compose-ui").get())
    add("compileOnly", catalog.findLibrary("androidx-compose-runtime").get())
    add("compileOnly", catalog.findLibrary("androidx-compose-foundation").get())
    add("testImplementation", project(":extensions:shared"))
    add("testImplementation", catalog.findLibrary("morphe-extensions-library").get())
    add("testImplementation", project(":extensions:web-tap-highlight"))
}
