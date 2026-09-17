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
