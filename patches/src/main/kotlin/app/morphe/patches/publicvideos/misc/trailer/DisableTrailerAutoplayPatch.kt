/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.publicvideos.misc.trailer

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.parameterRegister

private const val TRAILER_URL_PARAMETER_INDEX = 6

@Suppress("unused")
val disableTrailerAutoplayPatch = patch(
    name = "Disable trailer autoplay",
    description = "Stops the trailers that play on their own at the top of the Local Drama page. " +
        "Only thumbnails load.",
) {
    compatibleWith(AppCompatibilities.PUBLIC)

    execute {
        SeriesUiModelConstructorFingerprint.method.apply {
            addInstruction(0, "const/4 ${parameterRegister(TRAILER_URL_PARAMETER_INDEX)}, 0x0")
        }
    }
}
