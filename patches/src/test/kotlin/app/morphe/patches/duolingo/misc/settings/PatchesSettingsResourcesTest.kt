/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.duolingo.misc.settings

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PatchesSettingsResourcesTest {

    private val iconName = Regex("""hx_duolingo_ic_\w+""")

    private val extensionIcons by lazy {
        File("../extensions/extension/src/main/java/app/hxreborn/extension/duolingo")
            .walkTopDown().filter { it.extension == "java" }
            .flatMap { file -> iconName.findAll(file.readText()).map { it.value } }
            .toSortedSet()
    }

    private fun resourceText(path: String) =
        checkNotNull(javaClass.getResourceAsStream(path)) { "Missing patch resource $path" }
            .bufferedReader().use { it.readText() }

    @Test
    fun `copies every icon the menu shows`() {
        assertEquals(extensionIcons, ICON_RESOURCES.toSortedSet())
    }

    @Test
    fun `copies only vector drawables`() {
        for (name in ICON_RESOURCES) {
            val document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(resourceText("/duolingo/drawable/$name.xml").byteInputStream())
            assertEquals("vector", document.documentElement.tagName, name)
        }
    }

}
