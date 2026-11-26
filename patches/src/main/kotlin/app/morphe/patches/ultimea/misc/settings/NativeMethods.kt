/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.settings

internal typealias NativeFunction = app.morphe.patches.shared.misc.dart.NativeFunction
internal typealias NativeMethods = app.morphe.patches.shared.misc.dart.NativeMethods

internal fun NativeMethods.asyncBodyStart(function: NativeFunction): Int =
    asyncBodyStart(function, single("/api/base/v1/review/trigger"))
