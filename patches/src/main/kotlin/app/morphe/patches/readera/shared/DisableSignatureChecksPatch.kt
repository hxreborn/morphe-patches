/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch

private val disableNativeSignatureChecksPatch = rawResourcePatch {
    execute {
        val library = get(NativeSignatureChecks.LIBRARY, true)
        val bytes = library.readBytes()
        NativeSignatureChecks.applyTo(bytes)
        library.writeBytes(bytes)
    }
}

internal val disableSignatureChecksPatch = bytecodePatch {
    dependsOn(disableNativeSignatureChecksPatch)

    execute {
        LinkCodecExecutableFingerprint.method.addInstructions(
            0,
            """
                invoke-virtual { p2 }, Ljava/io/File;->getName()Ljava/lang/String;
                move-result-object p1
            """,
        )
    }
}
