/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.resprite.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.dart.DART_AOT_LIBRARY
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.replaceMasked

internal val PREMIUM_GETTER_HEAD =
    "20f043b800801c8ba0052037203044b800801c8ba0831ff8".hexToByteArray()

internal val PREMIUM_GETTER_HEAD_MASK = ByteArray(PREMIUM_GETTER_HEAD.size) { 0xff.toByte() }

private val RETURN_TRUE = "c0820091ef031daafd79c1a8c0035fd6".hexToByteArray()

internal val GET_PREMIUM_ITEM_APPEND =
    ("615f4091217c42f9013001b8a1835ff801b001b8c2c2009102f002b8027003b863c34091630c41f903b003b8" +
        "a4835df881b040b885f040b8a5801c8ba6b040b8257c4193a5031af8c17c4193").hexToByteArray()

internal val GET_PREMIUM_ITEM_APPEND_MASK = ByteArray(GET_PREMIUM_ITEM_APPEND.size) { 0xff.toByte() }

private const val GET_PREMIUM_ITEM_APPEND_OFFSET = 0x2c

private val SKIP_GET_PREMIUM_ITEM =
    "a4035ef8a2835df841b040b8257c4193a58319f846f040b8c6801c8b1a000014".hexToByteArray()

@Suppress("unused")
val unlockPremiumPatch = resourcePatch(
    name = "Unlock premium",
    description = "Unlocks every premium feature, including the export limit, symmetry, outline, filters, text, timelapse and custom brushes.",
) {
    compatibleWith(AppCompatibilities.RESPRITE)
    availability(requireArm64)

    val hideGetPremium by booleanOption(
        key = "hideGetPremium",
        default = true,
        title = "Hide Get Premium",
        description = "Hides the Get Premium entry from Help > Support.",
    )

    execute {
        val library = dartAotLibrary()

        val getterPatched = library.replaceMasked(
            PREMIUM_GETTER_HEAD,
            PREMIUM_GETTER_HEAD_MASK,
            mapOf(0 to RETURN_TRUE),
        )
        if (!getterPatched) throw PatchException("Could not find the premium state getter in $DART_AOT_LIBRARY")

        if (hideGetPremium == true) {
            val hidden = library.replaceMasked(
                GET_PREMIUM_ITEM_APPEND,
                GET_PREMIUM_ITEM_APPEND_MASK,
                mapOf(GET_PREMIUM_ITEM_APPEND_OFFSET to SKIP_GET_PREMIUM_ITEM),
            )
            if (!hidden) throw PatchException("Could not find the Get Premium support entry in $DART_AOT_LIBRARY")
        }
    }
}
