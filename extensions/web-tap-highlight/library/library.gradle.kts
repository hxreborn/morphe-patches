val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

extra["webAssetsPackage"] = "app.hxreborn.extension.shared"

dependencies {
    add("compileOnly", catalog.findLibrary("morphe-extensions-library").get())
}
