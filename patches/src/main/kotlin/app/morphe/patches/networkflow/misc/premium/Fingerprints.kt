/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.networkflow.misc.premium

import app.morphe.patcher.Fingerprint

internal object ProStatusToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    strings = listOf("ProStatus(source="),
)

internal object ProStatusDefaultConstructorFingerprint : Fingerprint(
    classFingerprint = ProStatusToStringFingerprint,
    name = "<init>",
    returnType = "V",
    parameters = listOf("I"),
)
