/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.settings

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.readInt
import app.morphe.patches.shared.writeInt
import app.morphe.patches.shared.readShort
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val SETTINGS_CALLBACK_WINDOW = 1024
private const val POOL_TYPE_ARGUMENTS_BEFORE_METHOD = 8
private const val CHANNEL_BEFORE_METHOD = 8
private const val OBJECT_REGISTER = 16
private const val TYPE_ARGUMENTS_REGISTER = 17
private const val ARGUMENT_DESCRIPTOR_REGISTER = 4
private const val INSTRUCTION_BYTES = 4
private const val BRANCH_IMMEDIATE_MASK = 0x03ffffff
private const val BRANCH_WITH_LINK = 0x94000000.toInt()
private const val NOP = 0xd503201f.toInt()
private const val ELF_PROGRAM_HEADERS = 32
private const val ELF_PROGRAM_HEADER_SIZE = 54
private const val ELF_PROGRAM_HEADER_COUNT = 56
private const val ELF_SEGMENT_FLAGS = 4
private const val ELF_SEGMENT_OFFSET = 8
private const val ELF_SEGMENT_FILE_SIZE = 32
private const val ELF_SEGMENT_MEMORY_SIZE = 40
private const val ELF_LOAD = 1
private const val ELF_EXECUTE = 1
private const val ELF_SECTIONS = 40
private const val ELF_SECTION_HEADER_SIZE = 58
private const val ELF_SECTION_HEADER_COUNT = 60
private const val ELF_SECTION_OFFSET = 24
private const val ELF_SECTION_SIZE = 32
private const val TITLE_FALLBACK_BRANCH = 68
private val TITLE_ALLOCATION =
    ("402b40f9492f40f90a8000b1c20100545f0109eb820100544a2b00f9904b9cd2b000a0f2100000f9100480d2100400f9" +
        "d0000058f1000058104401a900040091c0035fd60000001468787265626f726e2070617463686573").hexToByteArray()

internal fun ByteArray.addPatchesMenu() {
    val settings = nativeMethods.single("qaa_title", "contact_us", "app_settings") { "language_english" !in it.stringLoads.values }
    val contactLabel = settings.loads("contact_us").single()
    val titleCall = nativeMethods.firstCallAfter(contactLabel, settings.end)
    val contact = nativeMethods.matching("/contact").singleOrNull {
        it.start >= settings.end && it.start < settings.end + SETTINGS_CALLBACK_WINDOW
    } ?: throw PatchException("Expected one Contact Us callback beside Profile settings")
    val route = contact.loads("/contact").single()
    val menuInvoke = nativeMethods.firstCallAfter(route, contact.end)
    val invokeOffset = menuInvoke - contact.start
    val review = nativeMethods.single("requestReview")
    val reviewMethod = review.loads("requestReview").single()
    val reviewCall = nativeMethods.firstCallAfter(reviewMethod, review.end)
    val logan = nativeMethods.single("type", "log")
    val logMethod = logan.loads("log").last()
    val invoke = nativeMethods.firstCallAfter(logMethod, logan.end)
    val invokeTarget = nativeMethods.callTarget(invoke)
    if (invokeTarget != nativeMethods.callTarget(reviewCall)) {
        throw PatchException("Expected shared Flutter method-channel invocation")
    }
    val menu = mutableListOf(0xa9bf79fd.toInt(), 0xaa0f03fd.toInt(), 0xd10061ef.toInt())
    menu += nativeMethods.poolLoad(logMethod - CHANNEL_BEFORE_METHOD, OBJECT_REGISTER)
    menu += nativeMethods.poolLoad(nativeMethods.poolLoadStart(reviewMethod) - POOL_TYPE_ARGUMENTS_BEFORE_METHOD,
        TYPE_ARGUMENTS_REGISTER)
    menu += 0xa900c5f0.toInt()
    menu += nativeMethods.poolLoad(route, OBJECT_REGISTER)
    menu += 0xf90001f0.toInt()
    menu += nativeMethods.poolLoad(reviewCall - INSTRUCTION_BYTES, ARGUMENT_DESCRIPTOR_REGISTER)
    if (menu.size * INSTRUCTION_BYTES > invokeOffset) throw PatchException("Patches menu exceeds support callback frame")
    while (menu.size * INSTRUCTION_BYTES < invokeOffset) menu += NOP
    menu += BRANCH_WITH_LINK or (((invokeTarget - menuInvoke) shr 2) and BRANCH_IMMEDIATE_MASK)
    menu += listOf(0xaa1603e0.toInt(), 0xaa1d03ef.toInt(), 0xa8c179fd.toInt(), 0xd65f03c0.toInt())
    if (contact.start + menu.size * INSTRUCTION_BYTES > contact.end) {
        throw PatchException("Patches menu exceeds support callback boundary")
    }
    menu.forEachIndexed { slot, instruction -> writeInt(contact.start + slot * INSTRUCTION_BYTES, instruction) }
    addTitleAllocation(titleCall)
}

private fun ByteArray.addTitleAllocation(call: Int) {
    if (readInt(call) and 0xfc000000.toInt() != BRANCH_WITH_LINK) {
        throw PatchException("Expected patches row translation call")
    }
    val originalTarget = call + ((readInt(call) shl 6) shr 4)
    val start = appendNativeCode(TITLE_ALLOCATION)
    writeInt(start + TITLE_FALLBACK_BRANCH,
        0x14000000 or (((originalTarget - start - TITLE_FALLBACK_BRANCH) shr 2) and BRANCH_IMMEDIATE_MASK))
    writeInt(call, BRANCH_WITH_LINK or (((start - call) shr 2) and BRANCH_IMMEDIATE_MASK))
}

internal fun ByteArray.appendNativeCode(code: ByteArray): Int {
    val elf = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)
    val headers = elf.getLong(ELF_PROGRAM_HEADERS).toInt()
    val headerSize = readShort(ELF_PROGRAM_HEADER_SIZE)
    val segments = (0 until readShort(ELF_PROGRAM_HEADER_COUNT)).map { headers + it * headerSize }
        .filter { readInt(it) == ELF_LOAD }
    val executable = segments.single { readInt(it + ELF_SEGMENT_FLAGS) and ELF_EXECUTE != 0 }
    val segmentOffset = elf.getLong(executable + ELF_SEGMENT_OFFSET).toInt()
    val originalSize = elf.getLong(executable + ELF_SEGMENT_FILE_SIZE)
    if (elf.getLong(executable + ELF_SEGMENT_MEMORY_SIZE) != originalSize) {
        throw PatchException("Unexpected Dart executable segment memory size")
    }
    val segmentEnd = segmentOffset + originalSize.toInt()
    val nextSegment = segments.map { elf.getLong(it + ELF_SEGMENT_OFFSET).toInt() }
        .filter { it >= segmentEnd }.minOrNull()
        ?: throw PatchException("Could not find Dart executable segment boundary")
    val start = (segmentEnd + INSTRUCTION_BYTES - 1) / INSTRUCTION_BYTES * INSTRUCTION_BYTES
    val end = start + code.size
    if (end > nextSegment || (segmentEnd until end).any { this[it] != 0.toByte() }) {
        throw PatchException("Insufficient executable segment padding for patches title")
    }
    code.copyInto(this, start)
    elf.putLong(executable + ELF_SEGMENT_FILE_SIZE, (end - segmentOffset).toLong())
    elf.putLong(executable + ELF_SEGMENT_MEMORY_SIZE, (end - segmentOffset).toLong())
    val sections = elf.getLong(ELF_SECTIONS).toInt()
    val sectionSize = readShort(ELF_SECTION_HEADER_SIZE)
    (0 until readShort(ELF_SECTION_HEADER_COUNT)).map { sections + it * sectionSize }.filter {
        elf.getLong(it + ELF_SECTION_OFFSET) + elf.getLong(it + ELF_SECTION_SIZE) == segmentEnd.toLong()
    }.forEach {
        elf.putLong(it + ELF_SECTION_SIZE, end - elf.getLong(it + ELF_SECTION_OFFSET))
    }
    return start
}
