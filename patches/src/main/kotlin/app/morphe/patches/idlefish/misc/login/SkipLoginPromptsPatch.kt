/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.idlefish.misc.login

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.idlefish.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val skipLoginPromptsPatch = patch(
    name = "Skip login prompts",
    description = "Skips login screens that open at launch, on search and listing taps and during background requests.",
) {
    compatibleWith(AppCompatibilities.IDLEFISH)

    dependsOn(spoofSignaturePatch)

    execute {
        skipLoginPrompts()
    }
}

internal fun skipLoginPrompts() {
    FirstLaunchLoginGuideFingerprint.method.returnEarly(false)
    ExtraOpenLoginFingerprint.method.returnEarly(false)
    RemoteLoginFingerprint.method.addInstructions(
        0,
        """
            invoke-static {p0, p1}, $LOGIN_HANDLER->instance(Lmtopsdk/mtop/intf/Mtop;Ljava/lang/String;)$LOGIN_HANDLER
            move-result-object v0
            invoke-virtual {v0}, $LOGIN_HANDLER->onLoginCancel()V
            return-void
        """,
    )
}
