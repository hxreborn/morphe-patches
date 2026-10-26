/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.netanalyzer.shared.ClickableRowFingerprint
import app.morphe.patches.netanalyzer.shared.HeaderRowFingerprint
import app.morphe.patches.netanalyzer.shared.disableTamperDetectionPatch
import app.morphe.patches.netanalyzer.shared.removeComposableCall
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64

private const val PRO_VERSION_HEADER = "Pro Version"
private val ABOUT_ROWS_TO_REMOVE = listOf("Full Feature List", "Configure Ad Consent")

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes the banner ads, the ad consent prompt and the Pro upgrade prompts.",
) {
    compatibleWith(AppCompatibilities.NETWORK_ANALYZER)
    availability(requireArm64)
    dependsOn(disableTamperDetectionPatch)

    execute {
        AdsHostFingerprint.matchSingle().method.returnEarly()
        AdSlotFingerprint.matchSingle().method.returnEarly()
        ProUpsellRowFingerprint.matchSingle().method.returnEarly()

        removeComposableCall(PRO_VERSION_HEADER, HeaderRowFingerprint.matchSingle().originalMethod)
        val clickableRow = ClickableRowFingerprint.matchSingle().originalMethod
        ABOUT_ROWS_TO_REMOVE.forEach { label -> removeComposableCall(label, clickableRow) }
    }
}
