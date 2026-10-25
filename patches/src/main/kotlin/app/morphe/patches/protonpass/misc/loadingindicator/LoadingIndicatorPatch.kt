/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.loadingindicator

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonpass.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.drawPullRefreshAsLoadingIndicator
import app.morphe.patches.shared.misc.proton.resizeLoadingIndicatorContainer
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val ANDROID_VIEW_PARAMETERS = 5

@Suppress("unused")
val loadingIndicatorPatch = bytecodePatch(
    name = "Material 3 loading indicator",
    description = "Shows the pull to refresh indicator as the Material 3 Expressive shape-morphing loading indicator.",
) {
    compatibleWith(AppCompatibilities.PROTON_PASS)
    dependsOn(patchesSettingsPatch)

    execute {
        val progressReads = PullRefreshProgressFingerprint.matchAll().map { match ->
            val instructions = match.instructionMatches.map { it.instruction }
            Triple(
                descriptor(instructions.first().getReference<MethodReference>()!!),
                descriptor(instructions[2].getReference<FieldReference>()!!),
                descriptor(instructions[3].getReference<MethodReference>()!!),
            )
        }.distinct()
        val (distancePulled, threshold, thresholdValue) = progressReads.singleOrNull()
            ?: throw PatchException("Expected one PullRefreshState progress computation, found $progressReads")

        drawPullRefreshAsLoadingIndicator(PullRefreshContentFingerprint, ANDROID_VIEW_PARAMETERS) { _, state, progress, scratch ->
            """
                invoke-virtual { $state }, $distancePulled
                move-result $progress
                iget-object $scratch, $state, $threshold
                invoke-virtual { $scratch }, $thresholdValue
                move-result $scratch
                div-float/2addr $progress, $scratch
            """
        }
        PullRefreshIndicatorSizeFingerprint.matchSingle().let { match ->
            resizeLoadingIndicatorContainer(match.method, match.instructionMatches.last())
        }
    }
}
