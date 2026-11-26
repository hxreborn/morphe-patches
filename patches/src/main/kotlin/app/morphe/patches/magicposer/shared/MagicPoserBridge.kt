/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.magicposer.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourceFile
import app.morphe.util.inputStreamFromBundledResource

private const val RESOURCE_DIRECTORY = "magicposer"
private const val LIBRARY_ENTRY_DIRECTORY = "lib/arm64-v8a"

internal fun addMagicPoserBridge(libraryName: String) {
    val library = inputStreamFromBundledResource(RESOURCE_DIRECTORY, "lib$libraryName.so")
        ?: throw PatchException("Missing Magic Poser bridge library $libraryName")
    resourceFile("$LIBRARY_ENTRY_DIRECTORY/lib$libraryName.so", copy = false).writeBytes(library.use { it.readBytes() })
    MainActivityCreateFingerprint.method.addInstructions(
        0,
        """
            const-string v0, "$libraryName"
            invoke-static { v0 }, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V
        """,
    )
}
