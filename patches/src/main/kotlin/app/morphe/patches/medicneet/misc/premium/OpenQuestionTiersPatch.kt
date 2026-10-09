/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.medicneet.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.util.ReferenceUtil

private const val OPEN_TIER_RETRY_CLASS = "Lapp/hxreborn/extension/medicneet/OpenTierRetry;"

private const val TIER_FIELD = "tier"

internal val openQuestionTiersPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")

    execute {
        retryDeniedQuestionQueries()
        answerDeniedSegmentReads()
    }
}

private fun BytecodePatchContext.retryDeniedQuestionQueries() {
    val run = FirestoreQueryGetRunFingerprint.matchSingle()
    val (pathRead, _, paramsRead, handler) = run.instructionMatches
    val method = run.method

    val parser = FirestoreQueryParserFingerprint.matchSingle().instructionMatches
    val whereField = ReferenceUtil.getFieldDescriptor(parser.first().instruction.getReference<FieldReference>()!!)
    val fieldPathType = parser.last().instruction.getReference<TypeReference>()!!.type
    val fieldPathFactory = ReferenceUtil.getMethodDescriptor(fieldPathFactoryFingerprint(fieldPathType).matchSingle().method)

    val thisRegister = method.implementation!!.registerCount - method.parameterTypes.size - 1
    val failure = handler.getInstruction<OneRegisterInstruction>().registerA
    val resumeIndex = handler.index + 1
    val free = method.getFreeRegisterProvider(resumeIndex, 3, failure, thisRegister)
    val path = free.getFreeRegister4Bit()
    val where = free.getFreeRegister4Bit()
    val tier = free.getFreeRegister4Bit()
    val shouldRetry = path

    method.addInstructionsWithLabels(
        resumeIndex,
        """
            iget-object v$path, p0, ${ReferenceUtil.getFieldDescriptor(pathRead.instruction.getReference<FieldReference>()!!)}
            iget-object v$where, p0, ${ReferenceUtil.getFieldDescriptor(paramsRead.instruction.getReference<FieldReference>()!!)}
            if-eqz v$where, :resume
            iget-object v$where, v$where, $whereField
            const-string v$tier, "$TIER_FIELD"
            invoke-static { v$tier }, $fieldPathFactory
            move-result-object v$tier
            invoke-static { v$failure, v$path, v$where, v$tier }, $OPEN_TIER_RETRY_CLASS->shouldRetry(Ljava/lang/Throwable;Ljava/lang/String;Ljava/util/List;Ljava/lang/Object;)Z
            move-result v$shouldRetry
            if-eqz v$shouldRetry, :resume
            invoke-virtual { p0 }, ${run.classDef.type}->run()V
            return-void
        """,
        ExternalLabel("resume", method.getInstruction(resumeIndex)),
    )
}

private fun BytecodePatchContext.answerDeniedSegmentReads() {
    val run = FirestoreDocumentGetRunFingerprint.matchSingle()
    val matches = run.instructionMatches
    val method = run.method

    val holderRead = matches[0]
    val requestType = matches[1].instruction.getReference<TypeReference>()!!.type
    val requestPath = matches[6].instruction.getReference<FieldReference>()!!
    val snapshotCall = matches[11]
    val resultList = matches[12].instruction.getReference<FieldReference>()!!
    val resultReply = matches[14].instruction.getReference<FieldReference>()!!
    val send = matches[15].instruction.getReference<MethodReference>()!!
    val handler = matches[16]
    val report = matches[17].getInstruction<FiveRegisterInstruction>()

    val snapshotMessage = FirestoreSnapshotMessageFingerprint.match(snapshotCall.getMethodCalled())
    val (metadataCall, snapshotPath, _, snapshotMetadata) = snapshotMessage.instructionMatches
    val metadataFlags = FirestoreSnapshotMetadataFingerprint.match(metadataCall.getMethodCalled()).instructionMatches
    val snapshotType = snapshotMessage.originalMethod.returnType
    val metadataType = snapshotMetadata.instruction.getReference<FieldReference>()!!.type

    val thisRegister = holderRead.getInstruction<TwoRegisterInstruction>().registerB
    val failure = handler.getInstruction<OneRegisterInstruction>().registerA
    val resultRegister = report.registerC
    val resumeIndex = handler.index + 1
    val free = method.getFreeRegisterProvider(resumeIndex, 5, failure, thisRegister, resultRegister)
    val path = free.getFreeRegister4Bit()
    val snapshot = free.getFreeRegister4Bit()
    val metadata = free.getFreeRegister4Bit()
    val flag = free.getFreeRegister4Bit()
    val reply = free.getFreeRegister4Bit()
    val isOpenSegment = snapshot
    val results = metadata
    val insertIndex = flag

    method.addInstructionsWithLabels(
        resumeIndex,
        """
            iget-object v$path, v$thisRegister, ${ReferenceUtil.getFieldDescriptor(holderRead.instruction.getReference<FieldReference>()!!)}
            check-cast v$path, $requestType
            iget-object v$path, v$path, ${ReferenceUtil.getFieldDescriptor(requestPath)}
            invoke-static { v$failure, v$path }, $OPEN_TIER_RETRY_CLASS->isOpenSegmentRead(Ljava/lang/Throwable;Ljava/lang/String;)Z
            move-result v$isOpenSegment
            if-eqz v$isOpenSegment, :resume
            new-instance v$snapshot, $snapshotType
            invoke-direct { v$snapshot }, $snapshotType-><init>()V
            iput-object v$path, v$snapshot, ${ReferenceUtil.getFieldDescriptor(snapshotPath.instruction.getReference<FieldReference>()!!)}
            new-instance v$metadata, $metadataType
            invoke-direct { v$metadata }, $metadataType-><init>()V
            sget-object v$flag, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
            iput-object v$flag, v$metadata, ${ReferenceUtil.getFieldDescriptor(metadataFlags[0].instruction.getReference<FieldReference>()!!)}
            iput-object v$flag, v$metadata, ${ReferenceUtil.getFieldDescriptor(metadataFlags[1].instruction.getReference<FieldReference>()!!)}
            iput-object v$metadata, v$snapshot, ${ReferenceUtil.getFieldDescriptor(snapshotMetadata.instruction.getReference<FieldReference>()!!)}
            iget-object v$results, v$resultRegister, ${ReferenceUtil.getFieldDescriptor(resultList)}
            const/4 v$insertIndex, 0x0
            invoke-virtual { v$results, v$insertIndex, v$snapshot }, Ljava/util/ArrayList;->add(ILjava/lang/Object;)V
            iget-object v$reply, v$resultRegister, ${ReferenceUtil.getFieldDescriptor(resultReply)}
            invoke-virtual { v$reply, v$results }, ${ReferenceUtil.getMethodDescriptor(send)}
            return-void
        """,
        ExternalLabel("resume", method.getInstruction(resumeIndex)),
    )
}
