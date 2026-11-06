/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.connected2.misc.deleted

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/connected2/DeletedMessages;"

@Suppress("unused")
val keepDeletedMessagesPatch = bytecodePatch(
    name = "Keep deleted messages",
    description = "Keeps messages the other person deletes and marks deleted text messages with 🗑.",
) {
    compatibleWith(AppCompatibilities.CONNECTED2)
    availability(requireArm64)

    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch)

    extendWith("extensions/extension.mpe")

    execute {
        DeleteMessageHandlerFingerprint.matchSingle().apply {
            val (_, _, lookupCall, _, ageCheckMatch) = instructionMatches
            val ageCheck = ageCheckMatch.index
            val message = method.getInstruction<OneRegisterInstruction>(lookupCall.index + 1).registerA
            val skipDelete = method.getInstruction<BuilderOffsetInstruction>(ageCheck).target.location.instruction!!
            val messageType = lookupCall.instruction.getReference<MethodReference>()!!.returnType
            val contentTypeGetter = messageContentTypeGetterFingerprint(messageType).matchSingle().originalMethod
            val bodyGetter = messageBodyGetterFingerprint(messageType).matchSingle().originalMethod
            val bodySetter = messageBodySetterFingerprint(messageType).matchSingle().originalMethod
            val save = recordSaveFingerprint(classDefBy(messageType).superclass!!).matchSingle().originalMethod
            with(mutableClassDefBy(EXTENSION_CLASS)) {
                replaceStub(
                    "bodyOf",
                    1,
                    """
                        check-cast p0, $messageType
                        invoke-virtual { p0 }, ${descriptor(bodyGetter)}
                        move-result-object p0
                        return-object p0
                    """,
                )
                replaceStub(
                    "saveBody",
                    2,
                    """
                        check-cast p0, $messageType
                        invoke-virtual { p0, p1 }, ${descriptor(bodySetter)}
                        invoke-virtual { p0 }, ${descriptor(save)}
                        return-void
                    """,
                )
            }
            val (_, textField) = textContentTypeFingerprint(contentTypeGetter.returnType).matchSingle()
                .instructionMatches
            val registers = method.getFreeRegisterProvider(ageCheck + 1, 2, message)
            val contentType = registers.getFreeRegister4Bit()
            val textContentType = registers.getFreeRegister4Bit()

            method.addInstructionsWithLabels(
                ageCheck + 1,
                """
                    invoke-virtual { v$message }, ${descriptor(contentTypeGetter)}
                    move-result-object v$contentType
                    sget-object v$textContentType, ${textField.instruction.getReference<FieldReference>()!!}
                    invoke-static { v$message, v$contentType, v$textContentType }, $EXTENSION_CLASS->mark(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                    goto :skip_delete
                """,
                ExternalLabel("skip_delete", skipDelete),
            )
        }
    }
}
