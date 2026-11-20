val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("compileOnly", project(":extensions:shared:library"))
    add("compileOnly", catalog.findLibrary("androidx-annotation").get())
    add("testImplementation", project(":extensions:shared:library"))
}
