/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.hypic.misc.prompt

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities

private const val BUNDLE_QUERY_PARAMETER = "bundle"

@Suppress("unused")
val removeReinstallPromptPatch = bytecodePatch(
    name = "Remove reinstall prompt",
    description = "Removes the prompt to download the official app. Keeps saved app data.",
) {
    compatibleWith(AppCompatibilities.HYPIC)

    execute {
        RiskResponseInterceptorFingerprint.matchSingle().method.addInstructions(
            0,
            """
                move-object/from16 v0, p1
                invoke-interface { v0 }, $INTERCEPTOR_CHAIN->request()Lcom/bytedance/retrofit2/client/Request;
                move-result-object v1
                invoke-interface { v0, v1 }, $INTERCEPTOR_CHAIN->proceed(Lcom/bytedance/retrofit2/client/Request;)Lcom/bytedance/retrofit2/SsResponse;
                move-result-object v1
                return-object v1
            """,
        )

        val riskModalDeepLink = RiskModalDeepLinkFingerprint.matchSingle().method
        riskModalDeepLink.addInstructionsWithLabels(
            0,
            """
                const-string v0, "$BUNDLE_QUERY_PARAMETER"
                invoke-virtual { p1, v0 }, Landroid/net/Uri;->getQueryParameter(Ljava/lang/String;)Ljava/lang/String;
                move-result-object v0
                if-eqz v0, :open
                const-string v1, "$RISK_MODAL_BUNDLE"
                invoke-virtual { v0, v1 }, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                move-result v0
                if-eqz v0, :open
                return-void
            """,
            ExternalLabel("open", riskModalDeepLink.getInstruction(0)),
        )
    }
}
