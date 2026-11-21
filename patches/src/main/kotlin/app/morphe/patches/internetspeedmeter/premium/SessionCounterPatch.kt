/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.internetspeedmeter.premium

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.misc.smali.replaceMethod
import app.morphe.util.cloneMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import app.morphe.patches.shared.misc.extension.internetspeedmeterAdPreferenceExtensionPatch
import app.morphe.patches.shared.misc.extension.internetspeedmeterUnlockPremiumExtensionPatch
import app.morphe.patches.shared.misc.extension.sharedDimensionsExtensionPatch

private const val SESSION_COUNTER_METHOD = "sessionCounter"

internal val sessionCounterPatch = patch {
    dependsOn(internetspeedmeterAdPreferenceExtensionPatch, internetspeedmeterUnlockPremiumExtensionPatch, sharedDimensionsExtensionPatch)
    execute {
        val reference = SessionReferenceFingerprint.method
        val editIndex = SessionReferenceFingerprint.instructionMatches.first().index
        val counterRegister = reference.getInstruction<FiveRegisterInstruction>(
            SessionReferenceFingerprint.instructionMatches.last().index,
        ).registerE

        val counter = reference.cloneMutable(
            name = SESSION_COUNTER_METHOD,
            accessFlags = AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            returnType = "F",
        ).apply {
            replaceInstruction(editIndex, "return v$counterRegister")
            removeInstructions(editIndex + 1, instructions.size - editIndex - 1)
        }
        SessionReferenceFingerprint.classDef.directMethods.add(counter)

        OverviewSessionCounterFingerprint.classDef.replaceMethod(
            SESSION_COUNTER_METHOD,
            1,
            """
                invoke-static { p0 }, ${reference.definingClass}->$SESSION_COUNTER_METHOD(Landroid/content/SharedPreferences;)F
                move-result p0
                return p0
            """,
        )
    }
}
