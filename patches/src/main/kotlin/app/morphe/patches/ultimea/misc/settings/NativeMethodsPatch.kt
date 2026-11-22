/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.settings

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.misc.dart.dartAotLibrary

internal lateinit var nativeMethods: NativeMethods
    private set

internal val nativeMethodsPatch = patch {
    execute { nativeMethods = NativeMethods(dartAotLibrary().readBytes()) }
}
