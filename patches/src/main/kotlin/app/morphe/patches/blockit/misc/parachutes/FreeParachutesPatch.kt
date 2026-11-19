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
        val refreshMethodName = ParachutesBoughtFingerprint.method.name
        val listenerTypes = ParachutesBoughtFingerprint.classDef.interfaces
        val preferences = ParachuteCountSetterFingerprint.method
        val getter = ParachuteCountGetterFingerprint.method
        val billingClass = LaunchPurchaseFingerprint.classDef
        val listenerField = billingClass.fields.single { field ->
            field.type in listenerTypes && classDefBy(field.type).methods.any {
                it.name == refreshMethodName && it.parameterTypes.isEmpty()
            }
        }
        val preferencesField = billingClass.fields.single { it.type == preferences.definingClass }
        val billingType = billingClass.type

        LaunchPurchaseFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->size(Ljava/lang/String;)I
                move-result v0
                if-eqz v0, :launch
                iget-object v1, p0, $billingType->${preferencesField.name}:${preferencesField.type}
                invoke-virtual { v1 }, ${getter.definingClass}->${getter.name}()I
                move-result v2
                add-int/2addr v0, v2
                invoke-virtual { v1, v0 }, ${preferences.definingClass}->${preferences.name}(I)V
                iget-object v1, p0, $billingType->${listenerField.name}:${listenerField.type}
                if-eqz v1, :done
                invoke-interface { v1 }, ${listenerField.type}->$refreshMethodName()V
                :done
                return-void
            """,
            ExternalLabel("launch", LaunchPurchaseFingerprint.method.getInstruction(0)),
        )
    }
}
