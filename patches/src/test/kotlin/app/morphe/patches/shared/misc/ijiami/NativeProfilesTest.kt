/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ijiami

import app.morphe.patcher.patch.PatchException
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class NativeProfilesTest {
    @Test
    fun `rejects an unrecognized native library`() {
        val message = assertFailsWith<PatchException> {
            NativeProfiles.forLibexec(ByteArray(4096) { 0x2A })
        }.message

        assertTrue(message!!.contains("Unsupported"), message)
    }

    @Test
    fun `selects a bundled profile for every stock native library`() {
        val configured = System.getenv("IJIAMI_STOCK_APK")
        assumeTrue(configured != null, "IJIAMI_STOCK_APK is not set")

        configured!!.split(File.pathSeparatorChar).filter { it.isNotBlank() }.forEach { path ->
            val apk = File(path)
            assertTrue(apk.isFile, "configured stock APK does not exist: $apk")

            val libexec = ZipFile(apk).use { zip ->
                val entry = zip.getEntry("assets/ijm_lib/arm64-v8a/libexec.so")
                    ?: throw AssertionError("${apk.name} has no arm64 libexec.so")
                zip.getInputStream(entry).readBytes()
            }

            val profile = NativeProfiles.forLibexec(libexec)
            assertNotNull(
                NativeProfilesTest::class.java.getResourceAsStream("/ijiami/${profile.resource}"),
                "${apk.name} selected ${profile.name} but ${profile.resource} is not bundled",
            )
        }
    }
}
