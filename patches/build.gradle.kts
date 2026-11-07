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

java {
    sourceCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(libs.morphe.patches.library)
    implementation(libs.pngj)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation("app.morphe:morphe-patcher:${libs.versions.morphe.patcher.get()}")
}

tasks {
    jar {
        exclude("ar/com/hjg/pngj/pixels/ImageLine*.class")
        exclude(
            "com/google/gson/**",
            "META-INF/maven/com.google.code.gson/**",
            "META-INF/proguard/gson.pro",
            "app/morphe/util/PatchListGeneratorKt*.class",
            "app/morphe/util/JsonPatch*.class",
            "app/morphe/util/JsonCompatibility*.class",
        )
        exclude("LICENSE.txt", "NOTICE.txt")
        from(rootProject.file("LICENSE"))
        from(rootProject.file("NOTICE"))
        from(rootProject.file("third-party/licenses")) { into("META-INF/third-party/licenses") }

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
        filesMatching("proton-bundle-version.txt") {
            expand("version" to project.version)
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }

    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn("buildAndroid")

        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("app.morphe.util.PatchListGeneratorKt")
    }

    publish {
        dependsOn("generatePatchesList")
    }
}
