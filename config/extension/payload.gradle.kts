val moduleName = checkNotNull(parent).name

extension {
    name = "extensions/hxreborn/$moduleName-${project.name}.mpe"
}

android {
    namespace = "app.hxreborn.extension.${moduleName.replace("-", "")}.${project.name.replace("-", "")}payload"
}

val payloadOptions = file("payload-options.gradle.kts")
if (payloadOptions.isFile) apply(from = payloadOptions)
