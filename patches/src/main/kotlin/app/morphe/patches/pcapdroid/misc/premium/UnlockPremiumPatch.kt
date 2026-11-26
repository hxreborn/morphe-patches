/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pcapdroid.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findElementByAttributeValueOrThrow

val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks the firewall, malware detection and Pcapng format. Hides paid feature and donation links.",
) {
    compatibleWith(AppCompatibilities.PCAPDROID)

    execute {
        HasValidLicenseFingerprint.method.returnEarly(true)

        AboutMenuFingerprint.match().apply {
            val (inflateMenu) = instructionMatches
            val insertIndex = inflateMenu.index + 1
            val idRegister = method.getFreeRegisterProvider(insertIndex, 1).getFreeRegister4Bit()
            method.addInstructions(
                insertIndex,
                """
                    const v$idRegister, ${resourceId(ResourceType.ID, "paid_features")}
                    invoke-interface {p1, v$idRegister}, Landroid/view/Menu;->removeItem(I)V
                """,
            )
        }

        document("res/xml/root_preferences.xml").use { document ->
            val malwareDocsPreference = document.getElementsByTagName("Preference")
                .findElementByAttributeValueOrThrow("app:key", "malware_detection_docs")
            malwareDocsPreference.parentNode.removeChild(malwareDocsPreference)
        }

        document("res/menu/nav_items.xml").use { document ->
            val donateMenuItem = document.getElementsByTagName("item")
                .findElementByAttributeValueOrThrow("android:id", "@id/action_donate")
            donateMenuItem.parentNode.removeChild(donateMenuItem)
        }
    }
}
