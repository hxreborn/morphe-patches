/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.castletv.misc.fix.signature

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.stockSigningCertificate
import app.morphe.util.findInstructionIndicesReversed
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.PackedSwitchPayload
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val DEVICE_GUID_CLASS = "Lapp/hxreborn/extension/castle/DeviceGuid;"

private const val USE_SWITCHED_GUID = """
    invoke-static { }, $DEVICE_GUID_CLASS->switchedGuid()Ljava/lang/String;
    move-result-object v0
    if-eqz v0, :guid_not_switched
    return-object v0
    :guid_not_switched
    nop
"""

private fun MutableMethod.switchCaseStartIndices(): Map<Int, Int> {
    val instructions = instructions.toList()
    val addresses = instructions.runningFold(0) { address, instruction -> address + instruction.codeUnits }
    val switchIndex = instructions.indexOfFirst { it.opcode == Opcode.PACKED_SWITCH }
    if (switchIndex < 0) throw PatchException("No packed-switch in $definingClass->$name")
    val switchAddress = addresses[switchIndex]
    val payloadAddress = switchAddress + (instructions[switchIndex] as OffsetInstruction).codeOffset
    val payload = instructions.getOrNull(addresses.indexOf(payloadAddress)) as? PackedSwitchPayload
        ?: throw PatchException("No packed-switch payload in $definingClass->$name")
    return payload.switchElements.associate { element ->
        val start = addresses.indexOf(switchAddress + element.offset)
        if (start < 0) throw PatchException("Switch case ${element.key} has no target in $definingClass->$name")
        element.key to start
    }
}

private fun MutableMethod.addResponseObserver() {
    val decryptionIndex = indexOfFirstInstructionOrThrow(indexOfFirstStringInstructionOrThrow(SECURITY_KEY_PREFERENCE)) {
        getReference<MethodReference>()?.let { it.definingClass == CIPHER_CLASS && it.name == "doFinal" } == true
    }
    val bodyCreationIndex = indexOfFirstInstructionOrThrow(decryptionIndex) {
        opcode == Opcode.INVOKE_STATIC && getReference<MethodReference>()?.let {
            it.parameterTypes.size == 2 && it.parameterTypes[0] == STRING_CLASS && it.returnType.startsWith("L")
        } == true
    }
    val json = getInstruction<FiveRegisterInstruction>(bodyCreationIndex).registerC
    addInstruction(bodyCreationIndex, "invoke-static { v$json }, $DEVICE_GUID_CLASS->observeResponse($STRING_CLASS)V")
}

private fun MutableMethod.addSignedRequestRetry() {
    val signingLogIndex = indexOfFirstStringInstructionOrThrow(REQUEST_SIGNING_LOG)
    val caseStarts = switchCaseStartIndices()
    val (signerKey, signerStart) = caseStarts.entries.filter { it.value <= signingLogIndex }.maxByOrNull { it.value }
        ?: throw PatchException("No switch case of $definingClass->$name contains \"$REQUEST_SIGNING_LOG\"")
    val signerEnd = caseStarts.values.filter { it > signerStart }.minOrNull() ?: instructions.size
    val chainType = parameterTypes.single().toString()

    val proceedCalls = findInstructionIndicesReversed {
        getReference<MethodReference>()?.let {
            it.definingClass != definingClass && it.parameterTypes.size == 1 && it.returnType == returnType
        } == true
    }.filter { it in signerStart until signerEnd }
    if (proceedCalls.isEmpty()) throw PatchException("No proceed call in switch case $signerKey of $definingClass->$name")

    proceedCalls.forEach { index ->
        val call = getInstruction<FiveRegisterInstruction>(index)
        if (getInstruction(index + 1).opcode != Opcode.MOVE_RESULT_OBJECT) {
            throw PatchException("Proceed call at $index in $definingClass->$name has no move-result-object")
        }
        val response = getInstruction<OneRegisterInstruction>(index + 1).registerA
        val registers = getFreeRegisterProvider(index, 2, response, call.registerC, call.registerD)
        val chain = registers.getFreeRegister4Bit()
        val signer = registers.getFreeRegister4Bit()
        addInstructionsWithLabels(
            index + 2,
            """
                invoke-static { }, $DEVICE_GUID_CLASS->shouldRetry()Z
                move-result v$signer
                if-eqz v$signer, :signed_once_$index
                invoke-interface { v$response }, Ljava/io/Closeable;->close()V
                new-instance v$signer, $definingClass
                const v$response, $signerKey
                invoke-direct { v$signer, v$response }, $definingClass-><init>(I)V
                invoke-virtual { v$signer, v$chain }, $definingClass->$name($chainType)$returnType
                move-result-object v$response
                :signed_once_$index
                nop
            """,
        )
        addInstruction(index, "move-object/from16 v$chain, v${call.registerC}")
    }
}

@Suppress("unused")
val spoofSignaturePatch = bytecodePatch(
    name = "Spoof signature",
    description = "Removes the modified app warning that stops video playback.",
) {
    compatibleWith(AppCompatibilities.CASTLE_FOR_TV)
    extendWith("extensions/extension.mpe")

    execute {
        val certificate = packageMetadata.stockSigningCertificate().encoded.joinToString("") { "%02x".format(it) }

        SignatureDigestFingerprint.matchSingle().method.addInstructions(
            0,
            """
                new-instance v0, $SIGNATURE_CLASS
                const-string v1, "$certificate"
                invoke-direct { v0, v1 }, $SIGNATURE_CLASS-><init>($STRING_CLASS)V
                move-object p0, v0
            """,
        )

        listOf(RepositoryGuidFingerprint, ApplicationGuidFingerprint).forEach {
            it.matchSingle().method.addInstructionsWithLabels(0, USE_SWITCHED_GUID)
        }

        HttpInterceptorFingerprint.matchSingle().method.apply {
            addSignedRequestRetry()
            addResponseObserver()
        }
    }
}
