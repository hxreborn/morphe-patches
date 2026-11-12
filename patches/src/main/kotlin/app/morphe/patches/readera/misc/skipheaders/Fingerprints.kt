/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.skipheaders

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal const val JNI_DOC_CLASS = "Lorg/readera/jni/JniDoc;"
private const val RECT_TYPE = "Landroid/graphics/RectF;"

internal object SpeechTextIngestFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = listOf("I", RECT_TYPE),
    filters = listOf(
        methodCall(definingClass = JNI_DOC_CLASS, parameters = listOf("I", "I", "F", "F"), returnType = "I"),
    ),
)

internal object SpeechPagesFingerprint : Fingerprint(
    classFingerprint = SpeechTextIngestFingerprint,
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = listOf("I"),
    filters = listOf(
        methodCall(definingClass = "Ljava/lang/Math;", name = "max"),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        methodCall(definingClass = "Ljava/lang/Math;", name = "min"),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        newInstance(RECT_TYPE, MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "[I"),
    ),
)

internal object SmartCropFingerprint : Fingerprint(
    classFingerprint = SpeechTextIngestFingerprint,
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = RECT_TYPE,
    parameters = listOf("L", "I", "L"),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_STATIC, returnType = RECT_TYPE),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        methodCall(
            definingClass = JNI_DOC_CLASS,
            parameters = listOf("I", "I", "F", "F", "F", "F", "F", "F", "[F"),
            returnType = "I",
        ),
    ),
)

internal fun pageSizeLookupFingerprint(controllerClass: String, sizeType: String) = Fingerprint(
    definingClass = controllerClass,
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = controllerClass),
        methodCall(parameters = listOf("I"), returnType = sizeType, location = MatchAfterImmediately()),
    ),
)
