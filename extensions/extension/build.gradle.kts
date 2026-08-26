extension {
    name = "extensions/extension.mpe"
}

android {
    namespace = "app.hxreborn.extension"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "BUNDLE_VERSION", "\"${project.version}\"")
    }
}

dependencies {
    implementation(libs.morphe.extensions.library)
    compileOnly(libs.okhttp)
}
