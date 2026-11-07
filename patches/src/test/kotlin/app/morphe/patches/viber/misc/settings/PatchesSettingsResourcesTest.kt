/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.settings

import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class PatchesSettingsResourcesTest {
    @Test
    fun `copy a vector for every settings switch`() {
        val settings = File("../extensions/extension/src/main/java/app/hxreborn/extension/viber/PatchSetting.java")
            .readText().substringAfter("enum PatchSetting {").substringBefore(';')
        val icons = Regex("""\b([A-Z][A-Z_]+)\(""")
            .findAll(settings).map { it.groupValues[1].lowercase(Locale.ROOT) }.toList()
        assertTrue(icons.isNotEmpty())
        val patch = File("src/main/kotlin/app/morphe/patches/viber/misc/settings/PatchesSettingsPatch.kt").readText()
        val copies = Regex("\"([a-z_]+)\" to \"([a-z_]+)\"").findAll(patch)
            .associate { it.groupValues[1] to it.groupValues[2] }
        for (icon in icons) {
            val source = assertNotNull(copies[icon], "No resource copied for hx_viber_ic_$icon")
            val resource = assertNotNull(
                javaClass.getResourceAsStream("/viber/drawable/hx_viber_ic_$source.xml")
                    ?: javaClass.getResourceAsStream("/patreon/drawable/hx_patreon_ic_$source.xml"),
                source,
            )
            val vector = resource.use { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it) }
            assertEquals("vector", vector.documentElement.tagName, icon)
            assertEquals("24dp", vector.documentElement.getAttribute("android:width"), icon)
            assertEquals("24dp", vector.documentElement.getAttribute("android:height"), icon)
        }
    }
}
