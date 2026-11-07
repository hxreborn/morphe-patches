/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.post

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.markFeaturePatched
import app.morphe.patches.bereal.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.addFieldReader
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val CREATION_DATE_FIELD = "creationDate"
private const val EXPIRATION_DATE_FIELD = "expirationDate"
private const val JAVA_INSTANT_TYPE = "Ljava/time/Instant;"

private fun readFieldSmali(ownerType: String, reader: String) = """
    check-cast p0, $ownerType
    invoke-virtual { p0 }, $reader
    move-result-object p0
    return-object p0
"""

@Suppress("unused")
val removeLateFlagPatch: BytecodePatch = bytecodePatch(
    name = "Remove late flag",
    description = "Sends BeReals you take after the moment's window as on time.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(settingsPatch)

    execute {
        markFeaturePatched("${BEREAL_EXTENSION_PACKAGE}LateFlag;", removeLateFlagPatch)
        val moment = MomentToStringFingerprint.matchSingle().classDef
        val creationDate = moment.fields.single { it.name == CREATION_DATE_FIELD }
        val expirationDate = moment.fields.single { it.name == EXPIRATION_DATE_FIELD }
        val instantType = creationDate.type
        val instantValue = classDefBy(instantType).fields.single { it.type == JAVA_INSTANT_TYPE }
        val instantConstructor = timestampConstructorFingerprint(instantType).matchSingle().originalMethod
        val readCreationDate = mutableClassDefBy(moment.type).addFieldReader(creationDate)
        val readExpirationDate = mutableClassDefBy(moment.type).addFieldReader(expirationDate)
        val readInstantValue = mutableClassDefBy(instantType).addFieldReader(instantValue)
        mutableClassDefBy("${BEREAL_EXTENSION_PACKAGE}LateFlag;").apply {
            replaceStub("creationDate", 1, readFieldSmali(moment.type, readCreationDate))
            replaceStub("expirationDate", 1, readFieldSmali(moment.type, readExpirationDate))
            replaceStub("instantValue", 1, readFieldSmali(instantType, readInstantValue))
            replaceStub(
                "timestampOf",
                2,
                """
                    new-instance v0, $instantType
                    invoke-direct { v0, p0 }, ${descriptor(instantConstructor)}
                    return-object v0
                """,
            )
        }
        lateCheckFingerprint(moment.type).matchSingle().let { lateCheck ->
            val momentCast = lateCheck.instructionMatches.single()
            val register = momentCast.getInstruction<OneRegisterInstruction>().registerA
            lateCheck.method.addInstruction(
                momentCast.index + 1,
                "invoke-static/range { v$register .. v$register }, ${BEREAL_EXTENSION_PACKAGE}LateFlag;->onMoment(Ljava/lang/Object;)V",
            )
        }

        val retakeCounterSetter = CreatePostRetakeCounterSetterFingerprint.matchSingle().method
        val takenAtSetter = createPostTakenAtSetterFingerprint(retakeCounterSetter.definingClass).matchSingle().method
        val (_, timestampParameter) = takenAtSetter.parameterTypes
        val timestampType = timestampParameter.toString()
        val builder = createPostRequestBuilderFingerprint(retakeCounterSetter).matchSingle()
        val (_, isMainRead) = CorePostToStringFingerprint.matchSingle().instructionMatches
        val isMainField = isMainRead.getInstruction<ReferenceInstruction>().reference as FieldReference
        val (takenAtRead, conversion) = createPostTakenAtConversionFingerprint(
            isMainField.definingClass,
            instantType,
            timestampType,
            takenAtSetter,
        ).match(builder.method, builder.classDef).instructionMatches
        val corePost = takenAtRead.getInstruction<TwoRegisterInstruction>().registerB
        builder.method.apply {
            val takenAt = conversion.getInstruction<FiveRegisterInstruction>().registerC
            val isMain = getFreeRegisterProvider(conversion.index, 1, takenAt, corePost).getFreeRegister4Bit()
            addInstructions(
                conversion.index,
                """
                    iget-boolean v$isMain, v$corePost, $isMainField
                    invoke-static { v$takenAt, v$isMain }, ${BEREAL_EXTENSION_PACKAGE}LateFlag;->takenAt(Ljava/lang/Object;Z)Ljava/lang/Object;
                    move-result-object v$takenAt
                    check-cast v$takenAt, $instantType
                """,
            )
        }
    }
}
