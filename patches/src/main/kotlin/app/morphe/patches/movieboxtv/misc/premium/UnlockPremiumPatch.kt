/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.movieboxtv.misc.premium

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ijiami.editIjiamiPayload
import app.morphe.patches.shared.misc.ijiami.ijiamiPatch
import app.morphe.patches.shared.misc.requireArm

private const val SERVICE_LOCATOR = "Lcom/transsion/tvdata/TvServiceLocator;"
private const val VIP_USER_INFO = "Lcom/transsion/tvdata/bean/BffGetVipUserInfoData;"
private const val SUBJECT_INFO = "Lcom/transsion/tvdata/bean/BffSubjectInfo;"
private const val VISITOR_LOGIN = "Lcom/transsion/tvdata/bean/BffVisitorLoginData;"

private const val IS_VIP = "o0"
private const val SET_VIP_HEADERS = "S0"

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks full-length playback and removes region blocks. Requires Android 10 or later.",
) {
    compatibleWith(AppCompatibilities.MOVIEBOX_TV)
    dependsOn(ijiamiPatch)
    availability(requireArm)

    execute {
        editIjiamiPayload {
            method(SERVICE_LOCATOR, IS_VIP).returnEarly(true)
            method(VIP_USER_INFO, "isVip").returnBoxed(true)
            method(SERVICE_LOCATOR, SET_VIP_HEADERS).returnEarly()
            method(SUBJECT_INFO, "isVip").returnEarly(false)
            method(VISITOR_LOGIN, "getRegionBlock").returnBoxed(false)
        }
    }
}
