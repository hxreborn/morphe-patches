/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.glass

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object HomeMapFingerprint : Fingerprint(strings = listOf("mapState"))

internal fun backgroundElementFingerprint(shape: String) = Fingerprint(
    name = "<init>",
    parameters = listOf("J", "L", "F", shape, "I"),
)

internal fun backgroundDrawFingerprint(node: String, drawContent: MethodReference) = Fingerprint(
    definingClass = node,
    parameters = listOf("L"),
    returnType = "V",
    filters = listOf(methodCall(name = drawContent.name, parameters = emptyList(), returnType = "V")),
)

internal fun mapViewFingerprint(modifier: String) = Fingerprint(
    strings = listOf("mapState"),
    filters = listOf(methodCall(parameters = listOf("L", modifier, "L", "L", "I"), returnType = "V")),
)

internal fun backgroundFieldsFingerprint(element: String, node: String, shape: String) = Fingerprint(
    definingClass = element,
    parameters = emptyList(),
    filters = listOf(
        fieldAccess(definingClass = element, type = "J", opcode = Opcode.IGET_WIDE),
        fieldAccess(definingClass = node, type = "J", opcode = Opcode.IPUT_WIDE, location = MatchAfterImmediately()),
        fieldAccess(definingClass = element, type = shape, opcode = Opcode.IGET_OBJECT),
        fieldAccess(definingClass = node, type = shape, opcode = Opcode.IPUT_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal fun screenPositionFingerprint(coordinator: String) = Fingerprint(
    definingClass = coordinator,
    parameters = listOf("J"),
    returnType = "J",
    filters = listOf(
        methodCall(definingClass = coordinator, parameters = listOf("J"), returnType = "J"),
        fieldAccess(definingClass = coordinator, opcode = Opcode.IGET_OBJECT),
        methodCall(
            parameters = listOf("L"),
            returnType = "L",
            opcodes = listOf(Opcode.INVOKE_STATIC, Opcode.INVOKE_STATIC_RANGE),
        ),
        methodCall(definingClass = "Landroidx/compose/ui/platform/AndroidComposeView;", parameters = listOf("J"), returnType = "J"),
    ),
)

internal object MapFadeGradientFingerprint : Fingerprint(strings = listOf("LinearGradient(colors="))

internal fun mapFadeFingerprint(gradient: String) = Fingerprint(
    strings = listOf("\$this\$drawBehind"),
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        fieldAccess(type = "F", opcode = Opcode.IGET),
        methodCall(
            parameters = listOf("F"),
            returnType = "F",
            opcodes = listOf(Opcode.INVOKE_INTERFACE, Opcode.INVOKE_INTERFACE_RANGE),
            location = MatchAfterImmediately(),
        ),
        methodCall(definingClass = gradient, name = "<init>"),
        methodCall(parameters = listOf("L", "J", "J", "J", "F", "L", "I"), returnType = "V"),
        fieldAccess(opcode = Opcode.SGET_OBJECT, location = MatchAfterImmediately()),
    ),
)
