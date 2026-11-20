import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.AndroidComponentsExtension

abstract class GenerateWebAssets : DefaultTask() {

    @get:InputDirectory
    abstract val scripts: DirectoryProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val target = outputDirectory.get().asFile.resolve(packageName.get().replace('.', '/') + "/WebAssets.java")
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
            |package ${packageName.get()};
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

val catalog = libs

val extensionProjects = subprojects.filter { it.path.startsWith(":extensions:") || it.path == ":extension-tests" }

extensionProjects.filter { it.layout.projectDirectory.file("library.gradle.kts").asFile.isFile }.forEach { library ->
    library.apply(plugin = "com.android.library")
    library.extensions.configure<LibraryExtension> {
        namespace = "app.hxreborn.extension." + library.projectDir.relativeTo(rootDir.resolve("extensions"))
            .invariantSeparatorsPath.removeSuffix("/library").replace("/", "").replace("-", "")
        compileSdk = 36
        defaultConfig.minSdk = 23
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
    library.apply(from = library.layout.projectDirectory.file("library.gradle.kts"))
}

configure(extensionProjects) {
    val extensionProject = this

    tasks.withType<Test>().configureEach {
        systemProperty("user.timezone", "UTC")
        maxHeapSize = "2g"
        jvmArgs(
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

    configurations.matching {
        it.name == "debugRuntimeClasspath" || it.name == "releaseRuntimeClasspath"
    }.configureEach {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains", module = "annotations")
    }

    for (androidPlugin in listOf("com.android.application", "com.android.library")) {
        pluginManager.withPlugin(androidPlugin) {
            dependencies {
                add("testImplementation", catalog.junit)
                add("testImplementation", catalog.robolectric)
                add("testImplementation", catalog.json)
            }

            if (androidPlugin == "com.android.library") {
                val libraryDirectory = layout.projectDirectory.asFile
                val groupsDirectory = if (libraryDirectory.listFiles().orEmpty().any { it.resolve("classes.list").isFile }) {
                    libraryDirectory
                } else {
                    libraryDirectory.parentFile
                }
                val lists = groupsDirectory.listFiles().orEmpty().map { it.resolve("classes.list") }.filter { it.isFile }
                if (lists.isNotEmpty()) {
                    val unusedList = libraryDirectory.resolve("unused.list")
                    val compileTask = extra.properties["payloadClassesTask"] as String? ?: "compileReleaseJavaWithJavac"
                    val compiledClasses = (extra.properties["payloadClassesDir"] as Provider<Directory>?)
                        ?: provider { tasks.named(compileTask, JavaCompile::class.java).get().destinationDirectory.get() }
                    tasks.register("verifyPayloadClasses") {
                        description = "Fails when a payload class list names a missing class or a class sits in no list or in two"
                        dependsOn(compileTask)
                        inputs.files(lists)
                        inputs.files(unusedList)
                        doLast {
                            val directory = compiledClasses.get().asFile
                            val compiled = directory.walkTopDown().filter { it.extension == "class" }
                                .map { it.relativeTo(directory).invariantSeparatorsPath.removeSuffix(".class") }.toSet()
                            val owners = mutableMapOf<String, MutableList<String>>()
                            lists.forEach { list ->
                                list.readLines().filter { it.isNotBlank() }.forEach { owners.getOrPut(it) { mutableListOf() }.add(list.parentFile.name) }
                            }
                            val missing = owners.keys.filter { it !in compiled }
                            val twice = owners.filterValues { it.size > 1 }
                            val unused = if (unusedList.isFile) unusedList.readLines().filter { it.isNotBlank() }.toSet() else null
                            val unlisted = if (unused == null) emptyList() else compiled.filter { it !in owners && it !in unused }
                            val unusedAndListed = unused.orEmpty().filter { it in owners }
                            check(missing.isEmpty() && twice.isEmpty() && unlisted.isEmpty() && unusedAndListed.isEmpty()) {
                                "Payload class lists of ${project.path} disagree with the compiled classes: " +
                                    "listed but absent $missing, listed twice $twice, in no list $unlisted, listed and unused $unusedAndListed"
                            }
                        }
                    }
                }
            }

            val classList = layout.projectDirectory.file("classes.list").asFile
            if (androidPlugin == "com.android.application" && classList.isFile) {
                val parentProject = checkNotNull(parent)
                val library = if (parentProject.file("library.gradle.kts").isFile) {
                    parentProject
                } else {
                    rootProject.project("${parentProject.path}:library")
                }
                val patterns = classList.readLines().filter { it.isNotBlank() }.map { "$it.class" }
                val classesTask = library.extra.properties["payloadClassesTask"] as String? ?: "compileReleaseJavaWithJavac"
                val classesDirectory = library.extra.properties["payloadClassesDir"] as Provider<Directory>?
                    ?: provider { library.tasks.named(classesTask, JavaCompile::class.java).get().destinationDirectory.get() }
                val payloadClasses = tasks.register<Jar>("payloadClasses") {
                    archiveFileName.set("payload-classes.jar")
                    destinationDirectory.set(layout.buildDirectory.dir("payload"))
                    dependsOn("${library.path}:$classesTask")
                    from(classesDirectory) {
                        include(patterns)
                    }
                }
                dependencies {
                    add("implementation", files(payloadClasses.flatMap { it.archiveFile }))
                    add("compileOnly", library)
                }
            }

            val sharedManifest = rootProject.file("config/extension/AndroidManifest.xml")
            val ownsManifest = layout.projectDirectory.file("src/main/AndroidManifest.xml").asFile.exists()

            if (androidPlugin == "com.android.application") {
                extensions.configure<ApplicationExtension> {
                    testOptions.unitTests.isIncludeAndroidResources = true
                    if (!ownsManifest) sourceSets.getByName("main").manifest.srcFile(sharedManifest)
                }
            } else {
                extensions.configure<LibraryExtension> {
                    testOptions.unitTests.isIncludeAndroidResources = true
                    if (!ownsManifest) sourceSets.getByName("main").manifest.srcFile(sharedManifest)
                }
            }

            if (layout.projectDirectory.dir("src/main/js").asFile.isDirectory) {
                (extensions.getByName("androidComponents") as AndroidComponentsExtension<*, *, *>).onVariants { variant ->
                    val generate = tasks.register<GenerateWebAssets>("generate${variant.name.replaceFirstChar { it.uppercase() }}WebAssets") {
                        scripts.set(layout.projectDirectory.dir("src/main/js"))
                        packageName.set(
                            providers.provider {
                                extensionProject.extra.properties["webAssetsPackage"] as String?
                                    ?: "app.hxreborn.extension.${extensionProject.name.replace("-", "")}"
                            },
                        )
                        outputDirectory.set(layout.buildDirectory.dir("generated/webAssets/${variant.name}"))
                    }
                    variant.sources.java?.addGeneratedSourceDirectory(generate, GenerateWebAssets::outputDirectory)
                }
            }
        }
    }

    if (layout.projectDirectory.dir("src/main/java").asFile.isDirectory) {
        apply(plugin = "checkstyle")
        dependencies {
            add("checkstyle", catalog.spring.javaformat.checkstyle)
        }
        val checkstyle = tasks.register<Checkstyle>("checkstyle") {
            source("src/main/java")
            classpath = files()
        }
        tasks.matching { it.name == "check" }.configureEach {
            dependsOn(checkstyle)
        }
    }
}

tasks.register("testExtensions") {
    description = "Runs the unit tests of every extension module"
    dependsOn(
        extensionProjects.map { module ->
            module.tasks.matching { it.name.startsWith("test") && it.name.endsWith("UnitTest") }
        },
    )
}

tasks.register("checkExtensions") {
    description = "Runs Checkstyle, lint and the MovieBox API level check of every extension module"
    dependsOn(
        extensionProjects.map { module ->
            module.tasks.matching {
                it.name == "checkstyle" ||
                    it.name == "verifyPayloadClasses" ||
                    it.name == "checkMovieBoxApiLevel" ||
                    (it.name == "lintRelease" && module.file("src/main/java").isDirectory)
            }
        },
    )
}
