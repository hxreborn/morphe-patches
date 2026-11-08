/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.callerid

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch

@Suppress("unused")
val removeCallerIdPromptsPatch: BytecodePatch = bytecodePatch(
    name = "Remove Caller ID prompts",
    description = "Removes Caller ID introduction sheets and the Calls tab banner.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch)

    execute {
        CallerIdIntroductionFingerprint.matchSingle().method.returnEarly()
        CallerIdCallLogIntroductionFingerprint.matchSingle().method.returnEarly()
        CallerIdBannerEligibilityFingerprint.matchSingle().method.returnEarly(false)
    }
}
