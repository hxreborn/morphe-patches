/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.gms

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchAllMethodIndicesForEach
import app.morphe.patches.imo.misc.signin.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.gms.gmsCoreSupportPatchFor
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/imo/AccountChooser;"

private val accountChooserPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")

    execute {
        AddAccountCallFingerprint.matchAllMethodIndicesForEach { index ->
            val call = getInstruction<RegisterRangeInstruction>(index)
            val lastRegister = call.startRegister + call.registerCount - 1
            replaceInstruction(
                index,
                "invoke-static/range { v${call.startRegister} .. v$lastRegister }, $EXTENSION_CLASS->chooseAccount(" +
                    "Landroid/accounts/AccountManager;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;" +
                    "Landroid/os/Bundle;Landroid/app/Activity;Landroid/accounts/AccountManagerCallback;" +
                    "Landroid/os/Handler;)Landroid/accounts/AccountManagerFuture;",
            )
        }
    }
}

@Suppress("unused")
val gmsCoreSupportPatch = gmsCoreSupportPatchFor(AppCompatibilities.IMO, spoofSignaturePatch, accountChooserPatch)
