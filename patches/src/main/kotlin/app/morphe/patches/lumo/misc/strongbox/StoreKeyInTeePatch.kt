/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.lumo.misc.strongbox

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.lumo.misc.pairip.restoreWidgetActionsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.matchSingle

private const val STRONGBOX_BACKED_PARAMETER = "p2"

@Suppress("unused")
val storeKeyInTeePatch = bytecodePatch(
    name = "Store encryption key in TEE",
    description = "Stores the local encryption key in the TEE instead of StrongBox. " +
        "Prevents the Encryption error on devices whose StrongBox rejects the key.",
) {
    compatibleWith(AppCompatibilities.LUMO)
    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch, restoreWidgetActionsPatch)
    availability(requireArm64)

    execute {
        GenerateMasterKeyFingerprint.matchSingle().method.addInstruction(0, "const/4 $STRONGBOX_BACKED_PARAMETER, 0x0")
    }
}
