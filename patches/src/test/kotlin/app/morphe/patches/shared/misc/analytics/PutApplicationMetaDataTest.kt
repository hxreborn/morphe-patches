/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.analytics

import app.morphe.patches.elements
import app.morphe.patches.parseManifest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class PutApplicationMetaDataTest {
    private fun manifest(applicationBody: String) = parseManifest(
        """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.example">
                <application>$applicationBody</application>
            </manifest>
        """.trimIndent(),
    )

    @Test
    fun `adds the entry when the application does not declare it`() {
        val document = manifest("")

        document.putApplicationMetaData("flag", "false")

        val entry = document.elements("meta-data").single()
        assertEquals("flag", entry.getAttribute("android:name"))
        assertEquals("false", entry.getAttribute("android:value"))
    }

    @Test
    fun `rewrites a declared entry and drops its resource reference`() {
        val document = manifest("""<meta-data android:name="flag" android:value="true" android:resource="@bool/flag"/>""")

        document.putApplicationMetaData("flag", "false")

        val entry = document.elements("meta-data").single()
        assertEquals("false", entry.getAttribute("android:value"))
        assertFalse(entry.hasAttribute("android:resource"))
    }

    @Test
    fun `does not duplicate the entry when applied twice`() {
        val document = manifest("")

        document.putApplicationMetaData("flag", "false")
        document.putApplicationMetaData("flag", "false")

        assertEquals(1, document.elements("meta-data").size)
    }

    @Test
    fun `ignores a same-named entry declared inside a component`() {
        val document = manifest(
            """<activity android:name=".Main"><meta-data android:name="flag" android:value="true"/></activity>""",
        )

        document.putApplicationMetaData("flag", "false")

        val values = document.elements("meta-data").map { it.getAttribute("android:value") }
        assertEquals(listOf("true", "false"), values)
    }
}
