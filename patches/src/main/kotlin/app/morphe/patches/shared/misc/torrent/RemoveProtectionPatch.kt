/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.torrent

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

internal val removeProtectionPatch = bytecodePatch {
    compatibleWith(AppCompatibilities.BITTORRENT, AppCompatibilities.UTORRENT)
    dependsOn(removePairipProtectionPatch, removePairipVirtualizationPatch)

    execute {
        val match = CertificateComparisonFingerprint.matchSingle()
        val (_, comparisonResult) = match.instructionMatches
        val resultRegister = (comparisonResult.instruction as OneRegisterInstruction).registerA
        match.method.replaceInstruction(comparisonResult.index, "const/16 v$resultRegister, 0x1")
    }
}
