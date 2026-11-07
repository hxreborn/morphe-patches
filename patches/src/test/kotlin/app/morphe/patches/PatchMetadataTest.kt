/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.aliexpress.misc.settings.markPatchApplied
import app.morphe.patches.shared.misc.fillAppliedPatch
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class PatchMetadataTest {
    @Test
    fun `inject selected metadata with quotes and line breaks`() {
        // given
        lateinit var patch: BytecodePatch
        patch = bytecodePatch(
            name = "Selected \"feature\"",
            description = "Preserves backslashes \\.\nIncludes the full caveat.",
        ) {
            execute { markPatchApplied("disableAnalytics", patch) }
        }

        // when
        val methods = appliedPatchesMethods(patch)

        // then
        assertEquals(1, (methods.getValue("disableAnalytics") as NarrowLiteralInstruction).narrowLiteral)
        assertEquals(patch.name,
            ((methods.getValue("disableAnalyticsName") as ReferenceInstruction).reference as StringReference).string)
        assertEquals(patch.description,
            ((methods.getValue("disableAnalyticsDescription") as ReferenceInstruction).reference as StringReference).string)
        for (name in listOf("removeSponsoredItems", "removeSponsoredItemsName", "removeSponsoredItemsDescription")) {
            assertEquals(0, (methods.getValue(name) as NarrowLiteralInstruction).narrowLiteral)
        }
    }

    @Test
    fun `leave description unfilled when withDescription is false`() {
        // given
        lateinit var patch: BytecodePatch
        patch = bytecodePatch(name = "Selected feature", description = "Unused description.") {
            execute {
                fillAppliedPatch(
                    "Lapp/hxreborn/extension/aliexpress/AppliedPatches;",
                    "disableAnalytics",
                    patch,
                    withDescription = false,
                )
            }
        }

        // when
        val methods = appliedPatchesMethods(patch)

        // then
        assertEquals(1, (methods.getValue("disableAnalytics") as NarrowLiteralInstruction).narrowLiteral)
        assertEquals(patch.name,
            ((methods.getValue("disableAnalyticsName") as ReferenceInstruction).reference as StringReference).string)
        assertEquals(0, (methods.getValue("disableAnalyticsDescription") as NarrowLiteralInstruction).narrowLiteral)
    }

    private fun appliedPatchesMethods(patch: BytecodePatch): Map<String, Instruction> {
        val directory = Files.createTempDirectory("patch-metadata").toFile()
        try {
            val apk = directory.resolve("fixture.apk")
            ZipOutputStream(apk.outputStream()).use { zip ->
                for ((entry, resource) in listOf(
                    "AndroidManifest.xml" to "/metadata/AndroidManifest.axml",
                    "classes.dex" to "/extensions/extension.mpe",
                )) {
                    zip.putNextEntry(ZipEntry(entry))
                    checkNotNull(javaClass.getResourceAsStream(resource)).use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            Patcher(PatcherConfig(apk, directory.resolve("work"))).use { patcher ->
                patcher += setOf(patch)
                runBlocking { patcher().collect { assertNull(it.exception) } }
                return patcher.get().dexFiles.flatMap { dex ->
                    dex.stream.use { DexBackedDexFile.fromInputStream(Opcodes.getDefault(), it.buffered()).classes }
                }.single { it.type == "Lapp/hxreborn/extension/aliexpress/AppliedPatches;" }.methods
                    .associate { it.name to it.implementation!!.instructions.first() }
            }
        } finally {
            directory.deleteRecursively()
        }
    }
}
