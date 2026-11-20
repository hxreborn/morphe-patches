val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("implementation", catalog.findLibrary("morphe-extensions-library").get())
}

val libraryClasses = tasks.register<Sync>("extractPayloadClasses") {
    from(
        provider {
            configurations.getByName("releaseRuntimeClasspath").incoming.artifactView {
                attributes { attribute(Attribute.of("artifactType", String::class.java), "jar") }
            }.files.map { zipTree(it) }
        },
    )
    into(layout.buildDirectory.dir("payload-classes"))
}

extra["payloadClassesTask"] = "extractPayloadClasses"
extra["payloadClassesDir"] = layout.buildDirectory.dir("payload-classes")
