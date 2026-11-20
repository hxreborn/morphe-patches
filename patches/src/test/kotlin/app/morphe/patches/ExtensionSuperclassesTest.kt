/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches

import kotlin.test.Test
import kotlin.test.assertEquals

internal class ExtensionSuperclassesTest {
    private val d8RuntimePackage = "Lcom/android/tools/r8/"

    private val unloadedLibraryRecords = setOf(
        "Lapp/morphe/extension/shared/TrieSearch\$TrieCompressedPath;",
    )

    @Test
    fun `no extension class extends a D8 runtime class`() {
        val subclasses = ExtensionPayloads.resourceNames.flatMap(ExtensionPayloads::classes)
            .filter { it.superclass?.startsWith(d8RuntimePackage) == true && it.type !in unloadedLibraryRecords }
            .map { "${it.type} extends ${it.superclass}" }

        assertEquals(emptyList(), subclasses)
    }
}
