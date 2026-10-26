/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from fork-maintainers/iceraven-browser (MPL-2.0):
 * https://github.com/fork-maintainers/iceraven-browser/commit/56c08e8000c4e16c88010ddf2e991016ddb50a8c
 * Commit 56c08e8000c4e16c88010ddf2e991016ddb50a8c (2026-10-20),
 * automation/iceraven/patches/top_sites_no_most_visted_sites.patch
 */
package app.morphe.patches.firefox.layout.shortcuts

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.firefox.misc.ui.PATCH_SETTINGS
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.parameterRegister

private const val FRECENCY_CONFIG = "Lmozilla/components/feature/top/sites/TopSitesFrecencyConfig;"

@Suppress("unused")
val hideMostVisitedShortcutsPatch = bytecodePatch(
    name = "Hide most visited shortcuts",
    description = "Hides most visited sites from the homepage shortcuts and keeps pinned shortcuts.",
    default = false,
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("hideMostVisited")

        TopSitesFingerprint.matchSingle().method.let { method ->
            val config = method.parameterRegister(1)
            method.addInstructions(
                0,
                """
                    invoke-static/range { $config .. $config }, $PATCH_SETTINGS->mostVisited(Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object $config
                    check-cast $config, $FRECENCY_CONFIG
                """,
            )
        }
    }
}
