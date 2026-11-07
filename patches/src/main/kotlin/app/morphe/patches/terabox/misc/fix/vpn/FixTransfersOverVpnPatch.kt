/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.fix.vpn

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.terabox.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.terabox.misc.settings.markPatchApplied
import app.morphe.patches.terabox.misc.settings.patchesSettingsPatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val fixTransfersOverVpnPatch: BytecodePatch = bytecodePatch(
    name = "Fix transfers over VPN",
    description = "Runs uploads, downloads and saves from shared links while a VPN is active.",
) {
    compatibleWith(AppCompatibilities.TERABOX)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    execute {
        markPatchApplied("fixTransfersOverVpn", fixTransfersOverVpnPatch)
        NetworkConnectedFingerprint.matchSingle().apply {
            val (_, _, _, ethernetTransport, ethernetCheck) = instructionMatches
            method.apply {
                val transport = getInstruction<OneRegisterInstruction>(ethernetTransport.index).registerA
                val capabilities = getInstruction<FiveRegisterInstruction>(ethernetCheck.index).registerC

                addInstructionsWithLabels(
                    ethernetTransport.index,
                    """
                        const/4 v$transport, $TRANSPORT_VPN
                        invoke-virtual { v$capabilities, v$transport }, $NETWORK_CAPABILITIES->hasTransport(I)Z
                        move-result v$transport
                        if-eqz v$transport, :ethernet
                        return v$transport
                    """,
                    ExternalLabel("ethernet", ethernetTransport),
                )
            }
        }
    }
}
