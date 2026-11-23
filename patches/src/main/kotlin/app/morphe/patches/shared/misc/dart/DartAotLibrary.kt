/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.dart

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourceFile
import java.io.File

internal const val DART_AOT_LIBRARY = "lib/arm64-v8a/libapp.so"

internal fun dartAotLibrary(): File =
    resourceFile(DART_AOT_LIBRARY).takeIf(File::exists) ?: throw PatchException("Could not find $DART_AOT_LIBRARY")
