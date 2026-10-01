/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allinonecalculator.misc.gms

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.gms.GMS_CORE_SIGN_IN_CLASS
import app.morphe.patches.shared.misc.gms.bindGmsCoreSignInTypes
import app.morphe.patches.shared.misc.gms.gmsCoreSpoofedSignaturePatch
import app.morphe.patches.shared.misc.gms.redirectGmsPackageToGmsCore
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.util.matchSingle

private const val REQUEST_PARAMS_CLASS =
    "Lio/flutter/plugins/googlesignin/GetCredentialRequestParams;"

@Suppress("unused")
val gmsCoreSupportPatch = bytecodePatch(
    name = "GmsCore support",
    description = "Signs in through GmsCore instead of Google Play Services. " +
        "Requires GmsCore to be installed.",
) {
    compatibleWith(AppCompatibilities.ALL_IN_ONE_CALCULATOR)
    extendWith("extensions/extension.mpe")

    dependsOn(removePairipVirtualizationPatch, gmsCoreSpoofedSignaturePatch)

    execute {
        bindGmsCoreSignInTypes()

        GetCredentialFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-virtual { p0 }, $DELEGATE_CLASS->getActivity()Landroid/app/Activity;
                move-result-object v0
                invoke-virtual { p1 }, $REQUEST_PARAMS_CLASS->getServerClientId()Ljava/lang/String;
                move-result-object v1
                invoke-static { v0, v1, p2 }, $GMS_CORE_SIGN_IN_CLASS->getCredential(Landroid/app/Activity;Ljava/lang/String;Ljava/lang/Object;)V
                return-void
            """,
        )
    }

    finalize {
        redirectGmsPackageToGmsCore()
    }
}
