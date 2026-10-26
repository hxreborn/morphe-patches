/*
 * Copyright (C) 2026 heval99
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from heval99/Heval-Morphe-Patches:
 * https://github.com/heval99/Heval-Morphe-Patches/commit/28097b43dbc8de99bb516a44f84708ae6cbb0f64
 * Commit 28097b43dbc8de99bb516a44f84708ae6cbb0f64 (2026-10-25),
 * patches/src/main/kotlin/app/template/patches/firefox/misc/tabs/KeepBackgroundTabsLoadedPatch.kt
 */
package app.morphe.patches.firefox.misc.tabs

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.firefox.misc.ui.PATCH_SETTINGS
import app.morphe.patches.firefox.shared.forceSettingsPreference
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.addStaticMethod

private const val KEEP_TABS = "$PATCH_SETTINGS->keepTabsLoaded()Z"

private const val GECKO_PROCESS_TYPE = "Lorg/mozilla/gecko/process/GeckoProcessType;"

private val CONTENT_PROCESS_TYPES = listOf("CONTENT", "CONTENT_ISOLATED", "CONTENT_ISOLATED_WITH_ZYGOTE")

@Suppress("unused")
val keepBackgroundTabsLoadedPatch = bytecodePatch(
    name = "Keep background tabs loaded",
    description = "Keeps background tabs loaded and shows a tab's last screenshot while it reloads. " +
        "Experimental. Uses more memory.",
    default = false,
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("keepTabsLoaded")

        SetPriorityLevelFingerprint.matchSingle().let { match ->
            val contentChecks = CONTENT_PROCESS_TYPES.joinToString("\n") {
                """
                    sget-object v1, $GECKO_PROCESS_TYPE->$it:$GECKO_PROCESS_TYPE
                    if-eq v0, v1, :content
                """.trimIndent()
            }
            val keepPriority = match.classDef.addStaticMethod(
                "keepContentProcessPriority",
                listOf(INSTANCE_INFO, PRIORITY_LEVEL),
                PRIORITY_LEVEL,
                4,
                """
                    invoke-static { }, $KEEP_TABS
                    move-result v0
                    if-nez v0, :enabled
                    return-object p1
                    :enabled
                    iget-object v0, p0, $INSTANCE_INFO->mType:$GECKO_PROCESS_TYPE
                    $contentChecks
                    return-object p1
                    :content
                    sget-object v0, $PRIORITY_LEVEL->IDLE:$PRIORITY_LEVEL
                    if-ne p1, v0, :keep
                    sget-object p1, $PRIORITY_LEVEL->BACKGROUND:$PRIORITY_LEVEL
                    :keep
                    return-object p1
                """,
            )
            match.method.addInstructions(
                0,
                """
                    invoke-static {p0, p1}, $keepPriority
                    move-result-object p1
                """,
            )
        }

        forceSettingsPreference("pref_key_tab_reload_cover_enabled", true, KEEP_TABS)
    }
}
