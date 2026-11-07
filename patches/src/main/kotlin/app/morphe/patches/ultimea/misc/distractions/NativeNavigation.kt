/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.distractions

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.readInt
import app.morphe.patches.shared.writeInt
import app.morphe.patches.ultimea.misc.settings.appendNativeCode
import app.morphe.patches.ultimea.misc.settings.nativeMethods

private const val INSTRUCTION_BYTES = 4
private const val BRANCH_WITH_LINK = 0x94000000.toInt()
private const val BRANCH = 0x14000000
private const val BRANCH_MASK = 0x03ffffff
private const val ARRAY_LENGTH_BYTES = 64
private const val BODY_ARRAY_AFTER_BOTTOM_BAR_WINDOW = 4096
private const val CALLBACK_INDEX_ARGUMENT = 60
private const val CALLBACK_PROFILE_COMPARISON = 144
private const val CALLBACK_COMMUNITY_BRANCH = 184
private const val CALLBACK_HOME_COMPARISON = 280
private const val CONTROLLER_INITIAL_INDEX_BEFORE_LENGTH = 16
private const val REGISTER_MASK = 31
private const val INSTRUCTION_SHIFT = 2
private const val CALLBACK_BRANCH_SIGN_SHIFT = 8
private const val CALLBACK_BRANCH_SCALE_SHIFT = 11
private const val LOAD_TAB_CONTROLLER = 0xb8423002.toInt()
private const val PROFILE_INDEX_COMPARISON = 0xf100085f.toInt()
private const val HOME_INDEX_COMPARISON = 0xf100041f.toInt()
private const val INDEX_FIELD_LOAD_MASK = 0xffe00fff.toInt()
private const val INDEX_FIELD_LOAD = 0xb8400001.toInt()
private val CALLBACK_INDEX = "a20b40f942080091c0035fd6".hexToByteArray()

internal fun ByteArray.removeCommunityNavigation(): List<Pair<Int, ByteArray>> {
    val original = copyOf()
    val bottom = nativeMethods.single("assets/images/home_icon.svg", "assets/images/mine_icon.svg")
    val body = nativeMethods.all().single { function ->
        nativeMethods.calls(function).any { nativeMethods.callTarget(it) == bottom.start }
    }
    val controllerGetter = nativeMethods.calls(bottom).filter {
        readInt(it + INSTRUCTION_BYTES) == LOAD_TAB_CONTROLLER
    }.singleOrNull()?.let(nativeMethods::callTarget)
        ?: throw PatchException("Expected bottom-bar controller lookup")
    val callback = nativeMethods.all().singleOrNull { function ->
        nativeMethods.calls(function).any { nativeMethods.callTarget(it) == controllerGetter } &&
            (function.start until function.end step INSTRUCTION_BYTES).any { readInt(it) == PROFILE_INDEX_COMPARISON } &&
            (function.start until function.end step INSTRUCTION_BYTES).any { readInt(it) == HOME_INDEX_COMPARISON }
    } ?: throw PatchException("Expected one three-tab navigation callback")
    fun replace(address: Int, expected: Int, value: Int) {
        if (readInt(address) != expected) throw PatchException("Unexpected soundbar navigation instruction")
        writeInt(address, value)
    }
    fun branch(address: Int, target: Int, expected: Int) =
        replace(address, expected, BRANCH_WITH_LINK or (((target - address) shr INSTRUCTION_SHIFT) and BRANCH_MASK))
    val snapshot = nativeMethods.snapshot
    val arrayLoad = snapshot.widgetArrayPoolOffsets().flatMap(nativeMethods::references).single {
        it >= body.start && it < bottom.end + BODY_ARRAY_AFTER_BOTTOM_BAR_WINDOW
    }
    val stack = nativeMethods.containing(arrayLoad)
    val controllerLookup = nativeMethods.calls(callback).first { nativeMethods.callTarget(it) == controllerGetter }
    val indexFieldLoad = readInt(controllerLookup + INSTRUCTION_BYTES)
    if (indexFieldLoad and INDEX_FIELD_LOAD_MASK != INDEX_FIELD_LOAD) {
        throw PatchException("Expected navigation callback index field")
    }
    val indexRead = (stack.start until arrayLoad step INSTRUCTION_BYTES).first { readInt(it) == indexFieldLoad }
    val indexCall = nativeMethods.firstCallAfter(indexRead, stack.end)
    val indexStore = (indexCall + INSTRUCTION_BYTES until indexCall + 6 * INSTRUCTION_BYTES step INSTRUCTION_BYTES).first {
        readInt(it) and 0xffe00fe0.toInt() == 0xf80003a0.toInt()
    }
    val indexRegister = readInt(indexStore) and REGISTER_MASK
    val indexCode = listOf(0xf1000800.toInt() or (indexRegister shl 5) or indexRegister,
        0x9a9fa000.toInt() or (indexRegister shl 5) or indexRegister, readInt(indexStore), 0xd65f03c0.toInt())
    val bodyHelper = appendNativeCode(indexCode.flatMap { word ->
        List(INSTRUCTION_BYTES) { (word ushr (it * Byte.SIZE_BITS)).toByte() }
    }.toByteArray())
    val callbackHelper = appendNativeCode(CALLBACK_INDEX)
    branch(indexStore, bodyHelper, readInt(indexStore))
    snapshot.removeFirstArrayElement(nativeMethods.poolOffset(arrayLoad)).forEach { (offset, bytes) -> bytes.copyInto(this, offset) }
    val children = (bottom.start until bottom.end step INSTRUCTION_BYTES).single {
        readInt(it) == 0xaa0003e2.toInt() && readInt(it + INSTRUCTION_BYTES) == 0xf85d03a0.toInt() &&
            readInt(it + ARRAY_LENGTH_BYTES - INSTRUCTION_BYTES) == 0xd28000c0.toInt()
    }
    replace(children + INSTRUCTION_BYTES, 0xf85d03a0.toInt(), 0xf85c03a0.toInt())
    replace(children + 4 * INSTRUCTION_BYTES, 0xf85c03a0.toInt(), 0xf85d83a0.toInt())
    replace(children + ARRAY_LENGTH_BYTES - INSTRUCTION_BYTES, 0xd28000c0.toInt(), 0xd2800080.toInt())
    branch(callback.start + CALLBACK_INDEX_ARGUMENT, callbackHelper, 0xf9400ba2.toInt())
    replace(callback.start + CALLBACK_PROFILE_COMPARISON, PROFILE_INDEX_COMPARISON, 0xf100045f.toInt())
    val communityBranch = callback.start + CALLBACK_COMMUNITY_BRANCH
    val oldBranch = readInt(communityBranch)
    if (oldBranch and 0xff00001f.toInt() != 0xb5000000.toInt()) {
        throw PatchException("Expected community tab callback branch")
    }
    val target = communityBranch + ((oldBranch shl CALLBACK_BRANCH_SIGN_SHIFT) shr CALLBACK_BRANCH_SCALE_SHIFT)
    writeInt(communityBranch, BRANCH or (((target - communityBranch) shr INSTRUCTION_SHIFT) and BRANCH_MASK))
    replace(callback.start + CALLBACK_HOME_COMPARISON, HOME_INDEX_COMPARISON, 0xf100001f.toInt())
    val bottomReturn = (bottom.start until bottom.end step INSTRUCTION_BYTES).first { readInt(it) == 0xd65f03c0.toInt() }
    val field = nativeMethods.poolOffset(nativeMethods.poolLoads(bottom).first { it > bottomReturn })
    val references = nativeMethods.references(field).map(nativeMethods::containing).distinctBy { it.start }
    val homeCalls = references.flatMap { function ->
        val invocations = nativeMethods.calls(function)
        invocations.filterIndexed { index, call ->
            readInt(call - 2 * INSTRUCTION_BYTES) == 0xd2800022.toInt() &&
                (invocations.getOrElse(index - 1) { function.start } + INSTRUCTION_BYTES until call step INSTRUCTION_BYTES).any {
                    readInt(it) == 0xb8423001.toInt()
                }
        }
    }.groupBy(nativeMethods::callTarget)
    val calls = homeCalls.values.singleOrNull()
        ?: throw PatchException("Expected shared soundbar Home navigation calls")
    calls.forEach { replace(it - 2 * INSTRUCTION_BYTES, 0xd2800022.toInt(), 0xd2800002.toInt()) }
    val disconnected = references.single { "/login" in it.stringLoads.values && "/tabbar" in it.stringLoads.values }
    val selectedHome = (disconnected.start until disconnected.end step INSTRUCTION_BYTES).single {
        readInt(it) == 0xf100041f.toInt()
    }
    replace(selectedHome, 0xf100041f.toInt(), 0xf100001f.toInt())
    val lifecycle = nativeMethods.single(" onInit() called || currentRoute=")
    val initializers = nativeMethods.all().filter { function ->
        nativeMethods.calls(function).any { nativeMethods.callTarget(it) == lifecycle.start } &&
        (function.start until function.end step INSTRUCTION_BYTES).any { readInt(it) == 0xd2800062.toInt() } &&
            (function.start until function.end step INSTRUCTION_BYTES).any { readInt(it) == 0xb8023020.toInt() }
    }
    val initializer = initializers.singleOrNull { function ->
        (function.start until function.end step INSTRUCTION_BYTES).any { readInt(it) == 0xd2800050.toInt() }
    } ?: throw PatchException("Expected one three-tab soundbar controller initializer")
    val length = (initializer.start until initializer.end step INSTRUCTION_BYTES).single { readInt(it) == 0xd2800062.toInt() }
    replace(length, 0xd2800062.toInt(), 0xd2800042.toInt())
    replace(length - CONTROLLER_INITIAL_INDEX_BEFORE_LENGTH, 0xd2800050.toInt(), 0xd2800010.toInt())
    val changed = indices.filter { this[it] != original[it] }
    return changed.fold(mutableListOf<IntRange>()) { ranges, offset ->
        if (ranges.isNotEmpty() && ranges.last().last + 1 == offset) {
            ranges[ranges.lastIndex] = ranges.last().first..offset
        } else ranges += offset..offset
        ranges
    }.map { it.first to copyOfRange(it.first, it.last + 1) }
}
