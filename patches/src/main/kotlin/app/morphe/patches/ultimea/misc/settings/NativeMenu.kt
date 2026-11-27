/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.settings

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.misc.arm64.Arm64.NOP
import app.morphe.patches.shared.misc.dart.appendNativeCode
import app.morphe.patches.shared.readInt
import app.morphe.patches.shared.writeInt

private const val SETTINGS_CALLBACK_WINDOW = 1024
private const val POOL_TYPE_ARGUMENTS_BEFORE_METHOD = 8
private const val CHANNEL_BEFORE_METHOD = 8
private const val OBJECT_REGISTER = 16
private const val TYPE_ARGUMENTS_REGISTER = 17
private const val ARGUMENT_DESCRIPTOR_REGISTER = 4
private const val INSTRUCTION_BYTES = 4
private const val BRANCH_IMMEDIATE_MASK = 0x03ffffff
private const val BRANCH_WITH_LINK = 0x94000000.toInt()
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
    replaceWithPlatformMethod(contact, route, nativeMethods)
    addTitleAllocation(titleCall)
}

internal fun ByteArray.replaceWithPlatformMethod(callback: NativeFunction, route: Int, methods: NativeMethods) {
    val methodInvoke = methods.firstCallAfter(route, callback.end)
    val invokeOffset = methodInvoke - callback.start
    val review = methods.single("requestReview")
    val reviewMethod = review.loads("requestReview").single()
    val reviewCall = methods.firstCallAfter(reviewMethod, review.end)
    val logan = methods.single("type", "log")
    val logMethod = logan.loads("log").last()
    val invoke = methods.firstCallAfter(logMethod, logan.end)
    val invokeTarget = methods.callTarget(invoke)
    if (invokeTarget != methods.callTarget(reviewCall)) {
        throw PatchException("Expected shared Flutter method-channel invocation")
    }
    val replacement = mutableListOf(0xa9bf79fd.toInt(), 0xaa0f03fd.toInt(), 0xd10061ef.toInt())
    replacement += methods.poolLoad(logMethod - CHANNEL_BEFORE_METHOD, OBJECT_REGISTER)
    replacement += methods.poolLoad(methods.poolLoadStart(reviewMethod) - POOL_TYPE_ARGUMENTS_BEFORE_METHOD, TYPE_ARGUMENTS_REGISTER)
    replacement += 0xa900c5f0.toInt()
    replacement += methods.poolLoad(route, OBJECT_REGISTER)
    replacement += 0xf90001f0.toInt()
    replacement += methods.poolLoad(reviewCall - INSTRUCTION_BYTES, ARGUMENT_DESCRIPTOR_REGISTER)
    if (replacement.size * INSTRUCTION_BYTES > invokeOffset) throw PatchException("Platform method exceeds callback frame")
    while (replacement.size * INSTRUCTION_BYTES < invokeOffset) replacement += NOP
    replacement += BRANCH_WITH_LINK or (((invokeTarget - methodInvoke) shr 2) and BRANCH_IMMEDIATE_MASK)
    replacement += listOf(0xaa1603e0.toInt(), 0xaa1d03ef.toInt(), 0xa8c179fd.toInt(), 0xd65f03c0.toInt())
    if (callback.start + replacement.size * INSTRUCTION_BYTES > callback.end) {
        throw PatchException("Platform method exceeds callback boundary")
    }
    replacement.forEachIndexed { slot, instruction -> writeInt(callback.start + slot * INSTRUCTION_BYTES, instruction) }
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
