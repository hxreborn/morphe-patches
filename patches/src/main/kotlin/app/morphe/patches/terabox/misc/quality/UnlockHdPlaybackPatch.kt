/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.quality

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.terabox.misc.fix.signature.spoofSignaturePatch
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.formatter.DexFormatter
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/terabox/TeraboxHdUnlock;"
private const val MAIN_PLAYER_VIEW_ID = 115
private const val RELOAD_METHOD = "reloadWithResolution"

internal val unlockHdPlaybackPatch = bytecodePatch {
    compatibleWith(AppCompatibilities.TERABOX)
    dependsOn(spoofSignaturePatch)
    extendWith("extensions/extension.mpe")

    execute {
        PremiumResolutionThresholdFingerprint.matchSingle().method.returnEarly(Int.MAX_VALUE)

        OnlineVideoInfoRequestFingerprint.matchSingle().apply {
            val requestIndex = instructionMatches.first().index
            val waitingForMetadata = instructionMatches.last().instruction.getReference<FieldReference>()!!

            method.apply {
                addInstruction(requestIndex + 1, "return-void")

                val register = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
                addInstructions(
                    0,
                    """
                        const/4 v$register, 0x1
                        iput-boolean v$register, p0, ${DexFormatter.INSTANCE.getFieldDescriptor(waitingForMetadata)}
                    """,
                )
            }
        }

        MediaFileMetaDlinkFingerprint.matchSingle().method.apply {
            val returnIndex = indexOfFirstInstructionOrThrow(Opcode.RETURN_OBJECT)
            val dlink = getInstruction<OneRegisterInstruction>(returnIndex).registerA

            addInstructions(
                returnIndex,
                """
                    iget-object p0, p0, $MEDIA_FILE_META_CLASS->path:Ljava/lang/String;
                    invoke-static { p0, v$dlink }, $EXTENSION_CLASS->recordOriginalFile(Ljava/lang/String;Ljava/lang/String;)V
                """,
            )
        }

        SetMediaUrlFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static { p0, p1 }, $EXTENSION_CLASS->selectPlaybackUrl(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;
                move-result-object p1
            """,
        )

        val reload = PlayerReloadFingerprint.matchSingle()
        val presenter = reload.classDef
        val finalInfoType = reload.method.parameterTypes[2]
        val finalInfo = presenter.fields.single { it.type == finalInfoType }
        val switchResult = ResolutionSwitchResultFingerprint.matchSingle().method

        presenter.methods.add(
            ImmutableMethod(
                presenter.type,
                RELOAD_METHOD,
                listOf(ImmutableMethodParameter(VIDEO_PLAY_RESOLUTION_CLASS, null, null)),
                "Z",
                AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                null,
                null,
                MutableMethodImplementation(6),
            ).toMutable().apply {
                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static { p1 }, $EXTENSION_CLASS->requiresReload(Ljava/lang/Enum;)Z
                        move-result v0
                        if-eqz v0, :keep_stream
                        new-instance v1, Landroid/os/Bundle;
                        invoke-direct { v1 }, Landroid/os/Bundle;-><init>()V
                        const/4 v2, 0x0
                        iget-object v3, p0, ${DexFormatter.INSTANCE.getFieldDescriptor(finalInfo)}
                        invoke-direct { p0, v1, v2, v3, p1 }, ${DexFormatter.INSTANCE.getMethodDescriptor(reload.method)}
                        const/16 v1, $MAIN_PLAYER_VIEW_ID
                        invoke-virtual { p0, v1, v2 }, ${DexFormatter.INSTANCE.getMethodDescriptor(switchResult)}
                        :keep_stream
                        return v0
                    """,
                )
            },
        )

        ResolutionSwitchFingerprint.matchSingle().apply {
            val switchIndex = instructionMatches.first().index
            method.apply {
                val (presenterRegister, resolutionRegister) = getFreeRegisterProvider(switchIndex, 2).let {
                    it.getFreeRegister4Bit() to it.getFreeRegister4Bit()
                }
                addInstructionsWithLabels(
                    switchIndex,
                    """
                        move-object/from16 v$presenterRegister, p0
                        move-object/from16 v$resolutionRegister, p2
                        invoke-direct { v$presenterRegister, v$resolutionRegister }, ${presenter.type}->$RELOAD_METHOD($VIDEO_PLAY_RESOLUTION_CLASS)Z
                        move-result v$presenterRegister
                        if-nez v$presenterRegister, :switched
                    """,
                    ExternalLabel("switched", getInstruction(switchIndex + 1)),
                )
            }
        }
    }
}
