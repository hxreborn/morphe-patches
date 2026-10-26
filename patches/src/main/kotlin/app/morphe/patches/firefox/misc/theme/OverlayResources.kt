/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.firefox.misc.theme

import app.morphe.patcher.patch.ResourcePatchContext

internal fun ResourcePatchContext.addOverlayable(name: String, colorNames: Collection<String>) {
    get("res/values/overlayable.xml").takeUnless { it.exists() }?.writeText("<resources />")
    document("res/values/overlayable.xml").use { document ->
        val policy = document.createElement("policy").apply { setAttribute("type", "public") }
        colorNames.forEach { colorName ->
            policy.appendChild(
                document.createElement("item").apply {
                    setAttribute("type", "color")
                    setAttribute("name", colorName)
                },
            )
        }
        document.documentElement.appendChild(
            document.createElement("overlayable").apply {
                setAttribute("name", name)
                appendChild(policy)
            },
        )
    }
}

internal fun ResourcePatchContext.addArray(tag: String, name: String, items: Collection<String>) {
    get("res/values/arrays.xml").takeUnless { it.exists() }?.writeText("<resources />")
    document("res/values/arrays.xml").use { document ->
        document.documentElement.appendChild(
            document.createElement(tag).apply {
                setAttribute("name", name)
                items.forEach { item ->
                    appendChild(document.createElement("item").apply { textContent = item })
                }
            },
        )
    }
}
