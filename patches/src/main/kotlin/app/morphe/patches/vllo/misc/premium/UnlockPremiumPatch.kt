/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vllo.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.vllo.requireArm64Delta
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import org.w3c.dom.Element

private val hideStoreButtonPatch = resourcePatch {
    execute {
        listOf("res/layout/controller_main_v6.xml", "res/layout-land/controller_main_v6.xml").forEach { layout ->
            document(layout).use { document ->
                val images = document.getElementsByTagName("ImageView")
                (0 until images.length)
                    .map { images.item(it) as Element }
                    .single { it.getAttribute("android:id") == "@id/btn_premium_icon" }
                    .setAttribute("android:visibility", "gone")
            }
        }
    }
}

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks premium editing features, removes ads and the export watermark, and skips " +
        "the ad before importing audio. Hides the store button. The AI tools are not included.",
) {
    compatibleWith(AppCompatibilities.VLLO)

    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch, hideStoreButtonPatch)

    availability(requireArm64Delta)

    execute {
        val allFeatures = EntitlementEnumFingerprint.matchSingle()
            .instructionMatches[1].getInstruction<ReferenceInstruction>()
            .getReference<FieldReference>()!!

        premiumEntitlementCheckFingerprint(allFeatures.definingClass, allFeatures.name)
            .matchSingle().method.returnEarly(true)
    }
}
