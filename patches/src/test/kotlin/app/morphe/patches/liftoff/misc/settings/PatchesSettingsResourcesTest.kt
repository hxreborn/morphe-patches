/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.liftoff.misc.settings

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class PatchesSettingsResourcesTest {
    @Test
    fun `loads vector resources for every menu icon`() {
        val iconName = Regex("""hx_liftoff_ic_\w+""")
        val referencedIcons = File("../extensions/extension/src/main/java/app/hxreborn/extension/liftoff")
            .walkTopDown()
            .filter { it.extension == "java" }
            .flatMap { iconName.findAll(it.readText()).map { match -> match.value } }
            .toSet()
        assertTrue(referencedIcons.isNotEmpty())

        for (name in referencedIcons) {
            val resource = assertNotNull(javaClass.getResourceAsStream("/liftoff/drawable/$name.xml"), name)
            val document = resource.use { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it) }
            assertEquals("vector", document.documentElement.tagName, name)
            assertEquals("24dp", document.documentElement.getAttribute("android:width"), name)
            assertEquals("24dp", document.documentElement.getAttribute("android:height"), name)
        }
    }
}
