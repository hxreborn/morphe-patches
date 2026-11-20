extensions.configure<com.android.build.api.dsl.LibraryExtension> {
    namespace = "app.hxreborn.extension.castlepayload"
}

dependencies {
    add("compileOnly", project(":extensions:shared:library"))
    add("testImplementation", project(":extensions:shared:library"))
    add("testImplementation", project(":extension-test-fixtures"))
}
