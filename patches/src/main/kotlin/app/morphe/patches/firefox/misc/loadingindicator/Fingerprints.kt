/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.firefox.misc.loadingindicator

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

private const val CONFIG_MEDIUM_ANIM_TIME = 0x10e0001
private const val STOCK_CIRCLE_DIAMETER_DP = 40f

internal object SwipeRefreshLayoutConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Landroid/util/AttributeSet;"),
    filters = listOf(
        literal(CONFIG_MEDIUM_ANIM_TIME),
        literal(STOCK_CIRCLE_DIAMETER_DP),
        methodCall(definingClass = "Landroid/view/ViewGroup;", name = "setChildrenDrawingOrderEnabled"),
    ),
)
