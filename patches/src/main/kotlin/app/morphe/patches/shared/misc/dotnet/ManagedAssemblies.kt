/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.dotnet

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import java.io.File

private const val ASSEMBLY_STORE_LIBRARY = "libassembly-store.so"
private val ASSEMBLY_STORE_ABIS = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

internal fun ResourcePatchContext.editManagedAssembly(name: String, edit: ManagedAssembly.() -> Unit) {
    val libraries = ASSEMBLY_STORE_ABIS.map { get("lib/$it/$ASSEMBLY_STORE_LIBRARY") }.filter(File::exists)
    if (libraries.isEmpty()) throw PatchException("Could not find $ASSEMBLY_STORE_LIBRARY")

    libraries.forEach { library ->
        val store = AssemblyStoreLibrary(library.readBytes())
        val assembly = ManagedAssembly(store.readAssembly(name)).apply(edit)
        store.writeWithAssembly(name, assembly.image, library)
    }
}
