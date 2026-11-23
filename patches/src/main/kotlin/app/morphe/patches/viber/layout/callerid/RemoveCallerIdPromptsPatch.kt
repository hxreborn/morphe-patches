/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.callerid

import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch

@Suppress("unused")
val removeCallerIdPromptsPatch: Patch = patch(
    name = "Remove Caller ID prompts",
    description = "Removes Caller ID introduction sheets and the Calls tab banner.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch)

    execute {
        CallerIdIntroductionFingerprint.method.returnEarly()
        CallerIdCallLogIntroductionFingerprint.method.returnEarly()
        CallerIdBannerEligibilityFingerprint.method.returnEarly(false)
    }
}
