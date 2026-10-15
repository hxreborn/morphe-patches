/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.minipages

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal const val READ_LAYOUT_CLASS = "Lorg/readera/read/widget/ReadLayoutMain;"

internal object HidePageThumbnailsButtonFingerprint : Fingerprint(
    definingClass = READ_LAYOUT_CLASS,
    name = "onEventMainThread",
    returnType = "V",
    filters = listOf(
        opcode(Opcode.CONST),
        methodCall(definingClass = "Landroid/view/View;", name = "findViewById", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        literal(8, location = MatchAfterImmediately()),
        methodCall(name = "setVisibility", location = MatchAfterImmediately()),
    ),
)

internal fun hideButtonOnInflateFingerprint(buttonId: Long) = Fingerprint(
    definingClass = READ_LAYOUT_CLASS,
    name = "onFinishInflate",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        literal(buttonId),
        methodCall(definingClass = "Landroid/view/View;", name = "findViewById", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        methodCall(name = "setVisibility"),
    ),
)
