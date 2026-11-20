val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("testImplementation", catalog.findLibrary("assertj-core").get())
}
