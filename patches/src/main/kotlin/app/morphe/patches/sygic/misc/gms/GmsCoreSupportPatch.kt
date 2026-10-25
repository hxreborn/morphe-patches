/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.sygic.misc.gms

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.gms.gmsCoreSupportPatchFor
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private val playServicesFallbackPatch = bytecodePatch {
    execute {
        CredentialManagerAvailabilityFingerprint.matchSingle().method.returnEarly(false)

        GooglePlayServicesAvailabilityFingerprint.matchSingle().method.returnEarly(false)

        PlayServicesGetCredentialFingerprint.matchSingle().let { match ->
            val resultIndex = match.instructionMatches.last().index
            val register = match.method.getInstruction<OneRegisterInstruction>(resultIndex).registerA
            match.method.replaceInstruction(resultIndex, "const/4 v$register, 0x0")
        }
    }
}

@Suppress("unused")
val gmsCoreSupportPatch = gmsCoreSupportPatchFor(AppCompatibilities.SYGIC, playServicesFallbackPatch)
