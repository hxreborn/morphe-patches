/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.upselling

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.getResourceId
import app.morphe.patches.all.misc.resources.resourceMappingPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

@Suppress("unused")
val hideUpgradeUpsellingPatch = bytecodePatch(
    name = "Hide upgrade upselling",
    description = "Hides the top-bar upgrade button and promotional sidebar rows.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)
    dependsOn(resourceMappingPatch)

    execute {
        val rows = sidebarUpsellingRowFingerprints.mapNotNull { it.methodOrNull }

        if (rows.isEmpty()) throw PatchException("Could not find any sidebar upselling row")

        rows.forEach { it.addInstruction(0, "return-void") }

        val buttonDescriptionId =
            getResourceId(ResourceType.STRING, "upselling_button_item_content_description")
        val buttons = UpgradeUpsellingButtonFingerprint.classDef.methods.filter { method ->
            method.returnType == "V" &&
                method.instructionsOrNull?.any {
                    (it as? WideLiteralInstruction)?.wideLiteral == buttonDescriptionId
                } == true
        }

        if (buttons.isEmpty()) throw PatchException("Could not find any upgrade button")

        buttons.forEach { it.addInstruction(0, "return-void") }
    }
}
