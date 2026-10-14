/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ijiami

import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val PACKER_LIBRARIES = listOf("assets/ijm_lib/arm64-v8a/libexec.so", "assets/ijm_lib/armeabi/libexec.so")

internal class NativeProfilesTest {
    @Test
    fun `selects no profile for an unrecognized native library`() {
        assertNull(NativeProfiles.forLibexecOrNull(ByteArray(4096) { 0x2A }))
    }

    @Test
    fun `selects a bundled profile for every stock native library`() {
        val configured = System.getenv("IJIAMI_STOCK_APK")
        assumeTrue(configured != null, "IJIAMI_STOCK_APK is not set")

        configured!!.split(File.pathSeparatorChar).filter { it.isNotBlank() }.forEach { path ->
            val apk = File(path)
            assertTrue(apk.isFile, "configured stock APK does not exist: $apk")

            val packers = ZipFile(apk).use { zip ->
                PACKER_LIBRARIES.mapNotNull { library -> zip.getEntry(library)?.let { library to zip.getInputStream(it).readBytes() } }
            }
            assertTrue(packers.isNotEmpty(), "${apk.name} has no Ijiami packer library")

            packers.forEach { (library, libexec) ->
                val profile = assertNotNull(NativeProfiles.forLibexecOrNull(libexec), "${apk.name} has no profile for $library")
                assertBundled(apk, profile)
            }
        }
    }

    private fun assertBundled(apk: File, profile: NativeProfile) {
        assertNotNull(
            NativeProfilesTest::class.java.getResourceAsStream("/ijiami/${profile.resource}"),
            "${apk.name} selected ${profile.name} but ${profile.resource} is not bundled",
        )
    }
}
