/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.updates

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object UpdatePromptConfigFingerprint : Fingerprint(
    name = "toString",
    filters = listOf(string("versionDays:")),
)

internal fun updatePromptCheckFingerprint(configType: String) = Fingerprint(
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_BOOLEAN, definingClass = configType, type = "Z"),
        fieldAccess(opcode = Opcode.IGET_BOOLEAN, definingClass = configType, type = "Z"),
        string("In App Update Dialog Displayed"),
    ),
)

internal object UpdateRequiredDialogFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = emptyList(),
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET_OBJECT, definingClass = "Lcom/viber/voip/ui/dialogs/DialogCode;", name = "D374"),
        resourceLiteral(ResourceType.STRING, "dialog_374_message"),
        methodCall(opcode = Opcode.INVOKE_STATIC, returnType = "V"),
        opcode(Opcode.RETURN_OBJECT),
    ),
)

internal fun dialogLabelsFingerprint(labels: MethodReference) = Fingerprint(
    definingClass = labels.definingClass,
    name = labels.name,
    parameters = labels.parameterTypes.map { it.toString() },
    filters = listOf(
        methodCall(parameters = listOf("I"), returnType = "V"),
        methodCall(parameters = listOf("I"), returnType = "V"),
        methodCall(parameters = listOf("I"), returnType = "V"),
        methodCall(parameters = listOf("I"), returnType = "V"),
    ),
)

internal fun positiveLabelFingerprint(setter: MethodReference) = Fingerprint(
    definingClass = setter.definingClass,
    name = setter.name,
    parameters = listOf("I"),
    filters = listOf(fieldAccess(opcode = Opcode.IPUT_OBJECT, type = "Ljava/lang/String;")),
)
