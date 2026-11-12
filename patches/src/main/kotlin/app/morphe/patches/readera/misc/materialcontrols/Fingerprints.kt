/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.materialcontrols

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

internal fun controlConstructorFingerprint(widget: String, parent: String) = Fingerprint(
    definingClass = "Landroidx/appcompat/widget/$widget;",
    name = "<init>",
    parameters = listOf("Landroid/content/Context;", "Landroid/util/AttributeSet;", "I"),
    returnType = "V",
    filters = listOf(methodCall(definingClass = "Landroid/widget/$parent;", name = "<init>")),
)
