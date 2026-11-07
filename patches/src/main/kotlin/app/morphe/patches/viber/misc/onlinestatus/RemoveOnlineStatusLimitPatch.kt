/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.onlinestatus

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.addFilterCallAfter
import app.morphe.patches.viber.shared.markPatchApplied

@Suppress("unused")
val removeOnlineStatusLimitPatch: BytecodePatch = bytecodePatch(
    name = "Remove online status limit",
    description = "Removes the 24-hour wait between online status changes.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    execute {
        markPatchApplied("removeOnlineStatusLimit", removeOnlineStatusLimitPatch)
        listOf(OnlineStatusScreenChangeFingerprint, OnlineStatusPreferenceChangeFingerprint).forEach {
            it.matchSingle().apply {
                val (_, longValue, _) = instructionMatches
                method.addFilterCallAfter(longValue.index + 1, "$VIBER_SETTINGS_CLASS->lastOnlineChangeTime(J)J")
            }
        }
    }
}
