/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.androidauto

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.elements
import app.morphe.patches.parseManifest
import org.w3c.dom.Document
import org.w3c.dom.Element
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class ParkedGameManifestTest {
    private val carLauncher = "android.intent.category.CAR_LAUNCHER"

    @Test
    fun `adds the car launcher category to the main launcher filter`() {
        // given
        val manifest = parkedManifest(application = "", launcherCategories = listOf("android.intent.category.LAUNCHER"))

        // when
        declareParkedGame(manifest)

        // then
        assertEquals(listOf("android.intent.category.LAUNCHER", carLauncher), categoryNames(manifest, "main"))
    }

    @Test
    fun `leaves the car launcher category single when already declared`() {
        // given
        val manifest = parkedManifest(
            application = "",
            launcherCategories = listOf("android.intent.category.LAUNCHER", carLauncher),
        )

        // when
        declareParkedGame(manifest)

        // then
        assertEquals(listOf("android.intent.category.LAUNCHER", carLauncher), categoryNames(manifest, "main"))
    }

    @Test
    fun `marks the application as a game`() {
        // given
        val manifest = parkedManifest(application = "", launcherCategories = listOf("android.intent.category.LAUNCHER"))

        // when
        declareParkedGame(manifest)

        // then
        assertEquals("game", manifest.elements("application").single().getAttribute("android:appCategory"))
    }

    @Test
    fun `replaces another application category with game`() {
        // given
        val manifest = parkedManifest(
            application = "android:appCategory=\"social\"",
            launcherCategories = listOf("android.intent.category.LAUNCHER"),
        )

        // when
        declareParkedGame(manifest)

        // then
        assertEquals("game", manifest.elements("application").single().getAttribute("android:appCategory"))
    }

    @Test
    fun `leaves filters without the launcher category unchanged`() {
        // given
        val manifest = parkedManifest(application = "", launcherCategories = listOf("android.intent.category.LAUNCHER"))

        // when
        declareParkedGame(manifest)

        // then
        assertEquals(listOf("android.intent.category.DEFAULT"), categoryNames(manifest, "view"))
    }

    @Test
    fun `rejects a manifest without a main launcher filter`() {
        // given
        val manifest = parkedManifest(application = "", launcherCategories = listOf("android.intent.category.DEFAULT"))

        // when
        val failure = assertFailsWith<PatchException> { declareParkedGame(manifest) }

        // then
        assertEquals("No main launcher intent filter found", failure.message)
    }

    private fun categoryNames(manifest: Document, filterId: String) = manifest.elements("intent-filter")
        .single { it.getAttribute("tools:id") == filterId }
        .getElementsByTagName("category").let { categories ->
            (0 until categories.length).map { (categories.item(it) as Element).getAttribute("android:name") }
        }

    private fun parkedManifest(application: String, launcherCategories: List<String>) = parseManifest(
        """
        <manifest xmlns:android="http://schemas.android.com/apk/res/android" xmlns:tools="http://schemas.android.com/tools">
            <application $application>
                <activity android:name=".Game">
                    <intent-filter tools:id="main">
                        <action android:name="android.intent.action.MAIN"/>
                        ${launcherCategories.joinToString("\n") { "<category android:name=\"$it\"/>" }}
                    </intent-filter>
                    <intent-filter tools:id="view">
                        <action android:name="android.intent.action.VIEW"/>
                        <category android:name="android.intent.category.DEFAULT"/>
                    </intent-filter>
                </activity>
            </application>
        </manifest>
        """.trimIndent(),
    )
}
