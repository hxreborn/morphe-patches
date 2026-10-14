/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.movieboxtv.misc.allinone

import app.morphe.patcher.patch.ApkArchitecture
import app.morphe.patcher.patch.PatchAvailability
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ijiami.editIjiamiPayload
import app.morphe.patches.shared.misc.ijiami.ijiamiPatch

private const val SERVICE_LOCATOR = "Lcom/transsion/tvdata/TvServiceLocator;"
private const val VIP_USER_INFO = "Lcom/transsion/tvdata/bean/BffGetVipUserInfoData;"
private const val SUBJECT_INFO = "Lcom/transsion/tvdata/bean/BffSubjectInfo;"
private const val VISITOR_LOGIN = "Lcom/transsion/tvdata/bean/BffVisitorLoginData;"
private const val REMOTE_VERSION_INFO = "Lcom/transsion/version/update/RemoteVersionInfo;"
private const val TEEN_MODE_STRATEGY = "Lcom/transsion/ad/strategy/v;"

private const val IS_VIP = "o0"
private const val SET_VIP_HEADERS = "S0"
private const val TEEN_MODE_KEY = "mmkv_key_teen_mode"

@Suppress("unused")
val allInOnePatch = resourcePatch(
    name = "All-In-One",
    description = "Unlocks premium playback and removes ads, the region block and forced updates. " +
        "Requires Android 10 or later.",
) {
    compatibleWith(AppCompatibilities.MOVIEBOX_TV)
    dependsOn(ijiamiPatch)

    availability { _, architecture ->
        when (architecture) {
            ApkArchitecture.ARM64_V8A, ApkArchitecture.ARMEABI_V7A, ApkArchitecture.UNIVERSAL ->
                PatchAvailability.ENABLED
            else -> PatchAvailability.UNAVAILABLE
        }
    }

    execute {
        editIjiamiPayload {
            method(SERVICE_LOCATOR, IS_VIP).returnEarly(true)
            method(VIP_USER_INFO, "isVip").returnBoxed(true)
            method(SERVICE_LOCATOR, SET_VIP_HEADERS).returnEarly()
            method(SUBJECT_INFO, "isVip").returnEarly(false)

            method(VISITOR_LOGIN, "getRegionBlock").returnBoxed(false)
            listOf("getForceUpdate", "getHasUpdate").forEach {
                method(REMOTE_VERSION_INFO, it).returnEarly(false)
            }

            methodWithString(TEEN_MODE_STRATEGY, TEEN_MODE_KEY).returnEarly(true)
        }
    }
}
