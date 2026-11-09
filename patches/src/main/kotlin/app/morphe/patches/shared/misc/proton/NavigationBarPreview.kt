/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patches.shared.misc.smali.replaceMethod

private const val NAVIGATION_BAR_PREVIEW_CLASS = "${PROTON_EXTENSION_PACKAGE}NavigationBarPreview;"

internal fun BytecodePatchContext.bindNavigationBarPreview(
    icons: List<String>,
    labels: List<String>,
    showLabels: Boolean,
) {
    val preview = mutableClassDefBy(NAVIGATION_BAR_PREVIEW_CLASS)
    listOf(
        Triple("iconResources", ResourceType.DRAWABLE, icons),
        Triple("labelResources", ResourceType.STRING, labels),
    ).forEach { (stub, type, names) ->
        val registers = names.indices.joinToString { "v$it" }
        val constants = names.mapIndexed { index, name ->
            "const v$index, ${resourceId(type, name)}"
        }.joinToString("\n")
        preview.replaceMethod(
            stub,
            names.size,
            """
                $constants
                filled-new-array { $registers }, [I
                move-result-object v0
                return-object v0
            """,
        )
    }
    preview.replaceMethod(
        "showLabels",
        1,
        """
            const/4 v0, ${if (showLabels) "0x1" else "0x0"}
            return v0
        """,
    )
}
