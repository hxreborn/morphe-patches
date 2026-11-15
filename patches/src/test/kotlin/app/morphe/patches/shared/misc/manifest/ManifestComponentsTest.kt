/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.manifest

import app.morphe.patches.elements
import app.morphe.patches.parseManifest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ManifestComponentsTest {
    @Test
    fun `disables selected components without changing sign in or nested metadata`() {
        val manifest = parseManifest("""
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application>
                    <provider android:name="tracking" android:enabled="@bool/startup" />
                    <receiver android:name="tracking" />
                    <activity android:name="signin" android:enabled="true">
                        <meta-data android:name="tracking" android:value="true" />
                    </activity>
                </application>
            </manifest>
        """.trimIndent())

        manifest.disableApplicationComponents(setOf("tracking", "missing"))

        assertEquals("false", manifest.elements("provider").single().getAttribute("android:enabled"))
        assertEquals("false", manifest.elements("receiver").single().getAttribute("android:enabled"))
        assertEquals("true", manifest.elements("activity").single().getAttribute("android:enabled"))
        assertEquals("", manifest.elements("meta-data").single().getAttribute("android:enabled"))
    }

    @Test
    fun `removes selected metadata without removing other service registrations`() {
        val manifest = parseManifest("""
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application>
                    <service android:name="discovery">
                        <meta-data android:name="analytics" android:value="registrar" />
                        <meta-data android:name="messaging" android:value="registrar" />
                    </service>
                </application>
            </manifest>
        """.trimIndent())

        manifest.removeMetaData(setOf("analytics"))

        assertEquals("messaging", manifest.elements("meta-data").single().getAttribute("android:name"))
        assertEquals("discovery", manifest.elements("service").single().getAttribute("android:name"))
    }

    @Test
    fun `removes selected permission declarations without changing service requirements`() {
        val manifest = parseManifest("""
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <uses-permission android:name="notifications" />
                <uses-permission-sdk-23 android:name="notifications" />
                <uses-permission android:name="network" />
                <application><service android:name="game" android:permission="notifications" /></application>
            </manifest>
        """.trimIndent())

        manifest.removeUsesPermissions(setOf("notifications"))

        assertEquals("network", manifest.elements("uses-permission").single().getAttribute("android:name"))
        assertEquals(emptyList(), manifest.elements("uses-permission-sdk-23"))
        assertEquals("notifications", manifest.elements("service").single().getAttribute("android:permission"))
    }

}
