/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * patches/src/main/kotlin/app/ftl/patches/FireFox/TabIconsPatch.kt
 */
package app.morphe.patches.firefox.misc.ui

import app.morphe.patcher.patch.resourcePatch

private fun tabIconXml(path: String) = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#00000000"
        android:strokeColor="#FF000000"
        android:strokeWidth="2"
        android:strokeLineJoin="round"
        android:strokeLineCap="round"
        android:pathData="$path" />
</vector>
"""

val tabIconsPatch = resourcePatch {
    execute {
        mapOf(
            "hx_ic_scroll_up" to "M12,19L12,5M6,11L12,5L18,11",
            "hx_ic_scroll_down" to "M12,5L12,19M6,13L12,19L18,13",
            "hx_ic_tab_add_end" to "M12,4L12,12M8,8L16,8M8,16L12,20L16,16",
        ).forEach { (name, path) ->
            get("res/drawable/$name.xml", copy = false).apply {
                parentFile?.mkdirs()
                writeText(tabIconXml(path))
            }
        }
    }
}
