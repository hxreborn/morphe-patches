/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.misc.fix.signin

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.outlook.misc.settings.markPatchApplied
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.outlookAppliedPatchesExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10t
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val MSA_DESKTOP_REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf"
private const val ORIGINAL_SIGNER_SHA1_BASE64 = "fcg80qvoM1YMKJZibjBwQcDfOno="

val fixSignInPatch: Patch = patch(
    name = "Fix sign-in",
    description = "Fixes signing in with Microsoft accounts.",
) {
    dependsOn(outlookAppliedPatchesExtensionPatch)
    compatibleWith(AppCompatibilities.OUTLOOK)

    execute {
        markPatchApplied("fixSignIn", fixSignInPatch)
        val msaConfiguration = MsaConfigurationFingerprint.match()
        val (msaConfigurationInit) = msaConfiguration.instructionMatches
        msaConfiguration.method.apply {
            val redirectUriArgumentRegister = getInstruction<FiveRegisterInstruction>(msaConfigurationInit.index).registerE
            addInstruction(
                msaConfigurationInit.index,
                "const-string v$redirectUriArgumentRegister, \"$MSA_DESKTOP_REDIRECT_URI\"",
            )
        }

        SigningCertificateSha1Fingerprint.method.apply {
            addInstructionsWithLabels(
                0,
                """
                    const-string v0, "${AppCompatibilities.OUTLOOK.packageName}"
                    invoke-virtual { v0, p1 }, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v0
                    if-eqz v0, :package_signature
                    const-string v0, "$ORIGINAL_SIGNER_SHA1_BASE64"
                    return-object v0
                """,
                ExternalLabel("package_signature", getInstruction(0)),
            )
        }

        val configureMsalAuthenticator = ConfigureMsalAuthenticatorFingerprint.match()
        val (brokerClientRead, _, brokerSetupExit) = configureMsalAuthenticator.instructionMatches
        configureMsalAuthenticator.method.apply {
            val afterBrokerSetupIndex = getInstruction<BuilderOffsetInstruction>(brokerSetupExit.index).target.location.index
            replaceInstruction(
                brokerClientRead.index,
                BuilderInstruction10t(Opcode.GOTO, implementation!!.newLabelForIndex(afterBrokerSetupIndex)),
            )
        }
    }
}
