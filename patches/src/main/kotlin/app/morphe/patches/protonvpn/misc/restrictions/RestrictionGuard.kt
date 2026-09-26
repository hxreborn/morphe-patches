/*
 * Copyright (C) 2026 Hoo-dles
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hoo-dles/morphe-patches:
 * https://github.com/hoo-dles/morphe-patches/commit/3ad54cf13090739041b9f74c64c95e9994a0d980
 * Commit 3ad54cf13090739041b9f74c64c95e9994a0d980 (2026-07-27),
 * patches/src/main/kotlin/hoodles/morphe/patches/protonvpn/splittunneling/Fingerprints.kt
 */
package app.morphe.patches.protonvpn.misc.restrictions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode

internal open class RestrictionGuardFingerprint(settingGetter: String) : Fingerprint(
    definingClass = "Lcom/protonvpn/android/settings/data/BaseApplyEffectiveUserSettings;",
    name = "applyRestrictions",
    filters = listOf(
        opcode(Opcode.IF_EQZ),
        methodCall(
            definingClass = "Lcom/protonvpn/android/settings/data/LocalUserSettings;",
            name = settingGetter,
            location = MatchAfterImmediately(),
        ),
    ),
)

internal fun BytecodePatchContext.unlockUserSetting(
    viewState: Fingerprint,
    freeUserParameter: Int,
    restriction: RestrictionGuardFingerprint,
) {
    clearFreeUserParameter(viewState, freeUserParameter)
    keepUserSetting(restriction)
}

internal fun BytecodePatchContext.keepUserSetting(restriction: RestrictionGuardFingerprint) {
    restriction.matchSingle().run {
        method.replaceInstruction(instructionMatches.first().index, "nop")
    }
}
