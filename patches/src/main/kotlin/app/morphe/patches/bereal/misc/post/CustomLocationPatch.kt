/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.post

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.setExtensionIsPatchIncluded
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val customLocationPatch = bytecodePatch(
    name = "Custom location",
    description = "Sets the location shown on your BeReal to a place you choose on a map or by name.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(settingsPatch)

    execute {
        setExtensionIsPatchIncluded("${BEREAL_EXTENSION_PACKAGE}CustomLocation;")
        val retakeCounterSetter = CreatePostRetakeCounterSetterFingerprint.matchSingle().method
        val builder = createPostRequestBuilderFingerprint(retakeCounterSetter).matchSingle()
        val locationRead = LocationToStringFingerprint.matchAll(1..3)
            .mapNotNull { createPostLocationReadFingerprint(it.classDef.type).matchOrNull(builder.method, builder.classDef) }
            .single()
            .instructionMatches.first()
        val locationType = locationRead.getInstruction<ReferenceInstruction>().getReference<FieldReference>()!!.type
        builder.method.apply {
            val locationIndex = locationRead.index
            val location = locationRead.getInstruction<OneRegisterInstruction>().registerA
            val type = getFreeRegisterProvider(locationIndex + 1, 1, location).getFreeRegister()
            if (location > 15 || type > 15) throw PatchException("Location registers v$location and v$type exceed v15")
            addInstructions(
                locationIndex + 1,
                """
                    const-class v$type, $locationType
                    invoke-static { v$location, v$type }, ${BEREAL_EXTENSION_PACKAGE}CustomLocation;->location(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;
                    move-result-object v$location
                    check-cast v$location, $locationType
                """,
            )
        }
    }
}
