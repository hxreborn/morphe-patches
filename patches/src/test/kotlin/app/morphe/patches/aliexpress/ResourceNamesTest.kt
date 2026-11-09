/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.aliexpress

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ResourceNamesTest {
    @Test
    fun `looks up no resource renamed on a case-insensitive file system`() {
        // given
        val renamedNames = javaClass.getResource("/aliexpress/renamed-resources-8.180.1.txt")!!.readText()
            .lines()
            .filter(String::isNotBlank)
            .map { it.substringAfter('/') }
            .toSet()
        val quotedIdentifier = Regex(""""([A-Za-z_][A-Za-z0-9_]*)"""")

        // when
        val quotedNames = File("src/main/kotlin/app/morphe/patches/aliexpress")
            .walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap { source -> quotedIdentifier.findAll(source.readText()).map { it.groupValues[1] } }
            .toSet()

        // then
        assertTrue(quotedNames.isNotEmpty())
        assertEquals(emptySet(), quotedNames intersect renamedNames)
    }
}
