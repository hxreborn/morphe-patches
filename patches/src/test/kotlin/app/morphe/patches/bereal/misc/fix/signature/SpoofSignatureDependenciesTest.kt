/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.fix.signature

import app.morphe.patcher.patch.Patch
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val BEREAL_PACKAGE_PATH = "app/morphe/patches/bereal"
private const val EXPECTED_FEATURE_PATCHES = 21

internal class SpoofSignatureDependenciesTest {
    @Test
    fun `include signature spoof with every named BeReal patch`() {
        val features = namedBeRealPatches()

        assertEquals(EXPECTED_FEATURE_PATCHES, features.size)
        for (feature in features) {
            assertTrue(spoofSignaturePatch in feature.allDependencies(), feature.name)
        }
    }

    private fun namedBeRealPatches(): List<Patch<*>> {
        val packageName = BEREAL_PACKAGE_PATH.replace('/', '.')
        val roots = javaClass.classLoader.getResources(BEREAL_PACKAGE_PATH).toList().map { Path.of(it.toURI()) }

        return roots
            .flatMap { root ->
                Files.walk(root).use { paths ->
                    paths
                        .filter { it.fileName.toString().endsWith("Kt.class") }
                        .map { "$packageName.${root.relativize(it).toString().removeSuffix(".class").replace('/', '.')}" }
                        .toList()
                }
            }
            .map { Class.forName(it) }
            .flatMap { it.declaredFields.toList() }
            .filter { Modifier.isStatic(it.modifiers) && Patch::class.java.isAssignableFrom(it.type) }
            .map { field ->
                field.isAccessible = true
                field.get(null) as Patch<*>
            }
            .filter { it.name != null }
            .distinct()
    }

    private fun Patch<*>.allDependencies(): Set<Patch<*>> {
        val dependencies = mutableSetOf<Patch<*>>()

        fun collect(patch: Patch<*>) {
            if (dependencies.add(patch)) patch.dependencies.forEach(::collect)
        }

        collect(this)

        return dependencies
    }
}
