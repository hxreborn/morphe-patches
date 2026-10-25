/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.loadingindicator

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import com.android.tools.smali.dexlib2.Opcode

internal object PullToRefreshSpinnerFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L", "L", "I"),
    filters = listOf(resourceLiteral(ResourceType.RAW, "proton_spinner")),
)

internal object PullToRefreshPullingIconFingerprint : Fingerprint(
    filters = listOf(
        resourceLiteral(ResourceType.DRAWABLE, "proton_spinner_stationary"),
        methodCall(parameters = listOf("L", "F"), returnType = "L", opcode = Opcode.INVOKE_STATIC),
        methodCall(
            parameters = listOf("L", "F"),
            returnType = "L",
            opcode = Opcode.INVOKE_STATIC,
            location = MatchAfterWithin(1),
        ),
        methodCall(
            parameters = listOf("L", "Ljava/lang/String;", "L", "L", "L", "F", "L", "L", "I", "I"),
            returnType = "V",
            opcode = Opcode.INVOKE_STATIC_RANGE,
        ),
    ),
)

internal object PullToRefreshIndicatorSizeFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(type = "F", opcode = Opcode.SGET),
        methodCall(
            parameters = listOf("L", "F"),
            returnType = "L",
            opcode = Opcode.INVOKE_STATIC,
            location = MatchAfterImmediately(),
        ),
        methodCall(
            parameters = listOf("L", "Z", "L", "F", "L", "J", "F", "L", "L", "I", "I"),
            returnType = "V",
            opcode = Opcode.INVOKE_VIRTUAL_RANGE,
        ),
    ),
)
