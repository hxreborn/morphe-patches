/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.gstarmc.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.gstarmc.misc.jiagu.editPayloadDexes
import app.morphe.patches.gstarmc.misc.jiagu.jiaguRuntimePatch
import app.morphe.patches.shared.compat.AppCompatibilities

private const val PREFERENCES = "Lcom/stone/app/sharedpreferences/AppSharedPreferences;"
private const val BASE_ACTIVITY = "Lcom/stone/app/ui/base/BaseActivity;"
private const val ADS = "Lcom/stone/ad/InternalAdsManager;"

private const val ASYNC_CHECK = "checkFunctionPointAvailable"

private val TRUE_METHODS = mapOf(
    PREFERENCES to listOf(
        "checkFunctionPointUseable",
        "checkUserFunctionPoint_VipCode",
        "isUserVip_AD",
        "isUserVip_High",
        "isUserVip_Super",
        "isUserVip_Company",
    ),
    BASE_ACTIVITY to listOf(ASYNC_CHECK),
)

private val FALSE_METHODS = mapOf(
    PREFERENCES to listOf("checkAdSettingStatus", "checkAdDataShow", "checkAdSettingValid"),
)

private val VOID_METHODS = mapOf(ADS to listOf("loadInternalAds"))

private const val CHECK_CALLBACK = "Lcom/stone/app/ui/base/BaseActivity\$FunctionPointCheckCallback;"

@Suppress("unused")
val unlockPremiumPatch = rawResourcePatch(
    name = "Unlock premium",
    description = "Unlocks the paid drawing, annotation and measurement tools, and removes ads.",
) {
    compatibleWith(AppCompatibilities.DWG_FASTVIEW)
    dependsOn(jiaguRuntimePatch)

    execute {
        val pending = (TRUE_METHODS.keys + FALSE_METHODS.keys + VOID_METHODS.keys).toMutableSet()

        editPayloadDexes { editor ->
            TRUE_METHODS.forEach { (owner, names) ->
                if (!editor.defines(owner)) return@forEach
                names.forEach { editor.forceReturn(owner, it, 1) }
                pending -= owner
            }
            FALSE_METHODS.forEach { (owner, names) ->
                if (!editor.defines(owner)) return@forEach
                names.forEach { editor.forceReturn(owner, it, 0) }
                pending -= owner
            }
            VOID_METHODS.forEach { (owner, names) ->
                if (!editor.defines(owner)) return@forEach
                names.forEach { editor.forceReturnVoid(owner, it) }
                pending -= owner
            }

            if (editor.declares(BASE_ACTIVITY, ASYNC_CHECK, "V")) {
                editor.invokeBooleanCallback(BASE_ACTIVITY, ASYNC_CHECK, CHECK_CALLBACK, "onResult", true)
            }
        }

        if (pending.isNotEmpty()) {
            throw PatchException("Missing class data in payload: ${pending.sorted().joinToString()}")
        }
    }
}
