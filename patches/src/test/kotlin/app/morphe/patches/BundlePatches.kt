/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches

import app.morphe.patcher.patch.Patch
import app.morphe.patches.devcheck.misc.pairip.restoreVirtualizedMethodsPatch
import java.io.File
import java.io.InputStream
import java.lang.reflect.Modifier
import java.security.MessageDigest
import java.util.function.Supplier

internal object BundlePatches {
    data class Declared(val source: String, val className: String, val field: String, val patch: Patch)

    val classesDirectory = File("build/classes/kotlin/main")

    private val payloadByDigest: Map<String, String> by lazy {
        ExtensionPayloads.resourceNames.associateBy { digest(ExtensionPayloads.bytes(it)) }
    }

    val declared: List<Declared> by lazy {
        classesDirectory.walkTopDown().filter { it.name.endsWith("Kt.class") }.flatMap { file ->
            val className = file.relativeTo(classesDirectory).invariantSeparatorsPath
                .removeSuffix(".class").replace('/', '.')
            val source = className.removePrefix("app.morphe.patches.").removeSuffix("Kt").replace('.', '/') + ".kt"
            Class.forName(className).declaredFields
                .filter { Modifier.isStatic(it.modifiers) && Patch::class.java.isAssignableFrom(it.type) }
                .mapNotNull { field ->
                    field.isAccessible = true
                    (field.get(null) as Patch?)?.let { Declared(source, className, field.name, it) }
                }
        }.toList()
    }

    private fun digest(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private val extensionStreamProviders = Patch::class.java.getDeclaredField("extensionStreamProviders")
        .apply { isAccessible = true }

    private val patchesAssemblingTheirDexAtPatchTime = setOf<Patch>(restoreVirtualizedMethodsPatch)

    fun payloadsLoadedDirectlyBy(patch: Patch): Set<String> {
        if (patch in patchesAssemblingTheirDexAtPatchTime) return emptySet()
        @Suppress("UNCHECKED_CAST")
        val providers = extensionStreamProviders.get(patch) as List<Supplier<out Iterable<Supplier<InputStream>>>>
        return providers.flatMap { provider -> provider.get() }
            .mapNotNull { stream -> payloadByDigest[digest(stream.get().use { it.readBytes() })] }
            .toSet()
    }

    fun payloadsLoadedBy(patch: Patch, visited: MutableSet<Patch> = mutableSetOf()): Set<String> {
        if (!visited.add(patch)) return emptySet()
        val own = payloadsLoadedDirectlyBy(patch)
        return own + patch.dependencies.flatMap { payloadsLoadedBy(it, visited) }
    }
}
