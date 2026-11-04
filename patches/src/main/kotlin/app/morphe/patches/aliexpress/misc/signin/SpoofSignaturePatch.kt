/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.aliexpress.misc.signin

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.aliexpress.misc.settings.markPatchApplied
import app.morphe.patches.aliexpress.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.spoofSignature

private const val DESCRIPTION = "Restores product loading on patched installs."

@Suppress("unused")
val spoofSignaturePatch = bytecodePatch(
    name = "Spoof signature",
    description = DESCRIPTION,
) {
    compatibleWith(AppCompatibilities.ALIEXPRESS)

    dependsOn(signingDataPatch, patchesSettingsPatch)

    extendWith("extensions/extension.mpe")

    execute {
        markPatchApplied("spoofSignature", DESCRIPTION)
        spoofSignature("Lcom/aliexpress/app/AliExpressApp;")
        SecurityContextInitializerFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static/range { p1 .. p1 }, Lapp/hxreborn/extension/aliexpress/SecurityContext;->wrap(Landroid/content/Context;)Landroid/content/Context;
                move-result-object p1
            """.trimIndent(),
        )
    }
}
