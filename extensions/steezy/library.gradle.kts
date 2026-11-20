val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("compileOnly", catalog.findLibrary("okhttp").get())
    add("testImplementation", catalog.findLibrary("okhttp").get())
}

tasks.matching { it.name.startsWith("check") && it.name.endsWith("AarMetadata") }.configureEach {
    enabled = false
}
