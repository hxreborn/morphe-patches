/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.raindrop.layout.ai

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.hermes.hermesVariantsPatch

private const val HOME_HEADER_BUTTON = "52 04 0A 01 93 00 6F 07 09 00 07 04"
private const val HOME_HEADER_BUTTON_NULL = "52 04 0A 01 93 00 94 07 94 07 94 07"
private const val COLLECTION_HEADER_BUTTON = "52 05 0A 01 93 01 6F 06 08 01 06 05"
private const val COLLECTION_HEADER_BUTTON_NULL = "52 05 0A 01 93 01 94 06 94 06 94 06"
private const val READER_TOOLBAR_BUTTON = "52 03 08 01 6F 03 11 01 10 03"
private const val READER_TOOLBAR_BUTTON_NULL = "52 03 08 01 94 03 94 03 94 03"

@Suppress("unused")
val hideAiAssistantPatch = rawResourcePatch(
    name = "Hide AI assistant",
    description = "Hides the AI assistant button on the home screen, in collections and in the reader.",
) {
    compatibleWith(AppCompatibilities.RAINDROP)

    dependsOn(
        hermesVariantsPatch(
            setOf(
                HOME_HEADER_BUTTON to HOME_HEADER_BUTTON_NULL,
                COLLECTION_HEADER_BUTTON to COLLECTION_HEADER_BUTTON_NULL,
                READER_TOOLBAR_BUTTON to READER_TOOLBAR_BUTTON_NULL,
            ),
        ),
    )
}
