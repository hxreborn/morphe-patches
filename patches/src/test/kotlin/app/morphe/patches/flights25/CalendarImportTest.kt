/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.flights25

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.flights25.misc.premium.CalendarImport
import app.morphe.patches.shared.writeInt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class CalendarImportTest {
    @Test
    fun `changes only import selection and remaining allowance`() {
        val fixture = Fixture()
        val expected = fixture.expected()

        fixture.removeLimit()

        assertContentEquals(expected, fixture.bytes)
    }

    @Test
    fun `resolves shifted and unshifted pool addresses`() {
        val poolBases = mapOf(0x91040370.toInt() to 0x100, 0x91401370.toInt() to 0x4000)
        for ((instruction, poolBase) in poolBases) {
            val fixture = Fixture()
            fixture.useDerivedPoolAddress(instruction, poolBase)
            val expected = fixture.expected()

            fixture.removeLimit()

            assertContentEquals(expected, fixture.bytes)
        }
    }

    @Test
    fun `rejects missing importer before changing bytes`() {
        val fixture = Fixture()
        fixture.pool.remove(COMPLETED_OFFSET)

        fixture.assertRejected("Expected one per-flight calendar importer")
    }

    @Test
    fun `rejects ambiguous importer before changing bytes`() {
        val fixture = Fixture()
        fixture.load(PREMIUM_ENTRY + 12, COMPLETED_OFFSET)
        fixture.load(PREMIUM_ENTRY + 16, FREE_OFFSET)
        fixture.load(PREMIUM_ENTRY + 20, REMAINING_OFFSET)

        fixture.assertRejected("Expected one per-flight calendar importer")
    }

    @Test
    fun `rejects ambiguous dispatcher before changing bytes`() {
        val fixture = Fixture()
        fixture.call(IMPORTER_CALL + INSTRUCTION_BYTES, IMPORTER_ENTRY)

        fixture.assertRejected("Expected one calendar import dispatcher")
    }

    @Test
    fun `rejects unexpected premium branch before changing bytes`() {
        val fixture = Fixture()
        fixture.bytes.writeInt(PREMIUM_BRANCH, NOP)

        fixture.assertRejected("Expected premium calendar import selection")
    }

    @Test
    fun `rejects unrelated premium getter before changing bytes`() {
        val fixture = Fixture()
        fixture.pool[PREMIUM_OFFSET] = "unrelated"

        fixture.assertRejected("Expected premium calendar import selection")
    }

    @Test
    fun `rejects missing allowance before changing bytes`() {
        val fixture = Fixture()
        fixture.bytes.writeInt(ALLOWANCE_LITERAL, NOP)

        fixture.assertRejected("Expected one calendar import allowance")
    }

    @Test
    fun `rejects ambiguous allowance before changing bytes`() {
        val fixture = Fixture()
        fixture.call(IMPORTER_ENTRY + 24, PREMIUM_ENTRY)
        fixture.bytes.writeInt(PREMIUM_ENTRY + 12, MOVE_ALLOWANCE)
        fixture.call(PREMIUM_ENTRY + 16, COUNTER_ENTRY)

        fixture.assertRejected("Expected one calendar import allowance")
    }

    private class Fixture {
        val bytes = ByteArray(CODE_END)
        val pool = mutableMapOf(
            COMPLETED_OFFSET to "calendar_single_import_completed",
            FREE_OFFSET to "is_free",
            REMAINING_OFFSET to "remaining",
            COUNTER_OFFSET to "singleFlightFreeImportCount",
            PREMIUM_OFFSET to "isPremiumEnabled",
        )

        init {
            for (entry in listOf(COUNTER_ENTRY, DISPATCHER_ENTRY, IMPORTER_ENTRY, ALLOWANCE_ENTRY, PREMIUM_ENTRY)) {
                bytes.writeInt(entry, 0xa9bf79fd.toInt())
                bytes.writeInt(entry + INSTRUCTION_BYTES, 0xaa0f03fd.toInt())
            }
            load(COUNTER_ENTRY + 8, COUNTER_OFFSET)
            load(PREMIUM_ENTRY + 8, PREMIUM_OFFSET)
            call(PREMIUM_CHECK, PREMIUM_ENTRY)
            bytes.writeInt(PREMIUM_BRANCH, 0x36200080)
            call(IMPORTER_CALL, IMPORTER_ENTRY)
            load(IMPORTER_ENTRY + 8, COMPLETED_OFFSET)
            load(IMPORTER_ENTRY + 12, FREE_OFFSET)
            load(IMPORTER_ENTRY + 16, REMAINING_OFFSET)
            call(IMPORTER_ENTRY + 20, ALLOWANCE_ENTRY)
            bytes.writeInt(ALLOWANCE_LITERAL, MOVE_ALLOWANCE)
            call(ALLOWANCE_ENTRY + 12, COUNTER_ENTRY)
        }

        fun removeLimit() = CalendarImport(bytes, COUNTER_ENTRY until CODE_END, pool).removeLimit()

        fun load(address: Int, offset: Int, base: Int = POOL_REGISTER) {
            bytes.writeInt(address, 0xf9400000.toInt() or ((offset / Long.SIZE_BYTES) shl 10) or (base shl 5))
        }

        fun call(address: Int, target: Int) {
            val displacement = (target - address) / INSTRUCTION_BYTES
            bytes.writeInt(address, 0x94000000.toInt() or (displacement and CALL_DISPLACEMENT_MASK))
        }

        fun useDerivedPoolAddress(instruction: Int, poolBase: Int) {
            bytes.writeInt(IMPORTER_ENTRY + 8, instruction)
            pool[poolBase + COMPLETED_OFFSET] = pool.remove(COMPLETED_OFFSET)!!
            load(IMPORTER_ENTRY + 12, COMPLETED_OFFSET, TEMPORARY_REGISTER)
            load(IMPORTER_ENTRY + 16, FREE_OFFSET)
            load(IMPORTER_ENTRY + 20, REMAINING_OFFSET)
            call(IMPORTER_ENTRY + 24, ALLOWANCE_ENTRY)
        }

        fun expected() = bytes.copyOf().apply {
            writeInt(PREMIUM_BRANCH, NOP)
            writeInt(ALLOWANCE_ENTRY, 0xd29fffe0.toInt())
            writeInt(ALLOWANCE_ENTRY + INSTRUCTION_BYTES, 0xf2afffe0.toInt())
            writeInt(ALLOWANCE_ENTRY + 2 * INSTRUCTION_BYTES, 0xd65f03c0.toInt())
        }

        fun assertRejected(message: String) {
            val original = bytes.copyOf()

            val error = assertFailsWith<PatchException> { removeLimit() }

            assertEquals(message, error.message)
            assertContentEquals(original, bytes)
        }
    }

    private companion object {
        const val INSTRUCTION_BYTES = 4
        const val COUNTER_ENTRY = 0x100
        const val DISPATCHER_ENTRY = 0x140
        const val IMPORTER_ENTRY = 0x180
        const val ALLOWANCE_ENTRY = 0x200
        const val PREMIUM_ENTRY = 0x240
        const val CODE_END = 0x280
        const val PREMIUM_CHECK = DISPATCHER_ENTRY + 8
        const val PREMIUM_BRANCH = DISPATCHER_ENTRY + 12
        const val IMPORTER_CALL = DISPATCHER_ENTRY + 24
        const val ALLOWANCE_LITERAL = ALLOWANCE_ENTRY + 8
        const val COMPLETED_OFFSET = 16
        const val FREE_OFFSET = 24
        const val REMAINING_OFFSET = 32
        const val COUNTER_OFFSET = 40
        const val PREMIUM_OFFSET = 48
        const val POOL_REGISTER = 27
        const val TEMPORARY_REGISTER = 16
        const val CALL_DISPLACEMENT_MASK = 0x03ffffff
        const val MOVE_ALLOWANCE = 0xd2800061.toInt()
        const val NOP = 0xd503201f.toInt()
    }
}
