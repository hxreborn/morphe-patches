/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.habitify.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal object SnapshotToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    strings = listOf("DataSnapshot { key = "),
)

internal object FirebaseUserMapFingerprint : Fingerprint(
    definingClass = "Lio/invertase/firebase/auth/ReactNativeFirebaseAuthModule;",
    name = "firebaseUserToMap",
    strings = listOf("uid", "isAnonymous"),
)

internal object FirebaseUserIdFingerprint : Fingerprint(
    definingClass = "Lio/invertase/firebase/auth/ReactNativeFirebaseAuthModule;",
    name = "convertProviderData",
    filters = listOf(
        string("uid"),
        methodCall(returnType = "Ljava/lang/String;", parameters = emptyList(), location = MatchAfterImmediately()),
    ),
)

internal object ParseNodeFingerprint : Fingerprint(
    isStatic = true,
    strings = listOf(".priority", ".value", "Failed to parse node"),
)
