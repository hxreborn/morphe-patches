/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.promotions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

private const val CALLS_INTRO_FLAG_TO_TAG_WINDOW = 8

private val viewVisibilityParameters = listOf("Landroid/view/View;", "Z")

internal object ChatSuggestionsResultFingerprint : Fingerprint(
    filters = listOf(
        string("fragment_result_explore_bundle_key"),
        string("fragment_result_ess_key"),
    ),
)

internal object ChatSuggestionsVisibilityFingerprint : Fingerprint(
    classFingerprint = ChatSuggestionsResultFingerprint,
    returnType = "V",
    parameters = listOf("Z", "Z"),
)

internal object CallsContactsBindingFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(
        string("getViberOutBannerBinding()Lcom/viber/voip/databinding/CallsTabContactsViberOutBannerBinding;"),
    ),
)

internal object CallsBannerVisibilityFingerprint : Fingerprint(
    classFingerprint = CallsContactsBindingFingerprint,
    name = "onLoadFinished",
    returnType = "V",
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_BOOLEAN, type = "Z"),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = viewVisibilityParameters, returnType = "V",
            location = InstructionLocation.MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_BOOLEAN, type = "Z"),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = viewVisibilityParameters, returnType = "V",
            location = InstructionLocation.MatchAfterImmediately()),
    ),
)

internal object CallsIntroFingerprint : Fingerprint(
    filters = listOf(
        methodCall(name = "isEnabled", returnType = "Z"),
        opcode(Opcode.MOVE_RESULT, location = InstructionLocation.MatchAfterImmediately()),
        string("FTUX_CALLS_TAB_FRAGMENT_TAG", location = InstructionLocation.MatchAfterWithin(CALLS_INTRO_FLAG_TO_TAG_WINDOW)),
    ),
)

internal object ViberOutSectionVisibilityFingerprint : Fingerprint(
    classFingerprint = CallsContactsBindingFingerprint,
    name = "onLoadFinished",
    returnType = "V",
    filters = listOf(
        opcode(Opcode.CONST_4),
        fieldAccess(opcode = Opcode.IGET_BOOLEAN, type = "Z", location = InstructionLocation.MatchAfterImmediately()),
        opcode(Opcode.XOR_INT_2ADDR, location = InstructionLocation.MatchAfterImmediately()),
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, parameters = viewVisibilityParameters, returnType = "V",
            location = InstructionLocation.MatchAfterImmediately()),
    ),
)
