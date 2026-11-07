/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.gms

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

internal val playServicesCredentialProviderPatch = bytecodePatch {
    execute {
        CredentialProviderFrameworkAvailabilityFingerprint.matchSingle().method.returnEarly(false)

        PlayServicesGetCredentialRoutingFingerprint.matchOrNull()?.let { match ->
            val (_, _, availabilityResult) = match.instructionMatches
            val register = (availabilityResult.instruction as OneRegisterInstruction).registerA
            match.method.addInstruction(availabilityResult.index + 1, "const/4 v$register, 0x0")
        }
    }
}
