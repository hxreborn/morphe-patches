/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.coddy.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.coddy.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.indexOfMasked
import app.morphe.patches.shared.misc.dart.DART_AOT_LIBRARY
import app.morphe.patches.shared.misc.requireArm
import app.morphe.patches.shared.readInt
import app.morphe.patches.shared.writeInt
import java.io.File

internal val SUBSCRIPTION_FIELD_STORES =
    ("d9000094a1035ff8017000b8a1835ef801b000b8a1035ef801f000b8a1835df8013001b8" +
        "a1035df8017001b8a1835cf801b001b8a1035cf801f001b8a1835bf8013002b8a1035bf8" +
        "017002b8a1835af801b002b8a1835ff8").hexToByteArray()

internal val SUBSCRIPTION_FIELD_STORES_MASK =
    ("000000fc" + "ff".repeat(SUBSCRIPTION_FIELD_STORES.size - 4)).hexToByteArray()

internal val EMPTY_SUBSCRIPTION_SKIP =
    "e10302aa1edc1d91be7a7ef8c0033fd6e0012037f1238092a06b71f803f04ab8".hexToByteArray()

internal val EMPTY_SUBSCRIPTION_SKIP_MASK =
    "ffffffffff03c0ffffffffffffffffff1f00f8ffffffffffffffffffffffffff".hexToByteArray()

internal val INITIAL_SUBSCRIPTION =
    "2dbaf59770674091101645f9e04100a9647f47f943fbff97a1835ff820f00ab8".hexToByteArray()

internal val INITIAL_SUBSCRIPTION_MASK =
    "000000fcff03c0ffff03c0ffffffffffff03c0ff000000fcffffffffffffffff".hexToByteArray()

internal val EMPTY_SUBSCRIPTION_FACTORY =
    "fd79bfa9fd030faae056f097ef031daafd79c1a8c0035fd6".hexToByteArray()

internal val EMPTY_SUBSCRIPTION_FACTORY_MASK =
    "ffffffffffffffff000000fcffffffffffffffffffffffff".hexToByteArray()

internal val GUEST_PREMIUM_ACCOUNT_PROMPT =
    "41f04ab821801c8bd4ac1494013041b821801c8bd08200913f00106ba1020054".hexToByteArray()

internal val GUEST_PREMIUM_ACCOUNT_PROMPT_MASK =
    "ffffffffffffffff000000fcffffffffffffffffffffffffffffffff1f0000ff".hexToByteArray()

internal val IS_PRO =
    ("fd79bfa9fd030faaef4100d1502740f9ff0110eb09030054203041b800801c8b1f00166b" +
        "00020054e0012037203042b800801c8b01f05ff8217c4cd3706f4091108a46f9f00100a9" +
        "e00301aafe0300aabe7a7ef8c0033fd601007cd2").hexToByteArray()

internal val IS_PLUS =
    ("fd79bfa9fd030faaef4100d1502740f9ff0110ebc9020054203041b800801c8b1f00166b" +
        "c0010054a0012037203042b800801c8b01f05ff8217c4cd3706f4091108a46f9f00100a9" +
        "e00301aafe0300aabe7a7ef8c0033fd602000014").hexToByteArray()

internal val PLAN_GETTER_MASK =
    ("ffffffffffffffffffffffffff03c0ffffffffff1f0000ffffffffffffffffffffffffff" +
        "1f0000ff1f00f8ffffffffffffffffffffffffffffffffffff03c0ffff03c0ffffffffff" +
        "ffffffffffffffffffffffffffffffffffffffff").hexToByteArray()

internal val IS_PLUS_MASK = PLAN_GETTER_MASK.copyOf().also { mask ->
    "000000fc".hexToByteArray().copyInto(mask, mask.size - 4)
}

internal val AI_TUTOR_LIMIT_CHECK =
    ("fd79bfa9fd030faaefa100d1e00301aaa1831ff8502740f9ff0110eb690f005401f04ab8" +
        "21801c8b6ce30d94e10300aa41b0f197a0002037c0c20091ef031daafd79c1a8c0035fd6" +
        "a0835ff801704ab821801c8b61e30d9401b047b821801c8ba1031ef8207040b800801c8b" +
        "a0831ef8").hexToByteArray()

internal val AI_TUTOR_LIMIT_CHECK_MASK =
    ("ffffffffffffffffff03c0ffffffffffffffffffffffffffffffffff1f0000ffffffffff" +
        "ffffffff000000fcffffffff000000fc1f00f8ffffffffffffffffffffffffffffffffff" +
        "ffffffffffffffffffffffff000000fcffffffffffffffffffffffffffffffffffffffff" +
        "ffffffff").hexToByteArray()

internal val FREE_PROJECT_LIMIT =
    ("fd79bfa9fd030faaef2100d1e00301aaa1831ff8502740f9ff0110eb89020054e10300aa" +
        "14000094a0012036a0835ff801f041b821801c8b5ad70f94").hexToByteArray()

internal val FREE_PROJECT_LIMIT_MASK =
    ("ffffffffffffffffffffffffffffffffffffffffff03c0ffffffffff1f0000ffffffffff" +
        "000000fc1f00f8ffffffffffffffffffffffffff000000fc").hexToByteArray()

private const val PREMIUM_FIELD_LOAD_OFFSET = 28

private const val EMPTY_SUBSCRIPTION_BRANCH_OFFSET = 16

private const val ACCOUNT_PROMPT_BRANCH_OFFSET = 28

private const val FACTORY_ALLOCATION_OFFSET = 8

private const val PREMIUM_FIELD_STORE = 0xb8013001.toInt()

private const val LOAD_TRUE_INTO_X1 = 0x910082c1.toInt()

private const val NOP = 0xd503201f.toInt()

private const val BRANCH = 0x14000000

private const val BRANCH_WITH_LINK = 0x94000000.toInt()

private val RETURN_TRUE = intArrayOf(0x910082c0.toInt(), 0xd65f03c0.toInt())

private val RETURN_FALSE = intArrayOf(0x9100c2c0.toInt(), 0xd65f03c0.toInt())

internal const val ARMEABI_V7A_DART_AOT_LIBRARY = "lib/armeabi-v7a/libapp.so"

internal fun ByteArray.indexOfSingleMatch(library: String, what: String, pattern: ByteArray, mask: ByteArray): Int {
    val index = indexOfMasked(pattern, mask)
    if (index < 0) throw PatchException("Could not find $what in $library")
    if (indexOfMasked(pattern, mask, index + 1) >= 0) {
        throw PatchException("Found $what more than once in $library")
    }
    return index
}

internal fun ByteArray.writeInstructions(at: Int, vararg instructions: Int) =
    instructions.forEachIndexed { slot, instruction -> writeInt(at + slot * 4, instruction) }

private fun ByteArray.unlockPremiumArm64() {
    fun find(what: String, pattern: ByteArray, mask: ByteArray) =
        indexOfSingleMatch(DART_AOT_LIBRARY, what, pattern, mask)

    fun branchTarget(at: Int): Int = at + ((readInt(at) shl 6) shr 4)

    fun branch(from: Int, to: Int, opcode: Int) = opcode or (((to - from) shr 2) and 0x3ffffff)

    val fieldStores = find("the subscription field stores", SUBSCRIPTION_FIELD_STORES, SUBSCRIPTION_FIELD_STORES_MASK)
    writeInstructions(fieldStores + PREMIUM_FIELD_LOAD_OFFSET, LOAD_TRUE_INTO_X1)

    val emptySkip = find("the empty subscription check", EMPTY_SUBSCRIPTION_SKIP, EMPTY_SUBSCRIPTION_SKIP_MASK)
    writeInstructions(emptySkip + EMPTY_SUBSCRIPTION_BRANCH_OFFSET, NOP)

    val isPro = find("the Pro plan getter", IS_PRO, PLAN_GETTER_MASK)
    writeInstructions(isPro, *RETURN_FALSE)
    writeInstructions(find("the Plus plan getter", IS_PLUS, IS_PLUS_MASK), *RETURN_TRUE)
    writeInstructions(find("the free project limit", FREE_PROJECT_LIMIT, FREE_PROJECT_LIMIT_MASK), *RETURN_FALSE)
    writeInstructions(find("the AI tutor limit", AI_TUTOR_LIMIT_CHECK, AI_TUTOR_LIMIT_CHECK_MASK), *RETURN_FALSE)

    val accountPromptBranch =
        find("the guest account prompt", GUEST_PREMIUM_ACCOUNT_PROMPT, GUEST_PREMIUM_ACCOUNT_PROMPT_MASK) +
            ACCOUNT_PROMPT_BRANCH_OFFSET
    val promptSkipTarget = accountPromptBranch + (((readInt(accountPromptBranch) shl 8) shr 13) shl 2)
    writeInstructions(accountPromptBranch, branch(accountPromptBranch, promptSkipTarget, BRANCH))

    val emptyFactory = branchTarget(find("the initial subscription", INITIAL_SUBSCRIPTION, INITIAL_SUBSCRIPTION_MASK))
    val emptyFactoryCode = copyOfRange(emptyFactory, emptyFactory + EMPTY_SUBSCRIPTION_FACTORY.size)
    if (emptyFactoryCode.indexOfMasked(EMPTY_SUBSCRIPTION_FACTORY, EMPTY_SUBSCRIPTION_FACTORY_MASK) != 0) {
        throw PatchException("The initial subscription is not built by the empty factory")
    }
    val allocationStub = branchTarget(emptyFactory + FACTORY_ALLOCATION_OFFSET)

    val premiumFactory = isPro + RETURN_FALSE.size * 4
    writeInstructions(
        premiumFactory,
        emptyFactoryCode.readInt(0),
        emptyFactoryCode.readInt(4),
        branch(premiumFactory + FACTORY_ALLOCATION_OFFSET, allocationStub, BRANCH_WITH_LINK),
        LOAD_TRUE_INTO_X1,
        PREMIUM_FIELD_STORE,
        emptyFactoryCode.readInt(12),
        emptyFactoryCode.readInt(16),
        emptyFactoryCode.readInt(20),
    )
    writeInstructions(emptyFactory, branch(emptyFactory, premiumFactory, BRANCH))
}

@Suppress("unused")
val unlockPremiumPatch = resourcePatch(
    name = "Unlock premium",
    description = "Unlocks Coddy Plus with unlimited energy and no ads, and removes the daily AI tutor " +
        "limit and the free project limit.",
) {
    compatibleWith(AppCompatibilities.CODDY)
    availability(requireArm)
    dependsOn(spoofSignaturePatch)

    execute {
        val unlocksByLibrary = mapOf(
            DART_AOT_LIBRARY to ByteArray::unlockPremiumArm64,
            ARMEABI_V7A_DART_AOT_LIBRARY to ByteArray::unlockPremiumArmeabiV7a,
        )
        val libraries = unlocksByLibrary.mapKeys { (path) -> get(path) }.filterKeys(File::exists)
        if (libraries.isEmpty()) {
            throw PatchException("Could not find $DART_AOT_LIBRARY or $ARMEABI_V7A_DART_AOT_LIBRARY")
        }

        libraries.forEach { (library, unlock) -> library.writeBytes(library.readBytes().apply(unlock)) }
    }
}
