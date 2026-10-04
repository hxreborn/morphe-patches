/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

private const val REPORT_LOAD_FAILURE =
    "Lapp/hxreborn/extension/shared/AdMobLoadFailure;->report(Ljava/lang/Object;)V"

val failAdMobLoadsPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")

    execute {
        (AppOpenAdLoadFingerprint.matchAll() + InterstitialAdLoadFingerprint.matchAll()).forEach { match ->
            match.method.addInstructions(
                0,
                "invoke-static { p${match.method.parameters.size - 1} }, $REPORT_LOAD_FAILURE\n" +
                    "return-void",
            )
        }

        AdLoaderWithAdListenerFingerprint.matchSingle().method.addInstruction(
            0,
            "invoke-static { p1 }, $REPORT_LOAD_FAILURE",
        )
        AdLoaderLoadFingerprint.matchAll().forEach { it.method.returnEarly() }
    }
}
