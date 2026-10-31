/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.perplexity.misc.tracking

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EVENT_PAYLOAD_TYPE = "Ljava/util/Map;"

internal object AnalyticsEventPayloadFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    strings = listOf("event_name", "event_data", "hostname", "session_id"),
)

internal fun analyticsEventDispatchFingerprint(payloadBuilder: MethodReference) = Fingerprint(
    filters = listOf(
        methodCall(payloadBuilder),
        methodCall(
            parameters = listOf(EVENT_PAYLOAD_TYPE, "Lkotlin/coroutines/jvm/internal/ContinuationImpl;"),
            returnType = "Ljava/lang/Object;",
            opcode = Opcode.INVOKE_INTERFACE,
            location = MatchAfterWithin(16),
        ),
    ),
)

internal fun analyticsEventSinkFingerprint(sinkType: String) = Fingerprint(
    returnType = "Ljava/lang/Object;",
    custom = { method, classDef ->
        sinkType in classDef.interfaces && method.parameterTypes.firstOrNull() == EVENT_PAYLOAD_TYPE
    },
)
