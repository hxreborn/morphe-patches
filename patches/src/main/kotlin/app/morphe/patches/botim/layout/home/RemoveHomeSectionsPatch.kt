/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.botim.layout.home

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patches.botim.misc.premium.SduiCollectionItemsFingerprint
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.parameterRegister
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.findInstructionIndicesReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import app.morphe.patches.shared.misc.extension.botimRemoveHomeSectionsExtensionPatch

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/botim/HomeSections;"

@Suppress("unused")
val removeHomeSectionsPatch = patch(
    name = "Remove Home sections",
    description = "Removes the Services grid, Discover more banners and the search bar from Home.",
    default = false,
) {
    dependsOn(botimRemoveHomeSectionsExtensionPatch)
    compatibleWith(AppCompatibilities.BOTIM)

    execute {
        MainUiStateConstructorFingerprint.method.apply {
            val searchBarType = parameterTypes[SEARCH_BAR_PARAMETER_INDEX].toString()
            if (searchBarType != "Z") {
                throw PatchException(
                    "Expected boolean showSearchBar at parameter $SEARCH_BAR_PARAMETER_INDEX, got $searchBarType",
                )
            }
            addInstructions(0, "const/16 ${parameterRegister(SEARCH_BAR_PARAMETER_INDEX)}, 0x0")
        }

        SduiCollectionItemsFingerprint.method.apply {
            findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { index ->
                val listRegister = getInstruction<OneRegisterInstruction>(index).registerA
                addInstructionsAtControlFlowLabel(
                    index,
                    """
                        invoke-static { v$listRegister }, $EXTENSION_CLASS->withoutServicesAndDiscover(Ljava/util/List;)Ljava/util/List;
                        move-result-object v$listRegister
                    """,
                )
            }
        }
    }
}
