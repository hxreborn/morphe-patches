/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.coddy.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.indexOfMasked
import app.morphe.patches.shared.readInt

private val ARMEABI_V7A_SUBSCRIPTION_FIELD_STORES =
    ("d80000eb08101be5031080e50c101be5071080e510101be50b1080e514101be50f1080e518101be5131080e5" +
        "1c101be5171080e520101be51b1080e524101be51f1080e528101be5231080e52c101be5271080e504101be5")
        .hexToByteArray()

private val ARMEABI_V7A_SUBSCRIPTION_FIELD_STORES_MASK =
    ("000000ff" + "ff".repeat(ARMEABI_V7A_SUBSCRIPTION_FIELD_STORES.size - 4)).hexToByteArray()

private val ARMEABI_V7A_EMPTY_SUBSCRIPTION_SKIP =
    "0210a0e100e187e016e98ee2a8e99ee53eff2fe10010a0e148009ae5000051e10800001a84301be5ab4093e5".hexToByteArray()

private val ARMEABI_V7A_EMPTY_SUBSCRIPTION_SKIP_MASK =
    "ffffffffffffffff00f0ffff00f0ffffffffffffffffffffffffffffffffffff000000ffffffffffffffffff".hexToByteArray()

private val ARMEABI_V7A_INITIAL_SUBSCRIPTION =
    "e30df5eb0dea85e2b3ec9ee501408de8834795e54ffbffeb04101be5ab0081e5".hexToByteArray()

private val ARMEABI_V7A_INITIAL_SUBSCRIPTION_MASK =
    "000000ff00f0ffff00f0ffffffffffff00f0ffff000000ffffffffffffffffff".hexToByteArray()

private val ARMEABI_V7A_EMPTY_SUBSCRIPTION_FACTORY =
    "00482de900b08de2000000eb00d04be20088bde8".hexToByteArray()

private val ARMEABI_V7A_EMPTY_SUBSCRIPTION_FACTORY_MASK =
    "ffffffffffffffff000000ffffffffffffffffff".hexToByteArray()

private val ARMEABI_V7A_GUEST_PREMIUM_ACCOUNT_PROMPT =
    "ab1092e51ac115eb0f1090e548309ae5030051e11100001a".hexToByteArray()

private val ARMEABI_V7A_GUEST_PREMIUM_ACCOUNT_PROMPT_MASK =
    "ffffffff000000ffffffffffffffffffffffffff000000ff".hexToByteArray()

private val ARMEABI_V7A_IS_PRO =
    ("00482de900b08de208d04de224c09ae50c005de134d6269b0f0091e540c09ae50c0050e10e00000a080010e3" +
        "0c00001a1f0091e5011010e55116f3e704008de50eea85e25bef9ee500e08de50100a0e100e197e73eff2fe1" +
        "081020e2").hexToByteArray()

private val ARMEABI_V7A_IS_PLUS =
    ("00482de900b08de208d04de224c09ae50c005de14ed6269b0f0091e540c09ae50c0050e10c00000a080010e3" +
        "0a00001a1f0091e5011010e55116f3e704008de50eea85e25bef9ee500e08de50100a0e100e197e73eff2fe1" +
        "000000ea").hexToByteArray()

private val ARMEABI_V7A_PLAN_GETTER_MASK =
    ("ffffffffffffffffffffffffffffffffffffffff000000ffffffffffffffffffffffffff000000ffffffffff" +
        "000000ffffffffffffffffffffffffffffffffff00f0ffff00f0ffffffffffffffffffffffffffffffffffff" +
        "ffffffff").hexToByteArray()

private val ARMEABI_V7A_IS_PLUS_MASK = ARMEABI_V7A_PLAN_GETTER_MASK.copyOf().also { mask ->
    "000000ff".hexToByteArray().copyInto(mask, mask.size - 4)
}

private val ARMEABI_V7A_AI_TUTOR_LIMIT_CHECK =
    "00482de900b08de214d04de20100a0e104100be524c09ae50c005de11e6b159bab1090e5".hexToByteArray()

private val ARMEABI_V7A_AI_TUTOR_LIMIT_CHECK_MASK =
    "ffffffffffffffffffffffffffffffffffffffffffffffffffffffff000000ffffffffff".hexToByteArray()

private val ARMEABI_V7A_FREE_PROJECT_LIMIT =
    ("00482de900b08de204d04de20100a0e104100be524c09ae50c005de12c87179b0010a0e10e0000eb080010e3" +
        "0900000a04001be51b1090e54cb910eb").hexToByteArray()

private val ARMEABI_V7A_FREE_PROJECT_LIMIT_MASK =
    ("ffffffffffffffffffffffffffffffffffffffffffffffffffffffff000000ffffffffff000000ffffffffff" +
        "000000ffffffffffffffffff000000ff").hexToByteArray()

private const val PREMIUM_FIELD_LOAD_OFFSET = 28

private const val EMPTY_SUBSCRIPTION_BRANCH_OFFSET = 32

private const val ACCOUNT_PROMPT_BRANCH_OFFSET = 20

private const val FACTORY_ALLOCATION_OFFSET = 8

private const val LOAD_TRUE_INTO_R1 = 0xe59a1048.toInt()

private const val PREMIUM_FIELD_STORE = 0xe580100f.toInt()

private const val NOP = 0xe320f000.toInt()

private const val CONDITION_MASK = 0x0fffffff

private const val CONDITION_ALWAYS = 0xe0000000.toInt()

private const val BRANCH = 0xea000000.toInt()

private const val BRANCH_WITH_LINK = 0xeb000000.toInt()

private val RETURN_TRUE = intArrayOf(0xe59a0048.toInt(), 0xe12fff1e.toInt())

private val RETURN_FALSE = intArrayOf(0xe59a004c.toInt(), 0xe12fff1e.toInt())

internal fun ByteArray.unlockPremiumArmeabiV7a() {
    fun find(what: String, pattern: ByteArray, mask: ByteArray) =
        indexOfSingleMatch(ARMEABI_V7A_DART_AOT_LIBRARY, what, pattern, mask)

    fun branchTarget(at: Int): Int = at + 8 + ((readInt(at) shl 8) shr 6)

    fun branch(from: Int, to: Int, opcode: Int) = opcode or (((to - from - 8) shr 2) and 0xffffff)

    val fieldStores = find(
        "the subscription field stores",
        ARMEABI_V7A_SUBSCRIPTION_FIELD_STORES,
        ARMEABI_V7A_SUBSCRIPTION_FIELD_STORES_MASK,
    )
    writeInstructions(fieldStores + PREMIUM_FIELD_LOAD_OFFSET, LOAD_TRUE_INTO_R1)

    val emptySkip = find(
        "the empty subscription check",
        ARMEABI_V7A_EMPTY_SUBSCRIPTION_SKIP,
        ARMEABI_V7A_EMPTY_SUBSCRIPTION_SKIP_MASK,
    )
    writeInstructions(emptySkip + EMPTY_SUBSCRIPTION_BRANCH_OFFSET, NOP)

    val isPro = find("the Pro plan getter", ARMEABI_V7A_IS_PRO, ARMEABI_V7A_PLAN_GETTER_MASK)
    writeInstructions(isPro, *RETURN_FALSE)
    writeInstructions(find("the Plus plan getter", ARMEABI_V7A_IS_PLUS, ARMEABI_V7A_IS_PLUS_MASK), *RETURN_TRUE)
    writeInstructions(
        find("the free project limit", ARMEABI_V7A_FREE_PROJECT_LIMIT, ARMEABI_V7A_FREE_PROJECT_LIMIT_MASK),
        *RETURN_FALSE,
    )
    writeInstructions(
        find("the AI tutor limit", ARMEABI_V7A_AI_TUTOR_LIMIT_CHECK, ARMEABI_V7A_AI_TUTOR_LIMIT_CHECK_MASK),
        *RETURN_FALSE,
    )

    val accountPromptBranch = find(
        "the guest account prompt",
        ARMEABI_V7A_GUEST_PREMIUM_ACCOUNT_PROMPT,
        ARMEABI_V7A_GUEST_PREMIUM_ACCOUNT_PROMPT_MASK,
    ) + ACCOUNT_PROMPT_BRANCH_OFFSET
    writeInstructions(accountPromptBranch, (readInt(accountPromptBranch) and CONDITION_MASK) or CONDITION_ALWAYS)

    val emptyFactory = branchTarget(
        find("the initial subscription", ARMEABI_V7A_INITIAL_SUBSCRIPTION, ARMEABI_V7A_INITIAL_SUBSCRIPTION_MASK),
    )
    val emptyFactoryCode = copyOfRange(emptyFactory, emptyFactory + ARMEABI_V7A_EMPTY_SUBSCRIPTION_FACTORY.size)
    val isEmptyFactory = emptyFactoryCode.indexOfMasked(
        ARMEABI_V7A_EMPTY_SUBSCRIPTION_FACTORY,
        ARMEABI_V7A_EMPTY_SUBSCRIPTION_FACTORY_MASK,
    ) == 0
    if (!isEmptyFactory) {
        throw PatchException("The initial subscription is not built by the empty factory")
    }
    val allocationStub = branchTarget(emptyFactory + FACTORY_ALLOCATION_OFFSET)

    val premiumFactory = isPro + RETURN_FALSE.size * 4
    writeInstructions(
        premiumFactory,
        emptyFactoryCode.readInt(0),
        emptyFactoryCode.readInt(4),
        branch(premiumFactory + FACTORY_ALLOCATION_OFFSET, allocationStub, BRANCH_WITH_LINK),
        LOAD_TRUE_INTO_R1,
        PREMIUM_FIELD_STORE,
        emptyFactoryCode.readInt(12),
        emptyFactoryCode.readInt(16),
    )
    writeInstructions(emptyFactory, branch(emptyFactory, premiumFactory, BRANCH))
}
