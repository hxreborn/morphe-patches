plugins {
    checkstyle
}

extension {
    name = "extensions/extension.mpe"
}

android {
    namespace = "app.hxreborn.extension"

    sourceSets.getByName("test").java.srcDirs("../moviebox/src", "../moviebox/test", "../ijiami/src", "../ijiami/test")

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "BUNDLE_VERSION", "\"${project.version}\"")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("user.timezone", "UTC")
                it.jvmArgs(
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                    "--add-opens=java.base/java.net=ALL-UNNAMED",
                    "--add-opens=java.base/java.security=ALL-UNNAMED",
                    "--add-opens=java.base/java.text=ALL-UNNAMED",
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                )
            }
        }
    }
}

tasks.register<Checkstyle>("checkstyle") {
    source("src/main/java", "../moviebox/src", "../moviebox/test", "../ijiami/test")
    classpath = files()
}

val movieBoxMinSdk = 23

val checkMovieBoxApiLevel = tasks.register<Exec>("checkMovieBoxApiLevel") {
    dependsOn("compileDebugUnitTestJavaWithJavac")
    val apiVersions = androidComponents.sdkComponents.sdkDirectory.map {
        it.file("platforms/android-${android.compileSdk}/data/api-versions.xml").asFile.path
    }
    val classes = layout.buildDirectory.dir("intermediates/javac/debugUnitTest/compileDebugUnitTestJavaWithJavac/classes")
    val sources = layout.projectDirectory.dir("../moviebox/src")
    inputs.dir(classes)
    inputs.dir(sources)
    commandLine(
        "python3",
        rootProject.file(".github/scripts/check_android_api_level.py").path,
        movieBoxMinSdk.toString(),
        apiVersions.get(),
        classes.get().asFile.path,
        sources.asFile.path,
    )
}

tasks.matching { it.name == "lintRelease" }.configureEach {
    dependsOn(checkMovieBoxApiLevel)
}

tasks.named("check") {
    dependsOn("checkstyle")
}

dependencies {
    checkstyle(libs.spring.javaformat.checkstyle)
    implementation(libs.morphe.extensions.library)
    compileOnly(libs.okhttp)
    compileOnly(libs.androidx.annotation)
    compileOnly(libs.androidx.compose.ui)
    compileOnly(libs.androidx.compose.runtime)
    compileOnly(libs.androidx.compose.foundation)
    testImplementation(libs.junit)
    testImplementation(libs.assertj.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.okhttp)
    testImplementation(libs.json)
}

abstract class GenerateWebAssets : DefaultTask() {

    @get:InputDirectory
    abstract val scripts: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val target = outputDirectory.get().asFile.resolve("app/hxreborn/extension/WebAssets.java")
        target.parentFile.mkdirs()

        val fields = scripts.get().asFile.listFiles { file -> file.extension == "js" }
            .orEmpty()
            .sortedBy { it.name }
            .joinToString("\n\n") { file ->
                val name = file.nameWithoutExtension.uppercase().replace('-', '_')
                val literal = file.readText()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .lines()
                    .joinToString("\n            + ") { "\"$it\\n\"" }
                "    public static final String $name =\n            $literal;"
            }

        target.writeText(
            """
            |package app.hxreborn.extension;
            |
            |public final class WebAssets {
            |
            |    private WebAssets() {}
            |
            |$fields
            |}
            |
            """.trimMargin(),
        )
    }
}

val generateWebAssets by tasks.registering(GenerateWebAssets::class) {
    scripts.set(layout.projectDirectory.dir("src/main/js"))
}

androidComponents.onVariants { variant ->
    variant.sources.java?.addGeneratedSourceDirectory(
        generateWebAssets,
        GenerateWebAssets::outputDirectory,
    )
}
