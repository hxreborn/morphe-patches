extensions.configure<com.android.build.api.dsl.LibraryExtension> {
    namespace = "app.hxreborn.extension.castlepayload"
}

dependencies {
    add("compileOnly", project(":extensions:shared"))
    add("testImplementation", project(":extensions:shared"))
    add("testImplementation", testFixtures(project(":extensions:shared")))
}
