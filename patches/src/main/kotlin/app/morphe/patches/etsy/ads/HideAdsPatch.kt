/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.etsy.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val BOX_DATA_CLASS =
    "Lcom/etsy/android/ui/search/component/model/BoxApiModel\$Data;"
private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/etsy/EtsyAdsFilter;"

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Removes promoted listings and the \"with Ads\" label from search results.",
) {
    compatibleWith(AppCompatibilities.ETSY)
    extendWith("extensions/extension.mpe")

    execute {
        SearchGridBoxFingerprint.matchSingle().method.apply {
            val index = indexOfFirstInstructionOrThrow {
                opcode == Opcode.IGET_OBJECT &&
                    getReference<FieldReference>()?.let { field ->
                        field.definingClass == BOX_DATA_CLASS &&
                            field.type == "Ljava/util/List;"
                    } == true
            }
            val register = getInstruction<TwoRegisterInstruction>(index).registerA

            addInstructions(
                index + 1,
                "invoke-static { v$register }, $EXTENSION_CLASS->removeAds(Ljava/util/List;)Ljava/util/List;\n" +
                    "move-result-object v$register",
            )
        }

        SearchHeaderFingerprint.matchSingle().method.addInstructions(0, "const/16 p2, 0x0")
    }
}
