/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.theme

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

private const val COLOR_CONSTRUCTOR_RETURN_ADDRESS = 36

class ThemeInstructionsTest {
    private data class Stock(val path: String, val constructor: Int, val gradientReturn: Int?)

    @Test
    fun discardColorTrackingAfterPairLoad() {
        assertEquals(1, recordedColorStores(0xd503201f.toInt()))
        assertEquals(0, recordedColorStores(0xa9400be1.toInt()))
        assertEquals(0, recordedColorStores(0xd63f0120.toInt()))
        for (literal in listOf(0x18000002, 0x58000002, 0x98000002.toInt())) {
            assertEquals(0, recordedColorStores(literal))
        }
        assertEquals(1, recordedColorStores(0xd8000002.toInt()))
    }

    @Test
    fun discardColorAfterFloatingPointOrSystemWrite() {
        for (overwrite in listOf(0x9e660002.toInt(), 0x1e260002, 0x9eae0002.toInt(), 0x9e780002.toInt(),
            0x1e210002, 0x9e640002.toInt(), 0x1e300002, 0x9e690002.toInt(), 0x9e58e002.toInt(),
            0x4e083c02, 0x4e042c02, 0xd53bd042.toInt(), 0xd5287502.toInt())) {
            assertEquals(0, recordedColorStores(overwrite))
        }
        for (preserved in listOf(0x9e670002.toInt(), 0x9e620002.toInt(), 0x1e230002, 0x1e612802, 0xd51bd042.toInt(), 0x1e612000)) {
            assertEquals(1, recordedColorStores(preserved))
        }
    }

    @Test
    fun discardColorAfterSignExtendingLoad() {
        for (overwrite in listOf(0xb9800042.toInt(), 0xb8800042.toInt(), 0xb8a04842.toInt(),
            0x39800042, 0x39c00042, 0x79800042, 0x79c00042)) {
            assertEquals(0, recordedColorStores(overwrite))
        }
        for (preserved in listOf(0xf9800042.toInt(), 0xf8800042.toInt(), 0xa9810c82.toInt(), 0xa8810c82.toInt(), 0xd8400042.toInt())) {
            assertEquals(1, recordedColorStores(preserved))
        }
    }

    @Test
    fun retainColorAcrossCyclicReceiverFields() {
        val bytes = colorTrackingBytes(listOf(0xf800f294.toInt(), 0xf840f295.toInt(),
            0xb800b2a2.toInt(), 0xb840b2a2.toInt(), 0xb8007282.toInt(), 0xb5ffff69.toInt(), 0xd65f03c0.toInt()))
        assertEquals(2, collectColorStores(bytes))
    }

    @Test
    fun preserveColorAcrossRecognizedBarrierWrapper() {
        for (register in listOf(2, 19, 20)) {
            assertEquals(1, recordedColorStores(0x9400000b.toInt(), register))
            assertEquals(0, recordedColorStores(0x9400000c.toInt(), register))
        }
    }

    @Test
    fun discardColorAfterAtomicUpdateAndRetainItAfterConditionalCompare() {
        assertEquals(0, recordedColorStores(0xf8210042.toInt()))
        for (compare in listOf(0xfa420022.toInt(), 0xba420022.toInt(), 0xfa420822.toInt(), 0xba420822.toInt())) {
            assertEquals(1, recordedColorStores(compare))
        }
    }

    @Test
    fun discardCompareAndSwapResultAndExclusivePairLoadResults() {
        assertEquals(0, recordedColorStores(0xc8a27c61.toInt()))
        assertEquals(1, recordedColorStores(0xc8a17c62.toInt()))
        assertEquals(0, recordedColorStores(0xc87f0841.toInt()))
        assertEquals(0, recordedColorStores(0xc87f8841.toInt()))
    }

    @Test
    fun discardExclusiveStoreStatusRegisterAndPreserveStoredValue() {
        for (instruction in listOf(0xc8027c61.toInt(), 0xc802fc61.toInt(), 0xc8220460.toInt(),
            0xc8228460.toInt(), 0x08027c61, 0x48027c61)) {
            assertEquals(0, recordedColorStores(instruction))
        }
        assertEquals(1, recordedColorStores(0xc8017c62.toInt()))
        assertEquals(1, recordedColorStores(0xc89ffc62.toInt()))
    }

    @Test
    fun discardColorStateAfterIndirectTailBranch() {
        val bytes = colorTrackingBytes(listOf(0xaa1f03e2.toInt(), 0xb4000089.toInt(),
            0xb800b282.toInt(), 0xd65f03c0.toInt(), 0xd503201f.toInt(), 0xf85f03a2.toInt(), 0xd61f0200.toInt()))
        assertEquals(0, collectColorStores(bytes))
    }

    @Test
    fun discardWritesToZeroRegisterBeforeColorStores() {
        assertEquals(0, collectColorStores(colorTrackingBytes(listOf(0xaa0203ff.toInt(),
            0xb800b29f.toInt(), 0xd65f03c0.toInt()))))
        assertEquals(1, collectColorStores(colorTrackingBytes(listOf(0xb800f282.toInt(),
            0xb840f29f.toInt(), 0xb800b29f.toInt(), 0xd65f03c0.toInt()))))
        assertEquals(0, collectColorStores(colorTrackingBytes(listOf(0xb9000e82.toInt(),
            0xb9400e9f.toInt(), 0xb800b29f.toInt(), 0xd65f03c0.toInt()))))
    }

    @Test
    fun discardHighRegisterIdentityAcrossDartCalls() {
        for (register in listOf(19, 20)) {
            assertEquals(1, recordedColorStores(0xd503201f.toInt(), register))
            assertEquals(0, recordedColorStores(0xd63f0120.toInt(), register))
            assertEquals(0, recordedColorStores(0x9400001c.toInt(), register))
        }
    }

    @Test
    fun retainColorAfterOneBranchOverwritesReceiverField() {
        val bytes = colorTrackingBytes(listOf(0xb800b282.toInt(), 0xb4000080.toInt(), 0xd2800002.toInt(),
            0xb800b282.toInt(), 0xd503201f.toInt(), 0xb840b282.toInt(), 0xb8007282.toInt(), 0xd65f03c0.toInt()))
        assertEquals(2, collectColorStores(bytes))
    }

    @Test
    fun discardColorAfterDefiniteReceiverFieldOverwrite() {
        val bytes = colorTrackingBytes(listOf(0xb800f282.toInt(), 0xb800f29f.toInt(),
            0xb840f282.toInt(), 0xb800b282.toInt(), 0xd65f03c0.toInt()))
        assertEquals(1, collectColorStores(bytes))
    }

    @Test
    fun discardFrameColorAfterStoreThroughComputedAddress() {
        for (store in listOf(0x889ffd3f.toInt(), 0xb800013f.toInt(), 0xb900013f.toInt(), 0xa9007d3f.toInt(),
            0xbd000120.toInt(), 0x88087d3f.toInt(), 0x88a87d3f.toInt(), 0x88e87d3f.toInt(), 0xb820013f.toInt())) {
            val bytes = colorTrackingBytes(listOf(0xf81e83a2.toInt(), 0xd10063a9.toInt(), store,
                0xf85e83a2.toInt(), 0xb800b282.toInt(), 0xd65f03c0.toInt()))
            assertEquals(0, collectColorStores(bytes), store.toUInt().toString(16))
        }
        for (preserved in listOf(0x88dffd20.toInt(), 0x885ffd20.toInt(), 0xf9800120.toInt(), 0x889ffe9f.toInt())) {
            val bytes = colorTrackingBytes(listOf(0xf81e83a2.toInt(), 0xd10063a9.toInt(), preserved,
                0xf85e83a2.toInt(), 0xb800b282.toInt(), 0xd65f03c0.toInt()))
            assertEquals(1, collectColorStores(bytes), preserved.toUInt().toString(16))
        }
    }

    @Test
    fun trackCopiedFrameAddressUntilRegisterIsOverwritten() {
        val copied = colorTrackingBytes(listOf(0xf81e83a2.toInt(), 0xd10063a9.toInt(), 0xaa0903ea.toInt(),
            0x889ffd5f.toInt(), 0xf85e83a2.toInt(), 0xb800b282.toInt(), 0xd65f03c0.toInt()))
        assertEquals(0, collectColorStores(copied))
        val overwritten = colorTrackingBytes(listOf(0xf81e83a2.toInt(), 0xd10063a9.toInt(), 0xaa1403e9.toInt(),
            0x889ffd3f.toInt(), 0xf85e83a2.toInt(), 0xb800b282.toInt(), 0xd65f03c0.toInt()))
        assertEquals(1, collectColorStores(overwritten))
        val joined = colorTrackingBytes(listOf(0xf81e83a2.toInt(), 0xaa1403e9.toInt(), 0xb4000080.toInt(),
            0xd10063a9.toInt(), 0x14000003, 0xd503201f.toInt(), 0xf81e83bf.toInt(),
            0x889ffd3f.toInt(), 0xf85e83a2.toInt(), 0xb800b282.toInt(), 0xd65f03c0.toInt()))
        assertEquals(0, collectColorStores(joined))
    }

    @Test
    fun readOnlyBytesRecordedByFieldStore() {
        val narrowLoad = colorTrackingBytes(listOf(0xf800f282.toInt(), 0xb840f282.toInt(),
            0xb800b282.toInt(), 0xd65f03c0.toInt()))
        assertEquals(2, collectColorStores(narrowLoad))
        val wideLoad = colorTrackingBytes(listOf(0xb800f282.toInt(), 0xf840f282.toInt(),
            0xb800b282.toInt(), 0xd65f03c0.toInt()))
        assertEquals(1, collectColorStores(wideLoad))
    }

    @Test
    fun readOnlyBytesRecordedByFrameStore() {
        for ((save, load, expected) in listOf(
            Triple(0xb81e83a2.toInt(), 0xf85e83a2.toInt(), 0),
            Triple(0xf81e83a2.toInt(), 0xb85e83a2.toInt(), 1),
            Triple(0xb81e83a2.toInt(), 0xb85e83a2.toInt(), 1),
            Triple(0xf81e83a2.toInt(), 0xf85e83a2.toInt(), 1),
        )) {
            val bytes = colorTrackingBytes(listOf(save, load, 0xb800b282.toInt(), 0xd65f03c0.toInt()))
            assertEquals(expected, collectColorStores(bytes), load.toUInt().toString(16))
        }
        val adjacent = colorTrackingBytes(listOf(0xb81e83a2.toInt(), 0xb81ec3bf.toInt(),
            0xb85e83a2.toInt(), 0xb800b282.toInt(), 0xd65f03c0.toInt()))
        assertEquals(1, collectColorStores(adjacent))
    }

    @Test
    fun discardFrameColorAfterStoreThroughDartStackPointer() {
        for (call in listOf(0xd503201f.toInt(), 0x9400000a.toInt(), 0x9400001e.toInt())) {
            val bytes = colorTrackingBytes(listOf(0xaa0f03fd.toInt(), 0xf81e83a2.toInt(), call,
                0xb81e81ff.toInt(), 0xf85e83a2.toInt(), 0xb800b282.toInt(), 0xd65f03c0.toInt()))
            assertEquals(0, collectColorStores(bytes), call.toUInt().toString(16))
        }
    }

    @Test
    fun invalidateOnlyOverlappingDartStackWritesAfterAdjustment() {
        for ((writes, expected) in listOf(
            listOf(0xd10081ef.toInt(), 0xb80081ff.toInt()) to 0,
            listOf(0xd10081ef.toInt(), 0xb80101ff.toInt()) to 1,
            listOf(0xb81e05ff.toInt()) to 1,
            listOf(0xb81e05ff.toInt(), 0xb80081ff.toInt()) to 0,
            listOf(0xb81e8dff.toInt()) to 0,
            listOf(0xa9be7dff.toInt()) to 0,
            listOf(0xa8be7dff.toInt()) to 1,
            listOf(0xa8be7dff.toInt(), 0xb80081ff.toInt()) to 0,
        )) {
            val bytes = colorTrackingBytes(listOf(0xaa0f03fd.toInt(), 0xf81e83a2.toInt()) + writes +
                listOf(0xf85e83a2.toInt(), 0xb800b282.toInt(), 0xd65f03c0.toInt()))
            assertEquals(expected, collectColorStores(bytes), writes.joinToString { it.toUInt().toString(16) })
        }
    }

    @Test
    fun discardFrameColorsPassedToUnknownCallee() {
        for (call in listOf(0x9400002d.toInt(), 0xd63f0120.toInt())) {
            val escaped = colorTrackingBytes(listOf(0xf81e83a2.toInt(), 0xd10063a0.toInt(), call,
                0xf85f83b4.toInt(), 0xf85e83a2.toInt(), 0xb800b282.toInt(), 0xd65f03c0.toInt()))
            assertEquals(0, collectColorStores(escaped))
        }
        val overwrittenArgument = colorTrackingBytes(listOf(0xf81e83a2.toInt(), 0xd10063a0.toInt(),
            0xaa1f03e0.toInt(), 0x9400002c.toInt(), 0xf85f83b4.toInt(), 0xf85e83a2.toInt(),
            0xb800b282.toInt(), 0xd65f03c0.toInt()))
        assertEquals(1, collectColorStores(overwrittenArgument))
    }

    @Test
    fun retainFrameAliasAcrossSpillAndReload() {
        val bytes = colorTrackingBytes(listOf(0xf81e83a2.toInt(), 0xd10063a9.toInt(),
            0xf81e03a9.toInt(), 0xaa1f03e9.toInt(), 0xf85e03a9.toInt(), 0xb900013f.toInt(),
            0xf85e83a2.toInt(), 0xb800b282.toInt(), 0xd65f03c0.toInt()))
        assertEquals(0, collectColorStores(bytes))
        val overwritten = colorTrackingBytes(listOf(0xf81e83a2.toInt(), 0xd10063a9.toInt(),
            0xf81e03a9.toInt(), 0xf81e03b4.toInt(), 0xf85e03a9.toInt(), 0xb900013f.toInt(),
            0xf85e83a2.toInt(), 0xb800b282.toInt(), 0xd65f03c0.toInt()))
        assertEquals(1, collectColorStores(overwritten))
    }

    @Test
    fun discardFieldColorAfterOverlappingIntegerStore() {
        for ((save, overwrite, load) in listOf(
            Triple(0xb800f282.toInt(), 0xf800b29f.toInt(), 0xb840f282.toInt()),
            Triple(0xf800f282.toInt(), 0xb801329f.toInt(), 0xf840f282.toInt()),
        )) {
            val bytes = colorTrackingBytes(listOf(save, overwrite, load, 0xb800b282.toInt(), 0xd65f03c0.toInt()))
            assertEquals(1, collectColorStores(bytes), overwrite.toUInt().toString(16))
        }
        for ((save, overwrite, load) in listOf(
            Triple(0xb800f282.toInt(), 0xb800b29f.toInt(), 0xb840f282.toInt()),
            Triple(0xb800f282.toInt(), 0xb801329f.toInt(), 0xb840f282.toInt()),
            Triple(0xf800f282.toInt(), 0xb801729f.toInt(), 0xf840f282.toInt()),
        )) {
            val bytes = colorTrackingBytes(listOf(save, overwrite, load, 0xb800b282.toInt(), 0xd65f03c0.toInt()))
            assertEquals(2, collectColorStores(bytes), overwrite.toUInt().toString(16))
        }
    }

    @Test
    fun discardFieldColorAfterStoreThroughComputedAddress() {
        for (store in listOf(0x889ffd3f.toInt(), 0xb800013f.toInt(), 0xb900013f.toInt(), 0xa9007d3f.toInt(),
            0xbd000120.toInt(), 0x88087d3f.toInt(), 0x88a87d3f.toInt(), 0x88e87d3f.toInt(), 0xb820013f.toInt())) {
            val bytes = colorTrackingBytes(listOf(0xb800f282.toInt(), 0x91003e89.toInt(), store,
                0xb840f282.toInt(), 0xb800b282.toInt(), 0xd65f03c0.toInt()))
            assertEquals(1, collectColorStores(bytes), store.toUInt().toString(16))
        }
        for (load in listOf(0x88dffd20.toInt(), 0x885ffd20.toInt(), 0xf9800120.toInt())) {
            val bytes = colorTrackingBytes(listOf(0xb800f282.toInt(), 0x91003e89.toInt(), load,
                0xb840f282.toInt(), 0xb800b282.toInt(), 0xd65f03c0.toInt()))
            assertEquals(2, collectColorStores(bytes), load.toUInt().toString(16))
        }
    }

    @Test
    fun discardFieldColorAfterUnknownCalleeCanOverwriteIt() {
        val bytes = colorTrackingBytes(listOf(0xb800f282.toInt(), 0x9400002f.toInt(),
            0xf85f83b4.toInt(), 0xb840f282.toInt(), 0xb800b282.toInt(), 0xd65f03c0.toInt()))
        assertEquals(1, collectColorStores(bytes))
    }

    @Test
    fun retainLocalColorAcrossRotatedLoopFallthrough() {
        val bytes = colorTrackingBytes(listOf(0x14000005, 0xf85f03a2.toInt(), 0xf81e83a2.toInt(),
            0xaa1f03e2.toInt(), 0xd503201f.toInt(), 0xf85e83a2.toInt(), 0xb5ffff69.toInt(),
            0xb800b282.toInt(), 0xd65f03c0.toInt()))
        assertEquals(1, collectColorStores(bytes))
    }

    private fun recordedColorStores(overwrite: Int, register: Int = 2): Int {
        val bytes = colorTrackingBytes(listOf(0xaa0203e0.toInt() or register, overwrite,
            0xaa0003e2.toInt() or (register shl 16), 0xb800b282.toInt(), 0xd65f03c0.toInt()))
        return collectColorStores(bytes)
    }

    @Test
    fun discardFrameColorOverlappedByScalarStore() {
        for (store in listOf(0xf81ec3bf.toInt(), 0xb81ec3bf.toInt(), 0x781ec3bf, 0x381ec3bf,
            0xfc1ec3a0.toInt(), 0xbc1ec3a0.toInt(), 0x3c9e03a0, 0xf90007bf.toInt())) {
            val offset = if (store == 0xf90007bf.toInt()) 8 else -20
            val save = if (offset == 8) 0xf90007a2.toInt() else 0xf81e83a2.toInt()
            val load = if (offset == 8) 0xf94007a2.toInt() else 0xf85e83a2.toInt()
            val bytes = colorTrackingBytes(listOf(save, store, load, 0xb800b282.toInt(), 0xd65f03c0.toInt()))
            assertEquals(0, collectColorStores(bytes), store.toUInt().toString(16))
        }
        val bytes = colorTrackingBytes(listOf(0xf81e83a2.toInt(), 0xbc1e03a0.toInt(), 0xf85e83a2.toInt(),
            0xb800b282.toInt(), 0xd65f03c0.toInt()))
        assertEquals(1, collectColorStores(bytes))
    }

    @Test
    fun discardFrameColorOverlappedByPairedStore() {
        for (store in listOf(0xa93effbf.toInt(), 0xa93e7fbf.toInt(), 0x293d7fbf, 0xad3f7fbf.toInt(),
            0xa9be7fbf.toInt(), 0xa8be7fbf.toInt())) {
            val bytes = colorTrackingBytes(listOf(0xf81e83a2.toInt(), store, 0xf85e83a2.toInt(),
                0xb800b282.toInt(), 0xd65f03c0.toInt()))
            assertEquals(0, collectColorStores(bytes), store.toUInt().toString(16))
        }
        val bytes = colorTrackingBytes(listOf(0xf81e83a2.toInt(), 0xa93c7fbf.toInt(), 0xf85e83a2.toInt(),
            0xb800b282.toInt(), 0xd65f03c0.toInt()))
        assertEquals(1, collectColorStores(bytes))
    }

    private fun colorTrackingBytes(suffix: List<Int>): ByteArray {
        val bytes = ByteArray(256)
        val code = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val prefix = listOf(0xa9bf79fd.toInt(), 0x9400002b.toInt(), 0xaa0003f4.toInt(), 0xf81f83b4.toInt(),
            0x9400002c.toInt(), 0xaa0003f3.toInt(), 0xf81f03b3.toInt(), 0xaa1303e1.toInt(),
            0x94000020.toInt(), 0xf85f83b4.toInt(), 0xf85f03b3.toInt(), 0xaa1303e2.toInt())
        (prefix + suffix).forEachIndexed { index, instruction -> code.putInt(index * Int.SIZE_BYTES, instruction) }
        for ((address, header) in listOf(176 to ((291 shl 12) or (7 shl 8)), 192 to ((292 shl 12) or (3 shl 8)))) {
            code.putInt(address, 0xd2800000.toInt() or ((header and 0xffff) shl 5))
            code.putInt(address + 4, 0xf2a00000.toInt() or ((header ushr 16) shl 5))
            code.putInt(address + 8, 0x14000000 or ((240 - address - 8) / Int.SIZE_BYTES))
        }
        listOf(0xf81f8dfe.toInt(), 0xf81f8de1.toInt(), 0xaa0a03e1.toInt(), 0xf943f35e.toInt(),
            0xd63f03c0.toInt(), 0xf84085e1.toInt(), 0xf84085fe.toInt(), 0xd65f03c0.toInt())
            .forEachIndexed { index, instruction -> code.putInt(96 + index * Int.SIZE_BYTES, instruction) }
        return bytes
    }

    private fun collectColorStores(bytes: ByteArray): Int {
        val scanner = ThemeInstructions(bytes, 0, bytes.size)
        ThemeInstructions::class.java.getDeclaredMethod("collectColors", Int::class.javaPrimitiveType, MutableSet::class.java)
            .apply { isAccessible = true }.invoke(scanner, 160, mutableSetOf<Int>())
        val stores = ThemeInstructions::class.java.getDeclaredField("stores").apply { isAccessible = true }.get(scanner) as Set<*>
        for (store in stores) {
            val caller = store!!.javaClass.getDeclaredField("caller").apply { isAccessible = true }.getInt(store)
            assertEquals(COLOR_CONSTRUCTOR_RETURN_ADDRESS, caller)
        }
        return stores.size
    }

    @Test
    fun matchColorAndGradientHooksAcrossStockVersions() {
        val directory = System.getenv("ULTIMEA_STOCK_DIRECTORY")
        assumeTrue(directory != null, "Set ULTIMEA_STOCK_DIRECTORY to verify stock libraries")
        val targets = listOf(
            Stock("1.4.4-play/x/lib/arm64-v8a/libapp.so", 0x599e1c, 0x7d4fc8),
            Stock("1.4.1-apkpure/lib/arm64-v8a/libapp.so", 0x580414, null),
        )
        for ((path, constructor, gradientReturn) in targets) {
            val flutter = File(directory, path).resolveSibling("libflutter.so").readBytes()
            val priority = FlutterImageDecoder.priorityInstruction(flutter)
            assertEquals(0x12800008, java.nio.ByteBuffer.wrap(flutter).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt(priority))
            val image = ThemeElfImage(File(directory, path).readBytes())
            val matches = ThemeInstructions(image.bytes, image.codeStart, image.codeEnd).match()
            assertEquals(constructor, matches.colorConstructor, path)
            if (gradientReturn != null) {
                assertEquals(ThemeColorRole.SURFACE, matches.colorRolesByReturnAddress[0xa1b398])
            }
            gradientReturn?.let { assertEquals(it, matches.gradientReturn, path) }
            assertTrue(matches.colorRolesByReturnAddress.values.count { it == ThemeColorRole.INK } > 100, path)
            assertTrue(matches.colorRolesByReturnAddress.values.count { it == ThemeColorRole.SURFACE } > 100, path)
            assertTrue(matches.colorRolesByReturnAddress.values.count { it == ThemeColorRole.LINE } > 20, path)
            assertTrue(matches.colorRolesByReturnAddress.values.count { it == ThemeColorRole.PAGE } >= 10, path)
            FlutterImageDecoder.apply(flutter)
            assertEquals(0x52800028, java.nio.ByteBuffer.wrap(flutter).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt(priority))
            val originalColor = java.nio.ByteBuffer.wrap(image.bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt(matches.colorComponents)
            val originalGradient = java.nio.ByteBuffer.wrap(image.bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt(matches.gradientReturn)
            val originalSurface = java.nio.ByteBuffer.wrap(image.bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt(matches.containerColorStore)
            val originalSize = image.bytes.size
            ThemeHooks.apply(image, matches)
            val patched = java.nio.ByteBuffer.wrap(image.bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            assertEquals(originalSize, image.bytes.size)
            assertEquals(0x94000000.toInt(), patched.getInt(matches.colorComponents) and 0xfc000000.toInt())
            assertEquals(0x94000000.toInt(), patched.getInt(matches.gradientReturn) and 0xfc000000.toInt())
            assertEquals(0x94000000.toInt(), patched.getInt(matches.containerColorStore) and 0xfc000000.toInt())
            for ((site, header) in listOf(matches.colorComponents to 0, matches.gradientReturn to 4, matches.containerColorStore to 36)) {
                val target = image.instructionAddress(site) + (patched.getInt(site) shl 6 shr 4)
                assertEquals(image.hookAddress + patched.getInt(image.hookOffset + header), target)
            }
            val replay = patched.getInt(image.hookOffset + 8)
            assertEquals(originalColor, patched.getInt(image.hookOffset + replay))
            assertEquals(originalGradient, patched.getInt(image.hookOffset + patched.getInt(image.hookOffset + 12)))
            assertEquals(originalSurface, patched.getInt(image.hookOffset + patched.getInt(image.hookOffset + 40)))
            println("$path: constructor=${matches.colorConstructor.toString(16)}, gradient=${matches.gradientReturn.toString(16)}, roles=${matches.colorRolesByReturnAddress.values.groupingBy { it }.eachCount()}")
        }
    }
}
