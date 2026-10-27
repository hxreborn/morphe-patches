/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.post

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.setExtensionIsPatchIncluded
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val removeLateFlagPatch = bytecodePatch(
    name = "Remove late flag",
    description = "Sends BeReals you take after the moment's window as on time. Friends don't see a late tag.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(settingsPatch)

    execute {
        setExtensionIsPatchIncluded("${BEREAL_EXTENSION_PACKAGE}LateFlag;")
        val moment = MomentToStringFingerprint.matchSingle().classDef
        val instantType = moment.fields.single { it.name == "creationDate" }.type
        lateCheckFingerprint(moment.type).matchSingle().let { lateCheck ->
            val momentCast = lateCheck.instructionMatches.first()
            val register = momentCast.getInstruction<OneRegisterInstruction>().registerA
            lateCheck.method.addInstruction(
                momentCast.index + 1,
                "invoke-static/range { v$register .. v$register }, ${BEREAL_EXTENSION_PACKAGE}LateFlag;->onMoment(Ljava/lang/Object;)V",
            )
        }

        val retakeCounterSetter = CreatePostRetakeCounterSetterFingerprint.matchSingle().method
        val takenAtSetter = createPostTakenAtSetterFingerprint(retakeCounterSetter.definingClass).matchSingle().method
        val timestampType = takenAtSetter.parameterTypes[1].toString()
        val builder = createPostRequestBuilderFingerprint(retakeCounterSetter).matchSingle()
        val conversion = createPostTakenAtConversionFingerprint(instantType, timestampType, takenAtSetter)
            .match(builder.method, builder.classDef)
            .instructionMatches.first()
        builder.method.apply {
            val takenAt = conversion.getInstruction<FiveRegisterInstruction>().registerC
            addInstructions(
                conversion.index,
                """
                    invoke-static/range { v$takenAt .. v$takenAt }, ${BEREAL_EXTENSION_PACKAGE}LateFlag;->takenAt(Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v$takenAt
                    check-cast v$takenAt, $instantType
                """,
            )
        }
    }
}
