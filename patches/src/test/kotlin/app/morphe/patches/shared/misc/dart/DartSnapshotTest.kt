/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.dart

import app.morphe.patcher.patch.PatchException
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DartSnapshotTest {
    @Test
    fun removeWidgetWithoutMovingSerializedObjects() {
        val library = library()
        val snapshot = DartSnapshot(library)
        assertEquals("widget", snapshot.stringsByPoolOffset[16])
        assertEquals(setOf(24), snapshot.widgetArrayPoolOffsets())
        val replacements = snapshot.removeFirstArrayElement(24)
        assertTrue(byteArrayOf(0, 0, 0x81.toByte(), 1, 0xa3.toByte(), 1, 0xa4.toByte()).contentEquals(replacements.last().second))
        replacements.forEach { (offset, replacement) -> replacement.copyInto(library, offset) }
        val patched = DartSnapshot(library)
        assertEquals("widget", patched.stringsByPoolOffset[16])
        assertTrue(patched.widgetArrayPoolOffsets().isEmpty())
        assertEquals(LIBRARY_BYTES, library.size)
    }

    @Test
    fun rejectPoolBaseAfterRegisterOverwrite() {
        val library = library()
        val code = ByteBuffer.wrap(library).order(ByteOrder.LITTLE_ENDIAN)
        code.putInt(2048, 0xa9bf79fd.toInt())
        code.putInt(2052, 0xaa0f03fd.toInt())
        code.putInt(2056, 0x9100036a.toInt())
        code.putInt(2060, 0xd280000a.toInt())
        code.putInt(2064, 0xf9400940.toInt())
        val methods = app.morphe.patches.ultimea.misc.settings.NativeMethods(library)
        assertFailsWith<PatchException> { methods.poolOffset(2064) }
    }

    @Test
    fun rejectPoolBaseAfterAddressWriteback() {
        for (writeback in listOf(0xf8408540.toInt(), 0xf8008540.toInt(), 0xf8408d40.toInt(), 0xf8008d40.toInt())) {
            val library = library()
            val code = ByteBuffer.wrap(library).order(ByteOrder.LITTLE_ENDIAN)
            code.putInt(2048, 0xa9bf79fd.toInt())
            code.putInt(2052, 0xaa0f03fd.toInt())
            code.putInt(2056, 0x9140076a.toInt())
            code.putInt(2060, 0xd503201f.toInt())
            code.putInt(2064, 0xf9400141.toInt())
            assertEquals(4096, app.morphe.patches.ultimea.misc.settings.NativeMethods(library).poolOffset(2064))
            code.putInt(2060, writeback)
            val methods = app.morphe.patches.ultimea.misc.settings.NativeMethods(library)
            assertFailsWith<PatchException> { methods.poolOffset(2064) }
        }
    }

    @Test
    fun rejectPoolBaseAfterIntegerLiteralLoad() {
        for (literal in listOf(0x1800000a, 0x5800000a, 0x9800000a.toInt())) {
            val library = library()
            val code = ByteBuffer.wrap(library).order(ByteOrder.LITTLE_ENDIAN)
            code.putInt(2048, 0xa9bf79fd.toInt())
            code.putInt(2052, 0xaa0f03fd.toInt())
            code.putInt(2056, 0x9140076a.toInt())
            code.putInt(2060, literal)
            code.putInt(2064, 0xf9400141.toInt())
            val methods = app.morphe.patches.ultimea.misc.settings.NativeMethods(library)
            assertFailsWith<PatchException> { methods.poolOffset(2064) }
        }
    }

    @Test
    fun rejectPoolBaseThatIsInvalidOnOneBranch() {
        val library = library()
        val code = ByteBuffer.wrap(library).order(ByteOrder.LITTLE_ENDIAN)
        code.putInt(2048, 0xa9bf79fd.toInt())
        code.putInt(2052, 0xaa0f03fd.toInt())
        code.putInt(2056, 0x9140076a.toInt())
        code.putInt(2060, 0xb4000040.toInt())
        code.putInt(2064, 0xd280000a.toInt())
        code.putInt(2068, 0xf9400d41.toInt())
        val methods = app.morphe.patches.ultimea.misc.settings.NativeMethods(library)
        assertFailsWith<PatchException> { methods.poolOffset(2068) }
    }

    @Test
    fun retainPoolBaseAcrossTheFullWindow() {
        val library = poolLoadLibrary(0x9140076a.toInt(), 0xd503201f.toInt(), 0xd503201f.toInt())
        assertEquals(4096, app.morphe.patches.ultimea.misc.settings.NativeMethods(library).poolOffset(2068))
    }

    @Test
    fun retainPoolBaseWhenBranchSkipsOverwrite() {
        val library = poolLoadLibrary(0x9140076a.toInt(), 0x14000002, 0xd280000a.toInt())
        assertEquals(4096, app.morphe.patches.ultimea.misc.settings.NativeMethods(library).poolOffset(2068))
    }

    @Test
    fun rejectPoolBaseWhenBranchSkipsAssignment() {
        for (branch in listOf(0x14000002, 0xb4000040.toInt(), 0x54000041, 0x36000047)) {
            val library = poolLoadLibrary(0xd503201f.toInt(), branch, 0x9140076a.toInt())
            assertFailsWith<PatchException> { app.morphe.patches.ultimea.misc.settings.NativeMethods(library).poolOffset(2068) }
        }
    }

    @Test
    fun retainPoolBaseWhenBothBranchesPreserveIt() {
        for (branch in listOf(0xb4000040.toInt(), 0x54000041, 0x36000047)) {
            val library = poolLoadLibrary(0x9140076a.toInt(), branch, 0xd503201f.toInt())
            assertEquals(4096, app.morphe.patches.ultimea.misc.settings.NativeMethods(library).poolOffset(2068))
        }
    }

    @Test
    fun ignoreArrayReferenceBytesInsideStringsAndUnboxedFields() {
        for (library in listOf(library(literal = "widget\u0001\u0082"), library(unboxed = true), library(poolImmediate = true))) {
            val original = library.copyOf()
            val snapshot = DartSnapshot(library)
            val edits = snapshot.removeFirstArrayElement(24)
            edits.forEach { (offset, replacement) -> replacement.copyInto(library, offset) }
            val referenceBytes = byteArrayOf(1, 0x82.toByte())
            val payloadOffsets = (0 until original.lastIndex).filter { original[it] == referenceBytes[0] && original[it + 1] == referenceBytes[1] }
            for (offset in payloadOffsets) {
                kotlin.test.assertContentEquals(referenceBytes, library.copyOfRange(offset, offset + referenceBytes.size))
            }
            val editedOffsets = edits.flatMap { (offset, replacement) -> (offset until offset + replacement.size).toList() }.toSet()
            assertTrue(library.indices.filterNot { it in editedOffsets }.all { library[it] == original[it] })
            assertTrue(DartSnapshot(library).widgetArrayPoolOffsets().isEmpty())
            assertEquals(snapshot.stringsByPoolOffset[16], DartSnapshot(library).stringsByPoolOffset[16])
        }
    }

    private fun poolLoadLibrary(first: Int, branch: Int, skipped: Int): ByteArray {
        val library = library()
        val code = ByteBuffer.wrap(library).order(ByteOrder.LITTLE_ENDIAN)
        listOf(0xa9bf79fd.toInt(), 0xaa0f03fd.toInt(), first, branch, skipped, 0xf9400141.toInt())
            .forEachIndexed { index, instruction -> code.putInt(2048 + index * Int.SIZE_BYTES, instruction) }
        return library
    }

    @Test
    fun rejectPoolBaseAfterFloatingPointOrSystemWrite() {
        val overwrites = listOf(0x9e66000a.toInt(), 0x1e26000a, 0x9eae000a.toInt(), 0x9e78000a.toInt(),
            0x1e21000a, 0x9e64000a.toInt(), 0x1e30000a, 0x9e69000a.toInt(), 0x9e58e00a.toInt(),
            0x4e083c0a, 0x4e042c0a, 0xd53bd04a.toInt(), 0xd528750a.toInt())
        for (overwrite in overwrites) {
            val bytes = poolLoadLibrary(0x9140076a.toInt(), overwrite, 0xd503201f.toInt())
            assertFailsWith<PatchException> { app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes).poolOffset(2068) }
        }
        for (preserved in listOf(0x9e67000a.toInt(), 0x9e62000a.toInt(), 0x1e23000a, 0x1e61280a, 0xd51bd04a.toInt(), 0x1e612000)) {
            val bytes = poolLoadLibrary(0x9140076a.toInt(), preserved, 0xd503201f.toInt())
            assertEquals(4096, app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes).poolOffset(2068))
        }
    }

    @Test
    fun ignoreDirectPoolLoadsOnSkippedPaths() {
        val bytes = poolLoadLibrary(0xf9400b60.toInt(), 0x14000002, 0xf9400b60.toInt())
        val methods = app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes)
        assertEquals(16, methods.poolOffset(2056))
        assertFailsWith<PatchException> { methods.poolOffset(2064) }
        assertEquals(listOf(2056), methods.all().single().loads("widget"))
    }

    @Test
    fun ignoreAnUnreachableBackwardEdge() {
        val bytes = poolLoadLibrary(0xd503201f.toInt(), 0x14000004, 0xf9400b60.toInt())
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(2068, 0x17ffffff)
        assertFailsWith<PatchException> { app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes).poolOffset(2064) }
    }

    @Test
    fun retainLoopBodyReachedByBackwardBranch() {
        val bytes = poolLoadLibrary(0xd503201f.toInt(), 0x14000004, 0xf9400b60.toInt())
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        code.putInt(2068, 0xd65f03c0.toInt())
        code.putInt(2076, 0x17fffffd)
        assertEquals(16, app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes).poolOffset(2064))
    }

    @Test
    fun discardPoolBaseAfterSignExtendingLoad() {
        for (overwrite in listOf(0xb980004a.toInt(), 0xb880004a.toInt(), 0xb8a0484a.toInt(),
            0x3980004a, 0x39c0004a, 0x7980004a, 0x79c0004a)) {
            val bytes = poolLoadLibrary(0x9140076a.toInt(), overwrite, 0xd503201f.toInt())
            assertFailsWith<PatchException> { app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes).poolOffset(2068) }
        }
        for (preserved in listOf(0xf980004a.toInt(), 0xf880004a.toInt(), 0xa9812c4a.toInt(), 0xa8812c4a.toInt(), 0xd840004a.toInt())) {
            val bytes = poolLoadLibrary(0x9140076a.toInt(), preserved, 0xd503201f.toInt())
            assertEquals(4096, app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes).poolOffset(2068))
        }
    }

    @Test
    fun discardPoolBaseAfterAtomicUpdateAndRetainItAfterConditionalCompare() {
        val overwritten = poolLoadLibrary(0x9140076a.toInt(), 0xf821004a.toInt(), 0xd503201f.toInt())
        assertFailsWith<PatchException> { app.morphe.patches.ultimea.misc.settings.NativeMethods(overwritten).poolOffset(2068) }
        for (compare in listOf(0xfa42002a.toInt(), 0xba42002a.toInt(), 0xfa42082a.toInt(), 0xba42082a.toInt())) {
            val preserved = poolLoadLibrary(0x9140076a.toInt(), compare, 0xd503201f.toInt())
            assertEquals(4096, app.morphe.patches.ultimea.misc.settings.NativeMethods(preserved).poolOffset(2068))
        }
    }

    @Test
    fun discardCompareAndSwapResultAndExclusivePairLoadResults() {
        for (overwrite in listOf(0xc8aa7c61.toInt(), 0x88aa7c61.toInt(), 0xc87f2841.toInt(), 0xc87fa841.toInt())) {
            val bytes = poolLoadLibrary(0x9140076a.toInt(), overwrite, 0xd503201f.toInt())
            assertFailsWith<PatchException> { app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes).poolOffset(2068) }
        }
        val preserved = poolLoadLibrary(0x9140076a.toInt(), 0xc8a17c6a.toInt(), 0xd503201f.toInt())
        assertEquals(4096, app.morphe.patches.ultimea.misc.settings.NativeMethods(preserved).poolOffset(2068))
    }

    @Test
    fun discardPoolBaseAfterExclusiveStoreStatusWrite() {
        for (instruction in listOf(0xc80a7c61.toInt(), 0xc80afc61.toInt(), 0xc82a0460.toInt(),
            0xc82a8460.toInt(), 0x080a7c61, 0x480a7c61)) {
            val bytes = poolLoadLibrary(0x9140076a.toInt(), instruction, 0xd503201f.toInt())
            assertFailsWith<PatchException> { app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes).poolOffset(2068) }
        }
        val preserved = poolLoadLibrary(0x9140076a.toInt(), 0xc8017c6a.toInt(), 0xd503201f.toInt())
        assertEquals(4096, app.morphe.patches.ultimea.misc.settings.NativeMethods(preserved).poolOffset(2068))
    }

    @Test
    fun retainDerivedPoolBaseAcrossBackwardControlFlow() {
        val bytes = poolLoadLibrary(0x9100036a.toInt(), 0x14000003, 0xf9400d41.toInt())
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        code.putInt(2068, 0xd65f03c0.toInt())
        code.putInt(2072, 0xd503201f.toInt())
        code.putInt(2076, 0x17fffffd)
        val methods = app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes)
        assertEquals(listOf(2064), methods.references(24))
        assertEquals(2056, methods.poolLoadStart(2064))
        code.putInt(2072, 0xd280000a.toInt())
        assertTrue(app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes).references(24).isEmpty())
    }

    @Test
    fun rejectPoolBaseInvalidatedByLoopBackedge() {
        val bytes = poolLoadLibrary(0x9100036a.toInt(), 0x14000001, 0xf9400d41.toInt())
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        code.putInt(2068, 0xd280000a.toInt())
        code.putInt(2072, 0xb5ffffc9.toInt())
        code.putInt(2076, 0xd65f03c0.toInt())
        assertTrue(app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes).references(24).isEmpty())
    }

    @Test
    fun retainPoolBaseDefinedAboveItsBackwardBranchTarget() {
        val bytes = poolLoadLibrary(0x14000004, 0xd503201f.toInt(), 0xf9400d41.toInt())
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        code.putInt(2068, 0xd65f03c0.toInt())
        code.putInt(2072, 0x9100036a.toInt())
        code.putInt(2076, 0x17fffffd)
        val methods = app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes)
        assertEquals(listOf(2064), methods.references(24))
        assertEquals(2072, methods.poolLoadStart(2064))
    }

    @Test
    fun stopAtIndirectCallBeforeLaterDirectCall() {
        val bytes = poolLoadLibrary(0xf9400b60.toInt(), 0xd63f0120.toInt(), 0x94000004.toInt())
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(2068, 0xd65f03c0.toInt())
        val methods = app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes)
        assertEquals(2060, methods.firstCallAfter(2056, 2072))
        assertFailsWith<PatchException> { methods.callTarget(2060) }
    }

    @Test
    fun selectFirstExecutedAsyncInitializerBeforeLowerAddressHelper() {
        val trigger = "/api/base/v1/review/trigger"
        val bytes = library(literal = trigger)
        val instructions = listOf(0xa9bf79fd.toInt(), 0xaa0f03fd.toInt(), 0xf9400b60.toInt(),
            0x14000003, 0x94000004.toInt(), 0xd65f03c0.toInt(), 0x94000004.toInt(), 0x17fffffd,
            0xd65f03c0.toInt(), 0xd503201f.toInt(), 0xd65f03c0.toInt())
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        instructions.forEachIndexed { index, instruction -> code.putInt(2048 + index * Int.SIZE_BYTES, instruction) }
        val methods = app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes)
        assertEquals(2076, methods.asyncBodyStart(methods.single(trigger)))
    }

    @Test
    fun selectAsyncInitializerAfterRejoiningStackCheck() {
        val trigger = "/api/base/v1/review/trigger"
        val bytes = library(literal = trigger)
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val instructions = listOf(0xa9bf79fd.toInt(), 0xaa0f03fd.toInt(), 0xf9400b60.toInt(),
            0xb40000a9.toInt(), 0x9400000a.toInt(), 0x9400000b.toInt(), 0xd65f03c0.toInt(),
            0xd503201f.toInt(), 0x9400000a.toInt(), 0x17fffffb)
        instructions.forEachIndexed { index, instruction -> code.putInt(2048 + index * Int.SIZE_BYTES, instruction) }
        for (address in listOf(2104, 2112, 2120)) code.putInt(address, 0xd65f03c0.toInt())
        val methods = app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes)
        assertEquals(2068, methods.asyncBodyStart(methods.single(trigger)))
        code.putInt(2084, 0xd65f03c0.toInt())
        val ambiguous = app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes)
        assertFailsWith<PatchException> { ambiguous.asyncBodyStart(ambiguous.single(trigger)) }
    }

    @Test
    fun resolveAsyncInitializerWithStockStackOverflowRetry() {
        val directory = System.getenv("ULTIMEA_STOCK_DIRECTORY")
        org.junit.jupiter.api.Assumptions.assumeTrue(directory != null, "Set stock directory")
        val library = java.io.File(directory, "1.4.4-play/x/lib/arm64-v8a/libapp.so").readBytes()
        val methods = app.morphe.patches.ultimea.misc.settings.NativeMethods(library)
        val trigger = methods.single("/api/base/v1/review/trigger")
        assertEquals(0x108cfc8, methods.asyncBodyStart(trigger))
    }

    @Test
    fun ignoreCallsInsideSkippedBlocks() {
        val bytes = poolLoadLibrary(0x14000003, 0x94000001.toInt(), 0x94000001.toInt())
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        code.putInt(2068, 0x94000002.toInt())
        code.putInt(2072, 0xd65f03c0.toInt())
        val methods = app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes)
        assertEquals(listOf(2068), methods.calls(methods.containing(2068)))
        assertEquals(2068, methods.firstCallAfter(2052, 2072))
    }

    @Test
    fun retainEqualPoolOffsetsFromSeparateBranchDefinitions() {
        val bytes = poolLoadLibrary(0xb4000069.toInt(), 0x9100036a.toInt(), 0x14000002)
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        code.putInt(2068, 0x9100036a.toInt())
        code.putInt(2072, 0xf9400941.toInt())
        code.putInt(2076, 0xd65f03c0.toInt())
        val methods = app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes)
        assertEquals(16, methods.poolOffset(2072))
        assertFailsWith<PatchException> { methods.poolLoadStart(2072) }
        code.putInt(2068, 0x9140076a.toInt())
        assertFailsWith<PatchException> { app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes).poolOffset(2072) }
    }

    @Test
    fun discoverPoolLoadsInCalledFramelessFunctions() {
        val bytes = poolLoadLibrary(0x94000006.toInt(), 0xd65f03c0.toInt(), 0xd503201f.toInt())
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        code.putInt(2080, 0xf9400b60.toInt())
        code.putInt(2084, 0xd65f03c0.toInt())
        val methods = app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes)
        assertEquals(16, methods.poolOffset(2080))
        assertEquals(2080, methods.containing(2080).start)
        assertEquals("widget", methods.containing(2080).stringLoads[2080])
    }

    @Test
    fun selectCallReachableFromItsIdentifyingLoad() {
        val bytes = poolLoadLibrary(0xb4000069.toInt(), 0xf9400b60.toInt(), 0x14000002)
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        code.putInt(2068, 0x94000004.toInt())
        code.putInt(2072, 0x94000003.toInt())
        code.putInt(2076, 0xd65f03c0.toInt())
        val methods = app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes)
        assertEquals(2072, methods.firstCallAfter(2060, 2080))
    }

    @Test
    fun retainCalledFunctionBoundaryWhenTailBranchAlsoReachesIt() {
        val bytes = poolLoadLibrary(0x94000006.toInt(), 0x14000005, 0xd503201f.toInt())
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        code.putInt(2080, 0xf9400b60.toInt())
        code.putInt(2084, 0xd65f03c0.toInt())
        val methods = app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes)
        assertEquals(2080, methods.containing(2080).start)
        assertEquals("widget", methods.containing(2080).stringLoads[2080])
    }

    @Test
    fun selectFirstExecutedCallBeforeLowerAddressCall() {
        val bytes = poolLoadLibrary(0xf9400b60.toInt(), 0x14000003, 0x94000008.toInt())
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        code.putInt(2068, 0xd65f03c0.toInt())
        code.putInt(2072, 0x94000006.toInt())
        code.putInt(2076, 0x17fffffd)
        code.putInt(2096, 0xd65f03c0.toInt())
        val methods = app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes)
        assertEquals(2072, methods.firstCallAfter(2056, 2080))
    }

    @Test
    fun rejectAmbiguousFirstCallsOnSeparateBranches() {
        val bytes = poolLoadLibrary(0xf9400b60.toInt(), 0xb4000069.toInt(), 0x94000008.toInt())
        val code = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        code.putInt(2068, 0xd65f03c0.toInt())
        code.putInt(2072, 0x94000006.toInt())
        code.putInt(2076, 0xd65f03c0.toInt())
        code.putInt(2096, 0xd65f03c0.toInt())
        val methods = app.morphe.patches.ultimea.misc.settings.NativeMethods(bytes)
        assertFailsWith<PatchException> { methods.firstCallAfter(2056, 2080) }
    }

    @Test
    fun rejectArrayReferencedByAnotherPoolEntry() {
        val snapshot = DartSnapshot(library(sharedArray = true))
        assertFailsWith<PatchException> { snapshot.removeFirstArrayElement(24) }
    }

    @Test
    fun rejectArrayContainingPredefinedObjects() {
        val snapshot = DartSnapshot(library(widgetElements = false))
        assertTrue(snapshot.widgetArrayPoolOffsets().isEmpty())
        assertFailsWith<PatchException> { snapshot.removeFirstArrayElement(24) }
    }

    private fun library(sharedArray: Boolean = false, widgetElements: Boolean = true, literal: String = "widget", unboxed: Boolean = false, poolImmediate: Boolean = false): ByteArray {
        val poolLength = 131
        val stream = ByteArrayOutputStream()
        fun unsigned(value: Int, marker: Int = 128) {
            var rest = value
            while (rest >= if (marker == 192) 64 else 128) {
                stream.write(rest and 127)
                rest = rest ushr 7
            }
            stream.write(rest + marker)
        }
        fun reference(value: Int) {
            val groups = mutableListOf((value and 127) or 128)
            var rest = value ushr 7
            while (rest > 0) {
                groups += rest and 127
                rest = rest ushr 7
            }
            groups.reversed().forEach(stream::write)
        }
        fun cluster(cid: Int, count: Int, canonical: Boolean = false) {
            unsigned((cid shl 12) or if (canonical) 2 else 0, 192)
            unsigned(count)
        }
        listOf(128, 165, 4, 0, 0).forEach { unsigned(it) }
        cluster(93, 1, true)
        unsigned(literal.length shl 1)
        unsigned(1)
        unsigned(1)
        cluster(91, 32, true)
        unsigned(3)
        repeat(31) { unsigned(0) }
        cluster(175, 3, true)
        unsigned(if (unboxed) 3 else 2, 192)
        unsigned(if (unboxed) 3 else 2, 192)
        cluster(23, 1)
        unsigned(poolLength)
        unsigned(literal.length shl 1)
        stream.write(literal.toByteArray(Charsets.ISO_8859_1))
        unsigned(3)
        reference(1)
        listOf(if (widgetElements) 162 else 129, 163, 164).forEach(::reference)
        repeat(31) {
            unsigned(0)
            reference(1)
        }
        unsigned(if (unboxed) 4 else 0)
        if (unboxed) {
            repeat(3) {
                stream.write(1)
                stream.write(0x82)
                unsigned(0, 192)
            }
        }
        unsigned(poolLength)
        listOf(129, 130).plus(if (sharedArray) listOf(130) else emptyList()).forEach {
            stream.write(1)
            reference(it)
        }
        repeat(poolLength - if (sharedArray) 3 else 2) { index ->
            stream.write(0)
            if (poolImmediate && index == 0) {
                stream.write(1)
                stream.write(0x82)
            } else {
                unsigned(0, 192)
            }
        }
        val features = "compressed-pointers\u0000".toByteArray()
        val payload = stream.toByteArray()
        val result = ByteArray(LIBRARY_BYTES)
        val elf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN)
        elf.putLong(40, 64)
        elf.putShort(58, 64)
        elf.putShort(60, 3)
        elf.putInt(128 + 4, 11)
        elf.putLong(128 + 24, 256)
        elf.putLong(128 + 32, 48)
        elf.putInt(128 + 40, 2)
        elf.putLong(128 + 56, 24)
        elf.putLong(192 + 24, 304)
        val symbol = "\u0000_kDartIsolateSnapshotData\u0000".toByteArray()
        symbol.copyInto(result, 304)
        elf.putInt(280, 1)
        elf.putLong(288, SNAPSHOT_OFFSET.toLong())
        elf.putLong(32, 512)
        elf.putShort(54, 56)
        elf.putShort(56, 2)
        elf.putInt(512, 1)
        elf.putLong(512 + 32, 2048)
        elf.putInt(568, 1)
        elf.putInt(568 + 4, 1)
        elf.putLong(568 + 8, 2048)
        elf.putLong(568 + 16, 2048)
        elf.putLong(568 + 32, 256)
        elf.putInt(SNAPSHOT_OFFSET, 0xdcdcf5f5.toInt())
        elf.putLong(SNAPSHOT_OFFSET + 4, (52 + features.size + payload.size - 4).toLong())
        "97ff04a728735e6b6b098bdf983faaba".toByteArray().copyInto(result, SNAPSHOT_OFFSET + 20)
        features.copyInto(result, SNAPSHOT_OFFSET + 52)
        payload.copyInto(result, SNAPSHOT_OFFSET + 52 + features.size)
        return result
    }

    private companion object {
        const val LIBRARY_BYTES = 2304
        const val SNAPSHOT_OFFSET = 768
    }
}
