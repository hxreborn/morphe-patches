/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.secondary

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object SecondaryModeDefaultFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(
        methodCall(returnType = "Z", parameters = listOf(), opcode = Opcode.INVOKE_VIRTUAL),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        string("use_as_secondary"),
    ),
)

internal object SetSecondaryModeFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Z"),
    filters = listOf(string("Secondary state was changed to ")),
)

internal fun secondaryModeResetFingerprint(tabletCheck: MethodReference, setSecondaryMode: MethodReference) =
    Fingerprint(
        filters = listOf(
            methodCall(tabletCheck),
            opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
            methodCall(setSecondaryMode, MatchAfterImmediately()),
        ),
    )

internal object SecondaryPlatformNameFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    isStatic = true,
    filters = listOf(
        methodCall(returnType = "Z", parameters = emptyList(), opcode = Opcode.INVOKE_STATIC),
        string("AndroidTablet"),
        string("Android"),
    ),
)

internal object RegistrationDeviceTypeFingerprint : Fingerprint(
    filters = listOf(
        resourceLiteral(ResourceType.BOOL, "send_tablet_device_type_on_registration"),
        methodCall(
            definingClass = "Landroid/content/res/Resources;",
            name = "getBoolean",
            parameters = listOf("I"),
            returnType = "Z",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)
