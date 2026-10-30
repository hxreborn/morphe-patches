/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.analytics

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object TrackerStateRefreshFingerprint : Fingerprint(
    filters = listOf(
        literal(300L),
        methodCall(
            definingClass = "Ljava/util/concurrent/ScheduledExecutorService;",
            name = "schedule",
        ),
        methodCall(
            definingClass = "Ljava/util/concurrent/atomic/AtomicReference;",
            name = "getAndSet",
        ),
    ),
)

internal object TrackerAllowedFingerprint : Fingerprint(
    classFingerprint = TrackerStateRefreshFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        methodCall(name = "isEnabled", returnType = "Z"),
        opcode(Opcode.XOR_INT_LIT8),
    ),
)

internal object TrackerRegistryFingerprint : Fingerprint(
    filters = listOf(string("There is no tracker for event: ")),
)

internal object TrackerConsentFingerprint : Fingerprint(
    classFingerprint = TrackerRegistryFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_INTERFACE, parameters = listOf("Z"), returnType = "V"),
        methodCall(opcode = Opcode.INVOKE_INTERFACE, parameters = listOf(), returnType = "Z"),
        methodCall(opcode = Opcode.INVOKE_INTERFACE, parameters = listOf("Z"), returnType = "V"),
    ),
)

internal object SnapKitInitFingerprint : Fingerprint(
    definingClass = "Lcom/snapchat/kit/sdk/SnapKitInitProvider;",
    name = "onCreate",
    returnType = "Z",
    parameters = listOf(),
)
