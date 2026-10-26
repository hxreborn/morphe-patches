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

private const val ARM64_ABI = "arm64-v8a"

private val PACKER_LIBRARY = Regex("assets/[^/]+/(arm64-v8a|armeabi)/[^/]+\\.so")

internal class NativeProfilesTest {
    @Test
    fun `selects no profile for an unrecognized native library`() {
        assertNull(NativeProfiles.forLibexecOrNull(ByteArray(4096) { 0x2A }))
    }

    @Test
    fun `selects a bundled arm64 profile for every stock APK`() {
        val configured = System.getenv("IJIAMI_STOCK_APK")
        assumeTrue(configured != null, "IJIAMI_STOCK_APK is not set")

        configured!!.split(File.pathSeparatorChar).filter { it.isNotBlank() }.forEach { path ->
            val apk = File(path)
            assertTrue(apk.isFile, "configured stock APK does not exist: $apk")

            val librariesByAbi = ZipFile(apk).use { zip ->
                zip.entries().asSequence()
                    .mapNotNull { entry -> PACKER_LIBRARY.matchEntire(entry.name)?.let { it.groupValues[1] to zip.getInputStream(entry).readBytes() } }
                    .groupBy({ it.first }, { it.second })
            }
            assertTrue(librariesByAbi.isNotEmpty(), "${apk.name} has no Ijiami packer library")

            val profiles = librariesByAbi.mapValues { (_, libraries) ->
                libraries.firstNotNullOfOrNull { NativeProfiles.forLibexecOrNull(it) }
            }
            assertNotNull(profiles[ARM64_ABI], "${apk.name} has no profile for its $ARM64_ABI packer")
            profiles.values.filterNotNull().forEach { assertBundled(apk, it) }
        }
    }

    private fun assertBundled(apk: File, profile: NativeProfile) {
        assertNotNull(
            NativeProfilesTest::class.java.getResourceAsStream("/ijiami/${profile.resource}"),
            "${apk.name} selected ${profile.name} but ${profile.resource} is not bundled",
        )
    }
}
