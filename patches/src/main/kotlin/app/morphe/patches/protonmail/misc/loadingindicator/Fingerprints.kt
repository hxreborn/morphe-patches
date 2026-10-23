/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.loadingindicator

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val VIEW_INTEROP_PACKAGE = "Landroidx/compose/ui/viewinterop/"
private const val ANDROID_VIEW_FACTORY_PARAMETER = 0
private const val ANDROID_VIEW_UPDATE_PARAMETER = 2

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

internal fun androidViewFingerprint(modifier: String, composer: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", modifier, "L", composer, "I", "I"),
    custom = { method, classDef ->
        classDef.type.startsWith(VIEW_INTEROP_PACKAGE) &&
            method.parameterTypes[ANDROID_VIEW_FACTORY_PARAMETER] == method.parameterTypes[ANDROID_VIEW_UPDATE_PARAMETER]
    },
)
