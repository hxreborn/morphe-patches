/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.blockit.misc.parachutes

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/blockit/ParachutePacks;"

@Suppress("unused")
val freeParachutesPatch = bytecodePatch(
    name = "Free parachutes",
    description = "Adds parachute packs without a purchase.",
) {
    compatibleWith(AppCompatibilities.BLOCKIT)

    dependsOn(removePairipProtectionPatch)

    extendWith("extensions/extension.mpe")

    execute {
        val creditMethodName = ParachutesBoughtFingerprint.method.name
        val listenerTypes = ParachutesBoughtFingerprint.classDef.interfaces
        val billingClass = LaunchPurchaseFingerprint.classDef
        val listenerField = billingClass.fields.single { field ->
            field.type in listenerTypes && classDefBy(field.type).methods.any {
                it.name == creditMethodName && it.parameterTypes.map(CharSequence::toString) == listOf("I")
            }
        }
        val listenerFieldReference = "${billingClass.type}->${listenerField.name}:${listenerField.type}"

        LaunchPurchaseFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->size(Ljava/lang/String;)I
                move-result v0
                if-eqz v0, :launch
                iget-object v1, p0, $listenerFieldReference
                if-eqz v1, :launch
                invoke-interface { v1, v0 }, ${listenerField.type}->$creditMethodName(I)V
                return-void
            """,
            ExternalLabel("launch", LaunchPurchaseFingerprint.method.getInstruction(0)),
        )
    }
}
