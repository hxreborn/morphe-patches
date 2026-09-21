import java.util.zip.ZipFile

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
    implementation(libs.pngj)
    patchListGeneratorClasspath(libs.gson)
    testImplementation(kotlin("test"))
}

tasks {
    jar {
        exclude("ar/com/hjg/pngj/pixels/ImageLine*.class")

        doLast {
            val desktopOnlyPackages = listOf("java/awt/", "javax/imageio/", "javax/swing/")
            val offenders = ZipFile(archiveFile.get().asFile).use { zip ->
                zip.entries().asSequence()
                    .filter { it.name.endsWith(".class") }
                    .filter { entry ->
                        val text = zip.getInputStream(entry).readBytes().toString(Charsets.ISO_8859_1)
                        desktopOnlyPackages.any(text::contains)
                    }
                    .map { it.name }
                    .toList()
            }
            if (offenders.isNotEmpty()) {
                throw GradleException("Android has no desktop JDK, referenced from: $offenders")
            }
        }
    }

    processResources {
        inputs.property("version", project.version)
        filesMatching("protonmail-bundle-version.txt") {
            expand("version" to project.version)
        }
    }

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
