/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Additional terms under GPLv3 section 7:
 * - You must preserve reasonable legal notices and author attributions in this file.
 * - Modified versions must not misrepresent the origin of this file.
 *
 * Ported from hxreborn/revanced-patches:
 * https://gitlab.com/hxreborn/revanced-patches/-/commit/7cbb1133f9a4328f5df41162c96438cc36527125
 * Commit 7cbb1133f9a4328f5df41162c96438cc36527125 (2026-05-07),
 * patches/src/main/kotlin/app/revanced/patches/showly/misc/premium/UnlockPremiumPatch.kt
 */
package app.morphe.patches.showly.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.getResourceId
import app.morphe.patches.all.misc.resources.resourceMappingPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks ad removal, light theme, custom images, list view types, quick ratings, and transparent widgets.",
) {
    compatibleWith(AppCompatibilities.SHOWLY)

    dependsOn(resourceMappingPatch)

    execute {
        PremiumRepoConstructorFingerprint.classDef.methods
            .single {
                it.returnType == "Z" && it.parameters.isEmpty()
            }.returnEarly(true)

        // Prevent revocation
        QonversionCheckEntitlementsFingerprint.method.returnEarly()

        // Onboarding paywall
        TraktLoginPaywallNavigationFingerprint.method.apply {
            val actionIdConstIndex = TraktLoginPaywallNavigationFingerprint.instructionMatches[0].index
            val actionIdRegister = getInstruction<OneRegisterInstruction>(actionIdConstIndex).registerA
            val actionId = getResourceId(ResourceType.ID, "actionNavigateProgressFragment")!!

            replaceInstruction(actionIdConstIndex, "const v$actionIdRegister, 0x${actionId.toString(16)}")
        }
    }
}
