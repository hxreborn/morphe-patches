/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.consent

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.terabox.misc.fix.signature.spoofSignaturePatch
import app.morphe.util.indexOfFirstStringInstructionOrThrow
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val BLOCKED_REQUEST_URL = "https://consent.invalid/a/consent"

@Suppress("unused")
val hideConsentFormPatch = bytecodePatch(
    name = "Hide consent form",
    description = "Hides the ad consent form shown at startup.",
) {
    compatibleWith(AppCompatibilities.TERABOX)
    dependsOn(spoofSignaturePatch)

    execute {
        ConsentRequestFingerprint.matchSingle().method.apply {
            val index = indexOfFirstStringInstructionOrThrow(CONSENT_REQUEST_URL)
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            replaceInstruction(index, "const-string v$register, \"$BLOCKED_REQUEST_URL\"")
        }
    }
}
