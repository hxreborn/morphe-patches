/*
 * Copyright (C) 2026 Quantro100
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from Quantro100/Morphe-patches:
 * https://github.com/Quantro100/Morphe-patches/commit/f384d233384d2bc3d63d9f5011507f774d586b6f
 * Commit f384d233384d2bc3d63d9f5011507f774d586b6f (2026-06-02),
 * patches/src/main/kotlin/app/morphe/patches/aliexpress/media/EnableImageSavingPatch.kt
 */
package app.morphe.patches.aliexpress.misc.images

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object ImageSaveButtonFingerprint : Fingerprint(
    filters = listOf(
        string("hideSaveButton"),
        methodCall(
            definingClass = "Landroid/os/Bundle;",
            name = "getBoolean",
            parameters = listOf("Ljava/lang/String;", "Z"),
            returnType = "Z",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
    ),
)
