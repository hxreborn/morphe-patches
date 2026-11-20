plugins {
    id("com.android.application")
    checkstyle
}

android {
    namespace = "app.hxreborn.extension.tests"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets.getByName("test").java.srcDirs(
        "../extensions/moviebox/src",
        "../extensions/moviebox/test",
        "../extensions/ijiami/src",
        "../extensions/ijiami/test",
    )
}

val movieBoxMinSdk = 23

tasks.register<Checkstyle>("checkstyle") {
    source("../extensions/moviebox/src", "../extensions/moviebox/test", "../extensions/ijiami/test", "../extension-test-fixtures/src/main/java")
    classpath = files()
}

tasks.register<Exec>("checkMovieBoxApiLevel") {
    dependsOn("compileDebugUnitTestJavaWithJavac")
    val apiVersions = androidComponents.sdkComponents.sdkDirectory.map {
        it.file("platforms/android-${android.compileSdk}/data/api-versions.xml").asFile.path
    }
    val classes = layout.buildDirectory.dir("intermediates/javac/debugUnitTest/compileDebugUnitTestJavaWithJavac/classes")
    val sources = layout.projectDirectory.dir("../extensions/moviebox/src")
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

tasks.named("check") {
    dependsOn("checkstyle")
}

dependencies {
    checkstyle(libs.spring.javaformat.checkstyle)
    testImplementation(project(":extension-test-fixtures"))
    testImplementation(libs.assertj.core)
    testImplementation(libs.okhttp)
}
