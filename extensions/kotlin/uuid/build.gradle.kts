extension {
    name = "extensions/hxreborn/kotlin-uuid.mpe"
}

android {
    namespace = "app.hxreborn.extension.kotlin.uuidpayload"

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
        }
    }
}

val kotlinStdlib = configurations.detachedConfiguration(dependencies.create("org.jetbrains.kotlin:kotlin-stdlib:2.4.21"))
    .apply { isTransitive = false }

dependencies {
    implementation(files(kotlinStdlib))
}
