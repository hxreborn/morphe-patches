val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
val okhttpVersion = catalog.findVersion("okhttp").get().requiredVersion

extra["extensionCheckstyleSources"] = files("src/main/java", "src/test/java")

dependencies {
    add("testImplementation", testFixtures(project(":extensions:shared")))
    add("testImplementation", catalog.findLibrary("assertj-core").get())
    add("testImplementation", "com.squareup.okhttp3:okhttp-jvm:$okhttpVersion")
}

val android = extensions.getByType<com.android.build.api.dsl.LibraryExtension>()
val sdkComponents = extensions.getByType<com.android.build.api.variant.LibraryAndroidComponentsExtension>().sdkComponents

tasks.register<Exec>("checkMovieBoxApiLevel") {
    val javac = tasks.named<JavaCompile>("compileReleaseJavaWithJavac")
    val classes = javac.flatMap { it.destinationDirectory }
    val sources = layout.projectDirectory.dir("src/main/java")
    val apiVersions = sdkComponents.sdkDirectory.map { it.file("platforms/android-${android.compileSdk}/data/api-versions.xml") }
    val checker = rootProject.file(".github/scripts/check_android_api_level.py")
    dependsOn(javac)
    inputs.file(checker)
    inputs.file(apiVersions)
    inputs.dir(classes)
    inputs.dir(sources)
    commandLine(
        "python3",
        checker.path,
        android.defaultConfig.minSdk.toString(),
        apiVersions.get().asFile.path,
        classes.get().asFile.path,
        sources.asFile.path,
    )
}
