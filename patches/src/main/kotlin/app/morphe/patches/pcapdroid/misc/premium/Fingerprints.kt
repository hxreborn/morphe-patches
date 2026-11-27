/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pcapdroid.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

internal object BillingClassFingerprint : Fingerprint(
    name = "<clinit>",
    strings = listOf("pcapdroid_supporter"),
)

internal object HasValidLicenseFingerprint : Fingerprint(
    classFingerprint = BillingClassFingerprint,
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        methodCall(definingClass = "Ljava/lang/String;", name = "isEmpty"),
    ),
)

internal object AboutMenuFingerprint : Fingerprint(
    definingClass = "Lcom/emanuelef/remote_capture/activities/AboutActivity;",
    name = "onCreateMenu",
    returnType = "V",
    parameters = listOf("Landroid/view/Menu;", "Landroid/view/MenuInflater;"),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/view/MenuInflater;",
            name = "inflate",
            parameters = listOf("I", "Landroid/view/Menu;"),
            returnType = "V",
        ),
    ),
)
