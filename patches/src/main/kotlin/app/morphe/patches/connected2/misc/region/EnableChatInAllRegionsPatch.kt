/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.connected2.misc.region

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.requireArm64

@Suppress("unused")
val enableChatInAllRegionsPatch = bytecodePatch(
    name = "Enable chat in all regions",
    description = "Enables sign-in and chat in regions the server blocks. " +
        "Creating an account still requires a supported region.",
) {
    compatibleWith(AppCompatibilities.CONNECTED2)
    availability(requireArm64)

    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch)

    execute {
        HandleFeaturesFingerprint.matchSingle().method.addInstructions(
            0,
            """
                const-string v0, "xmpp"
                invoke-virtual { p1, v0 }, Lorg/json/JSONObject;->remove(Ljava/lang/String;)Ljava/lang/Object;
            """,
        )
    }
}
