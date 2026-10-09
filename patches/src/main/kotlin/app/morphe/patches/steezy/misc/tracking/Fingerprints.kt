/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.steezy.misc.tracking

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object RudderSetupFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(
        "Lcom/facebook/react/bridge/ReadableMap;",
        "Lcom/facebook/react/bridge/ReadableMap;",
        "Lcom/facebook/react/bridge/Promise;",
    ),
    strings = listOf("Rudder Client already initialized, Ignoring the new setup call"),
)

internal object BranchTrackingDisabledForContextFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("bnc_tracking_state"),
)

internal fun branchTrackingDisabledFingerprint(trackingControllerClass: String) = Fingerprint(
    definingClass = trackingControllerClass,
    returnType = "Z",
    parameters = listOf(),
)
