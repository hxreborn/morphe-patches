/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.post

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.markFeaturePatched
import app.morphe.patches.bereal.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val customLocationPatch: Patch = patch(
    name = "Custom location",
    description = "Sets the location shown on your BeReal to a place you choose on a map or by name.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(settingsPatch)

    execute {
        markFeaturePatched("${BEREAL_EXTENSION_PACKAGE}CustomLocation;", customLocationPatch)
        FusedLocationClientGetterFingerprint.match().let { getter ->
            val cast = getter.instructionMatches.single()
            val client = cast.getInstruction<OneRegisterInstruction>().registerA
            getter.method.addInstructions(
                cast.index + 1,
                """
                    invoke-static { v$client }, ${BEREAL_EXTENSION_PACKAGE}CustomLocation;->locationClient(Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v$client
                    check-cast v$client, Lcom/google/android/gms/location/FusedLocationProviderClient;
                """,
            )
        }
        val retakeCounterSetter = CreatePostRetakeCounterSetterFingerprint.method
        val builder = createPostRequestBuilderFingerprint(retakeCounterSetter).match()
        val (locationRead) = LocationToStringFingerprint.matchAll(1..3)
            .mapNotNull { createPostLocationReadFingerprint(it.classDef.type).matchOrNull(builder.method, builder.classDef) }
            .single()
            .instructionMatches
        val locationType = locationRead.fieldReference.type
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
