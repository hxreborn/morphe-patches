group = "app.hxreborn"

patches {
    about {
        name = "hxreborn Patches"
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

// Separate configuration so gson is available at runtime for the
// generatePatchesList task but never bundled into the APK.
val patchListGeneratorClasspath: Configuration by configurations.creating

dependencies {
    compileOnly(libs.gson)
    implementation(libs.morphe.patches.library)
    patchListGeneratorClasspath(libs.gson)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}