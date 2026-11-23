/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patches.aliexpress.misc.settings.markPatchApplied
import app.morphe.patches.shared.misc.addAppliedPatchMetadata
import app.morphe.patches.shared.misc.fillAppliedPatch
import com.android.tools.smali.dexlib2.Opcode
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private const val MISSPELLED_KEY = "disableAnayltics"

internal class PatchMetadataTest {
    @Test
    fun `inject selected metadata with quotes and line breaks`() {
        // given
        lateinit var selectedPatch: Patch
        selectedPatch = patch(
            name = "Selected \"feature\"",
            description = "Preserves backslashes \\.\nIncludes the full caveat.",
        ) {
            execute { markPatchApplied("disableAnalytics", selectedPatch) }
        }

        // when
        val metadata = metadataInstructions(selectedPatch)

        // then
        assertEquals(
            listOf("disableAnalytics", selectedPatch.name, selectedPatch.description),
            metadata.strings(),
        )
        assertEquals(Opcode.CONST_4, metadata[metadata.lastIndex - 1].opcode)
        assertEquals(Opcode.RETURN_OBJECT, metadata.last().opcode)
    }

    @Test
    fun `leave description unfilled when withDescription is false`() {
        // given
        lateinit var selectedPatch: Patch
        selectedPatch = patch(name = "Selected feature", description = "Unused description.") {
            execute {
                addAppliedPatchMetadata(
                    "Lapp/hxreborn/extension/aliexpress/AppliedPatches;",
                    "disableAnalytics",
                    selectedPatch,
                    withDescription = false,
                )
            }
        }

        // when
        val metadata = metadataInstructions(selectedPatch)

        // then
        assertEquals(listOf("disableAnalytics", selectedPatch.name), metadata.strings())
        assertEquals(2, metadata.count { it.opcode == Opcode.CONST_4 })
    }

    @Test
    fun `retain metadata from each selected patch`() {
        lateinit var first: Patch
        lateinit var second: Patch
        first = patch(name = "First", description = "First description") {
            execute { markPatchApplied("disableAnalytics", first) }
        }
        second = patch(name = "Second", description = "Second description") {
            dependsOn(first)
            execute { markPatchApplied("removeSponsoredItems", second) }
        }

        assertEquals(
            listOf(
                "removeSponsoredItems", second.name, second.description,
                "disableAnalytics", first.name, first.description,
            ),
            metadataInstructions(second).strings(),
        )
    }

    @Test
    fun `fail patching when the app never reads the key`() {
        // given
        lateinit var selectedPatch: Patch
        selectedPatch = patch(name = "Selected feature", description = "Selected description.") {
            execute { markPatchApplied(MISSPELLED_KEY, selectedPatch) }
        }

        // when
        val failure = patchFailure(selectedPatch)

        // then
        assertNotNull(failure)
        assertEquals(true, generateSequence(failure) { it.cause }.any { it.message?.contains(MISSPELLED_KEY) == true })
    }

    @Test
    fun `fill legacy applied patch without description`() {
        lateinit var selectedPatch: Patch
        selectedPatch = patch(name = "Selected feature", description = "Unused description.") {
            execute {
                fillAppliedPatch(
                    "Lapp/hxreborn/extension/aliexpress/AppliedPatches;",
                    "disableAnalytics",
                    selectedPatch,
                    withDescription = false,
                )
            }
        }

        val methods = appliedPatchesMethods(selectedPatch)
        assertEquals(1, (methods.getValue("disableAnalytics").first() as NarrowLiteralInstruction).narrowLiteral)
        assertEquals(selectedPatch.name, methods.getValue("disableAnalyticsName").strings().first())
        assertEquals(listOf("disableAnalytics"), methods.getValue("disableAnalyticsDescription").strings())
    }

    private fun List<Instruction>.strings() =
        mapNotNull { ((it as? ReferenceInstruction)?.reference as? StringReference)?.string }

    private fun metadataInstructions(patch: Patch) = appliedPatchesMethods(patch).getValue("metadata")

    private fun appliedPatchesMethods(patch: Patch): Map<String, List<Instruction>> =
        withFixturePatcher(patch) { patcher ->
            runBlocking { patcher().collect { assertNull(it.exception) } }
            patcher.get().dexFiles.flatMap { dex ->
                dex.stream.use { DexBackedDexFile.fromInputStream(Opcodes.getDefault(), it.buffered()).classes }
            }.single { it.type == "Lapp/hxreborn/extension/aliexpress/AppliedPatches;" }.methods
                .associate { it.name to it.implementation!!.instructions.toList() }
        }

    private fun patchFailure(patch: Patch): Throwable? = withFixturePatcher(patch) { patcher ->
        var failure: Throwable? = null
        runBlocking { patcher().collect { result -> result.exception?.let { failure = it } } }
        failure
    }

    private fun <T> withFixturePatcher(patch: Patch, block: (Patcher) -> T): T {
        val directory = Files.createTempDirectory("patch-metadata").toFile()
        try {
            val apk = directory.resolve("fixture.apk")
            ZipOutputStream(apk.outputStream()).use { zip ->
                for ((entry, resource) in listOf(
                    "AndroidManifest.xml" to "/metadata/AndroidManifest.axml",
                    "classes.dex" to "/extensions/hxreborn/aliexpress-settings.mpe",
                )) {
                    zip.putNextEntry(ZipEntry(entry))
                    checkNotNull(javaClass.getResourceAsStream(resource)).use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            Patcher(PatcherConfig(apk, directory.resolve("work"))).use { patcher ->
                patcher += setOf(patch)
                return block(patcher)
            }
        } finally {
            directory.deleteRecursively()
        }
    }
}
