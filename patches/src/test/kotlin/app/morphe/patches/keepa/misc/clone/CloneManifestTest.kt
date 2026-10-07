/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.keepa.misc.clone

import app.morphe.patches.elements
import app.morphe.patches.parseManifest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CloneManifestTest {
    private val manifest = """
        <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.keepa.mobile">
            <permission android:name="com.keepa.mobile.permission.C2D_MESSAGE"/>
            <uses-permission android:name="com.keepa.mobile.permission.C2D_MESSAGE"/>
            <uses-permission android:name="android.permission.INTERNET"/>
            <application android:label="Keepa">
                <activity android:name="com.keepa.mobile.MainActivity" android:label="Keepa">
                    <intent-filter>
                        <action android:name="android.intent.action.MAIN"/>
                        <category android:name="android.intent.category.LAUNCHER"/>
                    </intent-filter>
                </activity>
                <activity android:name="com.keepa.mobile.SettingsActivity" android:label="Settings"/>
                <provider android:name="androidx.core.content.FileProvider" android:authorities="com.keepa.mobile.fileprovider"/>
                <provider android:name="com.other.Provider" android:authorities="com.other.authority"/>
            </application>
        </manifest>
    """.trimIndent()

    private fun clone(packageName: String) = parseManifest(manifest).also { cloneManifest(it, packageName) }

    @Test
    fun `renames the package`() {
        assertEquals("com.keepa.mobile.clone2", clone("com.keepa.mobile.clone2").documentElement.getAttribute("package"))
    }

    @Test
    fun `renames only authorities under the original package`() {
        val authorities = clone("com.keepa.mobile.clone2").elements("provider").map { it.getAttribute("android:authorities") }

        assertEquals(listOf("com.keepa.mobile.clone2.fileprovider", "com.other.authority"), authorities)
    }

    @Test
    fun `renames the custom permission and its use but not platform permissions`() {
        val document = clone("com.keepa.mobile.clone2")

        assertEquals(
            listOf("com.keepa.mobile.clone2.permission.C2D_MESSAGE"),
            document.elements("permission").map { it.getAttribute("android:name") },
        )
        assertEquals(
            listOf("com.keepa.mobile.clone2.permission.C2D_MESSAGE", "android.permission.INTERNET"),
            document.elements("uses-permission").map { it.getAttribute("android:name") },
        )
    }

    @Test
    fun `labels the application and the launcher activity only`() {
        val document = clone("com.keepa.mobile.clone2")

        assertEquals("Keepa clone2", document.elements("application").single().getAttribute("android:label"))
        assertEquals(
            listOf("Keepa clone2", "Settings"),
            document.elements("activity").map { it.getAttribute("android:label") },
        )
    }
}
