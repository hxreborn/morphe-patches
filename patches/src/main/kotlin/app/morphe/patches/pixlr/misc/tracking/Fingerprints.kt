/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pixlr.misc.tracking

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object EnableCrashlyticsCollectionFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        fieldAccess(definingClass = "Ljava/lang/Boolean;", name = "TRUE", opcode = Opcode.SGET_OBJECT),
        string("firebase_crashlytics_collection_enabled", MatchAfterWithin(8)),
        methodCall(
            definingClass = "Landroid/content/SharedPreferences\$Editor;",
            name = "putBoolean",
            location = MatchAfterWithin(5),
        ),
    ),
)

internal object SendMobileEventsFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        string("mobile_events_enabled"),
        methodCall(returnType = "Z", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)

internal object ActivateFacebookAppFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Landroid/app/Application;"),
    filters = listOf(
        string("app_events_killswitch"),
    ),
)
