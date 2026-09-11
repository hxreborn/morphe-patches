group = "app.hxreborn"

patches {
    about {
        name = "hxreborn’s patches"
        description = "Patches for the apps I actually use"
        source = "git@github.com:hxreborn/morphe-patches.git"
        author = "hxreborn"
        contact = "https://github.com/hxreborn/morphe-patches/issues"
        website = "https://github.com/hxreborn/morphe-patches"
        license = "GPLv3"
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

// Matches the plugin's targetCompatibility 11 for this module's Java sources
java {
    sourceCompatibility = JavaVersion.VERSION_11
}

val patchListGeneratorClasspath: Configuration by configurations.creating

dependencies {
    compileOnly(libs.gson)
    implementation(libs.morphe.patches.library)
    patchListGeneratorClasspath(libs.gson)
    testImplementation(kotlin("test"))
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }

    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    publish {
        dependsOn("generatePatchesList")
    }
}
