/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.castletv.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.castletv.shared.IsAdFreeMemberFingerprint
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val PREMIUM_VIP_INFO_CLASS = "Lapp/hxreborn/extension/castletv/PremiumVipInfo;"
private const val VIP_INFO_CLASS = "Lcom/gxgx/base/bean/VipInfo;"
private const val PREVIEW_PLAYLIST_CLASS = "Lapp/hxreborn/extension/castle/PreviewPlaylist;"

private fun Method.indexOfFreeUserCheck(vipHelperType: String, startIndex: Int = 0) =
    indexOfFirstInstruction(startIndex) {
        opcode == Opcode.INVOKE_VIRTUAL &&
            (this as ReferenceInstruction).reference.let { reference ->
                reference is MethodReference &&
                    reference.definingClass == vipHelperType &&
                    reference.returnType == "Z" &&
                    reference.parameterTypes.isEmpty()
            }
    }

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks member-only titles and shows the account as Premium. " +
        "Requires a signed-in account.",
) {
    compatibleWith(AppCompatibilities.CASTLE_FOR_TV)
    extendWith("extensions/extension.mpe")

    execute {
        UserVipInfosFingerprint.matchSingle().method.addInstructions(
            0,
            """
                const-class v0, $VIP_INFO_CLASS
                invoke-static { v0 }, $PREMIUM_VIP_INFO_CLASS->get(Ljava/lang/Class;)Ljava/lang/Object;
                move-result-object v0
                check-cast v0, $VIP_INFO_CLASS
                return-object v0
            """,
        )

        VideoIsPreviewFingerprint.matchSingle().method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v0
            """,
        )

        PlaylistParserFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static { p1, p2 }, $PREVIEW_PLAYLIST_CLASS->expand(Landroid/net/Uri;Ljava/io/InputStream;)Ljava/io/InputStream;
                move-result-object p2
            """,
        )

        val vipHelperType = IsAdFreeMemberFingerprint.matchSingle().classDef.type
        val defaultResolution = DefaultResolutionFingerprint.matchSingle().method
        var checkIndex = defaultResolution.indexOfFreeUserCheck(vipHelperType)
        if (checkIndex < 0) throw PatchException("No free user check in the default resolution method")
        while (checkIndex >= 0) {
            val resultIndex = checkIndex + 1
            val register = defaultResolution.getInstruction<OneRegisterInstruction>(resultIndex).registerA
            defaultResolution.replaceInstruction(resultIndex, "const/4 v$register, 0x1")
            checkIndex = defaultResolution.indexOfFreeUserCheck(vipHelperType, resultIndex + 1)
        }
    }
}
