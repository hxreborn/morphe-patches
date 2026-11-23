/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.podcastrepublic.misc.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

private const val BANNER_STATE_READ_WINDOW = 5
private const val BANNER_STATE_UNBOX_WINDOW = 2

internal object AdLicenseFingerprint : Fingerprint(
    returnType = "Z",
    parameters = emptyList(),
    isStatic = true,
    filters = listOf(
        string("no_ad_license"),
        methodCall(
            parameters = listOf("Landroid/content/SharedPreferences;", "Ljava/lang/String;", "Z"),
            returnType = "Z",
            opcode = Opcode.INVOKE_STATIC,
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)

internal fun bannerVisibilityFingerprint(adLicenseClass: String) = Fingerprint(
    returnType = "V",
    parameters = listOf("Landroidx/compose/runtime/Composer;", "I"),
    filters = listOf(
        fieldAccess(definingClass = adLicenseClass, opcode = Opcode.SGET_OBJECT),
        methodCall(
            definingClass = "Landroidx/compose/runtime/State;",
            name = "getValue",
            opcode = Opcode.INVOKE_INTERFACE,
            location = MatchAfterWithin(BANNER_STATE_READ_WINDOW),
        ),
        methodCall(
            definingClass = "Ljava/lang/Boolean;",
            name = "booleanValue",
            opcode = Opcode.INVOKE_VIRTUAL,
            location = MatchAfterWithin(BANNER_STATE_UNBOX_WINDOW),
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        opcode(Opcode.IF_NEZ, MatchAfterImmediately()),
    ),
)
