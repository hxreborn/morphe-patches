/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.theme

import app.morphe.patcher.patch.PatchException
import java.io.File
import java.nio.file.Files
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ThemeMatcherTest {
    @Test
    fun matchColorGradientContainerAndCallerRolesWithoutStock() {
        val fixture = Fixture()
        assertEquals(fixture.expected, ThemeInstructions(fixture.bytes, 0, fixture.bytes.size).match())
    }

    @Test
    fun discoverNestedColorFactoriesBeforeTheirCallers() {
        val fixture = Fixture(factoryDepth = 3)
        assertEquals(fixture.expected, ThemeInstructions(fixture.bytes, 0, fixture.bytes.size).match())
    }

    @Test
    fun retainFactoryRoleWhenReturnedColorAlsoFillsLocalInkField() {
        val fixture = Fixture(factoryDepth = 1, factoryInk = true)
        assertEquals(fixture.expected, ThemeInstructions(fixture.bytes, 0, fixture.bytes.size).match())
    }

    @Test
    fun classifyColorAssignmentReachedThroughRotatedLoop() {
        val fixture = Fixture(rotatedLoop = true)
        assertEquals(fixture.expected, ThemeInstructions(fixture.bytes, 0, fixture.bytes.size).match())
    }

    @Test
    fun classifyColorCarriedAcrossLoopIterations() {
        for (iterations in listOf(1, 3)) {
            val fixture = Fixture(carriedIterations = iterations)
            assertEquals(fixture.expected, ThemeInstructions(fixture.bytes, 0, fixture.bytes.size).match())
        }
    }

    @Test
    fun ignoreColorAssignmentAfterReturn() {
        val fixture = Fixture(trailingStore = true)
        assertEquals(fixture.expected, ThemeInstructions(fixture.bytes, 0, fixture.bytes.size).match())
    }

    @Test
    fun rejectUnsafeGradientReplayInstructions() {
        for (instruction in listOf(0xa8c179fd.toInt(), 0xaa1003e0.toInt(), 0xaa0f03e0.toInt(),
            0xaa1e03e0.toInt(), 0xaa1f03e0.toInt(), 0xaa0203e1.toInt())) {
            val fixture = Fixture()
            ByteBuffer.wrap(fixture.bytes).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(fixture.expected.gradientReturn, instruction)
            assertFailsWith<PatchException> { ThemeInstructions(fixture.bytes, 0, fixture.bytes.size).match() }
        }
        val fixture = Fixture()
        ByteBuffer.wrap(fixture.bytes).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(fixture.expected.gradientReturn + Int.SIZE_BYTES, 0xd503201f.toInt())
        assertFailsWith<PatchException> { ThemeInstructions(fixture.bytes, 0, fixture.bytes.size).match() }
    }

    @Test
    fun resolveInkThroughThreeFactoryFramesInRuntimeHook() {
        val directory = System.getenv("ULTIMEA_STOCK_DIRECTORY")
        val python = System.getenv("ULTIMEA_ARM64_PYTHON")
        assumeTrue(directory != null && python != null, "Set stock directory and ARM64 Python interpreter")
        val image = ThemeElfImage(File(directory, "1.4.4-play/x/lib/arm64-v8a/libapp.so").readBytes())
        val matches = ThemeInstructions(image.bytes, image.codeStart, image.codeEnd).match()
        val factories = matches.colorRolesByReturnAddress.filterValues { it == ThemeColorRole.FACTORY }.keys.take(3)
        assertEquals(3, factories.size)
        val ink = matches.colorRolesByReturnAddress.entries.first { it.value == ThemeColorRole.INK }.key
        val cache = image.modeCacheAddress
        ThemeHooks.apply(image, matches)
        val temporary = Files.createTempDirectory("theme-runtime").toFile()
        try {
            val library = temporary.resolve("library.so").apply { writeBytes(image.bytes) }
            val configuration = temporary.resolve("configuration.json").apply {
                writeText("""{"factories":${factories.map(image::instructionAddress)},"ink":${image.instructionAddress(ink)},"cache":$cache,"site":${image.instructionAddress(matches.colorComponents)}}""")
            }
            val script = temporary.resolve("resolve_factory_color.py")
            checkNotNull(javaClass.getResourceAsStream("/ultimea/resolve_factory_color.py")).use {
                script.outputStream().use(it::copyTo)
            }
            for (bytes in listOf(image.bytes, relocateFileSegments(image.bytes))) {
                library.writeBytes(bytes)
                val process = ProcessBuilder(python, script.path, library.path, configuration.path).redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().readText()
                assertEquals(0, process.waitFor(), output)
                println(output)
            }
        } finally {
            temporary.deleteRecursively()
        }
    }

    private fun relocateFileSegments(bytes: ByteArray): ByteArray {
        val padding = 65536
        val relocated = ByteArray(bytes.size + padding)
        bytes.copyInto(relocated, padding)
        bytes.copyInto(relocated, endIndex = minOf(padding, bytes.size))
        val header = ByteBuffer.wrap(relocated).order(ByteOrder.LITTLE_ENDIAN)
        val programHeaders = header.getLong(32).toInt()
        val entrySize = header.getShort(54).toInt() and 0xffff
        val count = header.getShort(56).toInt() and 0xffff
        val loads = (0 until count).map { programHeaders + it * entrySize }.filter { header.getInt(it) == 1 }
        for (load in loads) header.putLong(load + 8, header.getLong(load + 8) + padding)
        val last = loads.maxBy { header.getLong(it + 16) }
        header.putLong(last + 40, header.getLong(last + 40) + padding * 2)
        return relocated
    }

    private class Fixture(factoryDepth: Int = 0, rotatedLoop: Boolean = false, trailingStore: Boolean = false, carriedIterations: Int = 0, factoryInk: Boolean = false) {
        val bytes: ByteArray
        val expected: ThemeInstructionMatches

        private data class ColorField(val classId: Int, val size: Int, val offset: Int, val count: Int, val role: ThemeColorRole, val floatingField: Int? = null)

        init {
            val instructions = mutableListOf<Int>()
            val labels = mutableMapOf<String, Int>()
            val calls = mutableListOf<Triple<Int, Int, String>>()
            val roles = mutableMapOf<Int, ThemeColorRole>()
            fun address() = instructions.size * Int.SIZE_BYTES
            fun emit(instruction: Int): Int {
                val site = address()
                instructions += instruction
                return site
            }
            fun label(name: String) {
                labels[name] = address()
            }
            fun call(name: String): Int {
                calls += Triple(address(), 0x94000000.toInt(), name)
                emit(0)
                return address()
            }
            fun allocate(classId: Int, register: Int) {
                call("class-$classId")
                emit(0xaa0003e0.toInt() or register)
            }
            fun store(base: Int, offset: Int): Int = emit(0xb8000002.toInt() or (base shl 5) or (offset shl 12))
            val constructor = address()
            emit(0xa9bf79fd.toInt())
            call("allocator")
            val components = address()
            listOf(24, 16, 8).forEachIndexed { index, shift -> emit(0x9340fc00.toInt() or (shift shl 16) or (index + 1)) }
            emit(0xd65f03c0.toInt())
            emit(0xa9bf79fd.toInt())
            val gradientLoop = address()
            repeat(4) {
                emit(0x1e624000)
                emit(0xbc017040.toInt())
            }
            val branch = address()
            emit(0x14000000 or (((gradientLoop - branch) / Int.SIZE_BYTES) and 0x03ffffff))
            val gradientReturn = address()
            emit(0xaa0203e0.toInt())
            emit(0xaa1d03ef.toInt())
            emit(0xa8c179fd.toInt())
            emit(0xd65f03c0.toInt())
            emit(0xa9bf79fd.toInt())
            var containerStore = -1
            for (offset in listOf(11, 15, 19, 23, 27, 39, 43, 47, 51)) {
                val site = store(20, offset)
                if (offset == 23) containerStore = site
            }
            emit(0xd65f03c0.toInt())
            val fields = listOf(
                ColorField(291, 112, 11, 50, ThemeColorRole.INK),
                ColorField(292, 48, 7, 50, ThemeColorRole.SURFACE),
                ColorField(293, 64, 35, 10, ThemeColorRole.INK),
                ColorField(294, 48, 31, 10, ThemeColorRole.LINE),
                ColorField(295, 80, 11, 10, ThemeColorRole.PAGE),
                ColorField(296, 112, 67, 2, ThemeColorRole.SURFACE),
                ColorField(297, 240, 179, 3, ThemeColorRole.SURFACE),
                ColorField(298, 48, 11, 3, ThemeColorRole.SURFACE),
                ColorField(298, 48, 23, 3, ThemeColorRole.INK),
                ColorField(300, 32, 7, 1, ThemeColorRole.LINE, 11),
                ColorField(301, 32, 7, 1, ThemeColorRole.INK),
                ColorField(302, 64, 7, 1, ThemeColorRole.PRESERVE, 15),
            )
            for ((classId, _, offset, count, role, floatingField) in fields) {
                repeat(count) { index ->
                    emit(0xa9bf79fd.toInt())
                    val skipStore = classId == (if (factoryInk) 292 else 291) && index == 0
                    val rotate = rotatedLoop && skipStore
                    val entryBranch = if (rotate) emit(0) else -1
                    val body = address()
                    allocate(classId, 20)
                    floatingField?.let { emit(0xbc000000.toInt() or (20 shl 5) or (it shl 12)) }
                    if (skipStore) allocate(292, 21)
                    emit(0xf81f83b4.toInt())
                    if (skipStore) emit(0xf81e83b5.toInt())
                    val carry = if (skipStore) carriedIterations else 0
                    val loopHead = address()
                    if (carry > 0) {
                        emit(0xf85d83a2.toInt())
                        store(20, offset)
                        repeat(carry - 1) { slot ->
                            emit(0xf84003a2.toInt() or (((-48 - slot * 8) and 0x1ff) shl 12))
                            emit(0xf80003a2.toInt() or (((-40 - slot * 8) and 0x1ff) shl 12))
                        }
                    }
                    if (skipStore && factoryDepth > 0) {
                        roles[call("factory-0")] = role
                        emit(0xaa0003e2.toInt())
                    } else {
                        allocate(299, 19)
                        emit(0xf81f03b3.toInt())
                        emit(0xaa1303e1.toInt())
                        roles[call("constructor")] = role
                        emit(0xf85f03a2.toInt())
                    }
                    emit(0xf85f83b4.toInt())
                    if (skipStore) emit(0xf85e83b5.toInt())
                    if (skipStore) {
                        emit(0x14000002)
                        store(21, 7)
                    }
                    if (carry > 0) {
                        emit(0xf80003a2.toInt() or (((-40 - (carry - 1) * 8) and 0x1ff) shl 12))
                        emit(0xb5000009.toInt() or ((((loopHead - address()) / Int.SIZE_BYTES) and 0x7ffff) shl 5))
                    } else {
                        store(20, offset)
                    }
                    if (rotate) {
                        instructions[entryBranch / Int.SIZE_BYTES] = 0x14000000 or ((address() - entryBranch) / Int.SIZE_BYTES)
                        emit(0xb5000009.toInt() or ((((body - address()) / Int.SIZE_BYTES) and 0x7ffff) shl 5))
                    }
                    if (skipStore && factoryDepth > 0) emit(0xaa1f03e0.toInt())
                    emit(0xd65f03c0.toInt())
                    if (trailingStore && skipStore) store(21, 7)
                }
            }
            repeat(factoryDepth) { index ->
                label("factory-$index")
                emit(0xa9bf79fd.toInt())
                if (index < factoryDepth - 1) {
                    roles[call("factory-${index + 1}")] = ThemeColorRole.FACTORY
                } else {
                    if (factoryInk) {
                        allocate(298, 20)
                        emit(0xf81f03b4.toInt())
                    }
                    allocate(299, 1)
                    emit(0xf81f83a1.toInt())
                    roles[call("constructor")] = ThemeColorRole.FACTORY
                    emit(0xf85f83a0.toInt())
                    if (factoryInk) {
                        emit(0xf85f03b4.toInt())
                        emit(0xaa0003e2.toInt())
                        store(20, 23)
                    }
                }
                emit(0xd65f03c0.toInt())
            }
            for ((classId, size) in fields.map { it.classId to it.size }.distinct() + (299 to 48)) {
                label("class-$classId")
                val header = (classId shl 12) or (size / 16 shl 8)
                emit(0xd2800000.toInt() or ((header and 0xffff) shl 5))
                emit(0xf2a00000.toInt() or ((header ushr 16) shl 5))
                val site = address()
                calls += Triple(site, 0x14000000, "allocator")
                emit(0)
            }
            repeat(4) { emit(0xd503201f.toInt()) }
            labels["constructor"] = constructor
            labels["allocator"] = address() + 64
            bytes = ByteArray(address())
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            instructions.forEachIndexed { index, word -> buffer.putInt(index * Int.SIZE_BYTES, word) }
            for ((site, opcode, name) in calls) {
                buffer.putInt(site, opcode or (((labels.getValue(name) - site) / Int.SIZE_BYTES) and 0x03ffffff))
            }
            expected = ThemeInstructionMatches(constructor, components, gradientReturn, containerStore, roles)
        }
    }
}
