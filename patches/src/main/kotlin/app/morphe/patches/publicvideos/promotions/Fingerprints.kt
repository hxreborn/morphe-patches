/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.publicvideos.promotions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

private val profileToolPrefStrings = listOf(
    "IS_AD_BOOKING_ENABLED",
    "SHOW_VIP_PACKAGE_BANNER",
    "VIP_PACKAGE_PROFILE_BANNER_TEXT",
)

internal object BottomBarCtaImageFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("bottomBarCtaClicked", "CTA Image"),
)

internal object ProfileHeaderToolsFingerprint : Fingerprint(
    strings = profileToolPrefStrings,
)

internal fun profileToolAddedFingerprint(toolType: String, toolField: String) = Fingerprint(
    strings = profileToolPrefStrings,
    filters = listOf(
        fieldAccess(definingClass = toolType, name = toolField, opcode = Opcode.SGET_OBJECT),
        methodCall(definingClass = "Lkotlin/collections/builders/ListBuilder;", name = "add"),
    ),
)
