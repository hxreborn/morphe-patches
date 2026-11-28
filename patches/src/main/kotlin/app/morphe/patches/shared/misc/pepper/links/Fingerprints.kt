/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.pepper.links

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchFirst
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object DealUrlIdentifiersFingerprint : Fingerprint(
    isStatic = true,
    returnType = "Landroid/net/Uri;",
    parameters = listOf(
        "Ljava/lang/String;",
        "L",
        "Lcom/pepper/analytics/model/OcularContext;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
    ),
    strings = listOf("user_id", "device_id", "analytics_context", "recombee_id"),
)

internal object DealSessionIdAppendFingerprint : Fingerprint(
    isStatic = false,
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        string("session_id"),
        methodCall(
            parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;"),
            returnType = "Landroid/net/Uri;",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        methodCall(
            smali = "Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal object DealSessionRedirectFingerprint : Fingerprint(
    isStatic = false,
    returnType = "Ljava/lang/Comparable;",
    parameters = listOf("Landroid/net/Uri;", "L"),
    strings = listOf("session_id"),
)

internal object AppendQueryParameterFingerprint : Fingerprint(
    isStatic = true,
    returnType = "Landroid/net/Uri;",
    parameters = listOf("Landroid/net/Uri;", "Ljava/lang/String;", "Ljava/lang/String;"),
    filters = listOf(
        opcode(Opcode.IF_NEZ, location = MatchFirst()),
        methodCall(smali = "Landroid/net/Uri;->buildUpon()Landroid/net/Uri\$Builder;"),
        methodCall(
            smali = "Landroid/net/Uri\$Builder;->appendQueryParameter(" +
                "Ljava/lang/String;Ljava/lang/String;)Landroid/net/Uri\$Builder;",
        ),
        methodCall(smali = "Landroid/net/Uri\$Builder;->build()Landroid/net/Uri;"),
    ),
)
