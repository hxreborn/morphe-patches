/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.fof

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.resource.ResourceType
import com.android.tools.smali.dexlib2.Opcode

internal object HomeTabToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("HomeTab(title=", ", entry="),
)

internal fun homeTabListFingerprint(homeTabType: String) = Fingerprint(
    name = "invoke",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "general_fof"),
        methodCall(definingClass = homeTabType, name = "<init>", location = MatchAfterWithin(4)),
        methodCall(definingClass = "Ljava/util/List;", name = "add", location = MatchAfterImmediately()),
    ),
)

internal object EndOfFeedFooterToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("EndOfFeedFooter(viewState="),
)

internal fun endOfFeedFooterVisibilityFingerprint(footerType: String) = Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        methodCall(definingClass = "Ljava/lang/Boolean;", name = "booleanValue"),
        opcode(Opcode.MOVE_RESULT),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        checkCast(footerType, location = MatchAfterWithin(20)),
    ),
)

internal object NotActivatedToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("NotActivated(buttonText="),
)

internal fun activationCardFingerprint(notActivatedType: String) = Fingerprint(
    isStatic = true,
    returnType = "V",
    parameters = listOf("L", notActivatedType, "L", "L", "L", "Landroidx/compose/runtime/Composer;", "I"),
    filters = listOf(resourceLiteral(ResourceType.STRING, "timeline_fof_activation_title")),
)
