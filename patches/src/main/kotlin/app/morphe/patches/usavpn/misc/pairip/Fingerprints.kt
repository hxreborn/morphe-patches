/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.usavpn.misc.pairip

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall

private const val ACTIVITY_CONTENT_ID = 0x1020002L

internal object LandscapeInsetsFingerprint : Fingerprint(
    definingClass = NAVIGATION,
    returnType = "V",
    filters = listOf(
        literal(ACTIVITY_CONTENT_ID),
        methodCall(definingClass = "Landroid/app/Activity;", name = "getWindow"),
    ),
)
