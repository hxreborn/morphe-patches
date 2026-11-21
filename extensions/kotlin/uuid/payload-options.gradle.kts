val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
        }
    }
}

val kotlinVersion = catalog.findVersion("kotlin-stdlib").get().requiredVersion
val kotlinStdlib = configurations.detachedConfiguration(dependencies.create("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"))
    .apply { isTransitive = false }

dependencies {
    add("implementation", files(kotlinStdlib))
}
