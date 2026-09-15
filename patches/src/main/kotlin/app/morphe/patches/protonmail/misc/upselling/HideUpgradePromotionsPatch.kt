/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.upselling

import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.util.getFreeRegisterProvider
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.getResourceId
import app.morphe.patches.all.misc.resources.resourceMappingPatch
import app.morphe.patches.protonmail.misc.banner.autoDeleteBannerStatePatch
import app.morphe.patches.protonmail.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.indexOfFirstLiteralInstruction
import app.morphe.util.returnEarly
import org.w3c.dom.Element

private val unhandledSidebarUpsellingPatch = resourcePatch {
    execute {
        val declared = document(get("res/values/public.xml").inputStream()).use { document ->
            val resources = document.documentElement.childNodes

            (0 until resources.length)
                .mapNotNull { resources.item(it) as? Element }
                .filter { it.getAttribute("type") == ResourceType.STRING.value }
                .map { it.getAttribute("name") }
                .filter { it.startsWith(SIDEBAR_UPSELLING_LABEL_PREFIX) }
        }

        val unhandled = declared - SIDEBAR_UPSELLING_LABELS.toSet()

        if (unhandled.isNotEmpty()) {
            throw PatchException("Unhandled sidebar upselling rows: ${unhandled.joinToString()}")
        }
    }
}

private const val UPSELLING_VISIBILITY_CLASS =
    "Lapp/hxreborn/extension/protonmail/UpsellingVisibility;"

private fun MutableMethod.returnVoidWhenUpsellingHidden() {
    val free = getFreeRegisterProvider(0, 1).getFreeRegister()

    addInstructionsWithLabels(
        0,
        """
            invoke-static { }, $UPSELLING_VISIBILITY_CLASS->isHidden()Z
            move-result v$free
            if-eqz v$free, :show
            return-void
        """,
        ExternalLabel("show", getInstruction(0)),
    )
}

@Suppress("unused")
val hideUpgradePromotionsPatch = bytecodePatch(
    name = "Hide upgrade promotions",
    description = "Hides the top-bar upgrade button, promotional sidebar rows and the auto-delete " +
        "upgrade banner in Trash and Spam. Keeps the Empty trash and Empty spam buttons.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)
    dependsOn(
        patchesSettingsPatch,
        resourceMappingPatch,
        unhandledSidebarUpsellingPatch,
        autoDeleteBannerStatePatch,
    )

    extendWith("extensions/extension.mpe")

    execute {
        mutableClassDefBy(UPSELLING_VISIBILITY_CLASS).methods
            .single { it.name == "isPatched" }
            .returnEarly(true)

        val rows = sidebarUpsellingRowFingerprints.mapNotNull { it.methodOrNull }

        if (rows.isEmpty()) throw PatchException("No sidebar upselling rows matched")

        rows.forEach { it.returnVoidWhenUpsellingHidden() }

        val buttonDescriptionId =
            getResourceId(ResourceType.STRING, "upselling_button_item_content_description")
                ?: throw PatchException("Could not find upselling_button_item_content_description")
        val buttons = UpgradeUpsellingButtonFingerprint.classDef.methods.filter { method ->
            method.returnType == "V" &&
                method.indexOfFirstLiteralInstruction(buttonDescriptionId) >= 0
        }

        if (buttons.isEmpty()) throw PatchException("No upgrade buttons matched")

        buttons.forEach { it.returnVoidWhenUpsellingHidden() }
    }
}
