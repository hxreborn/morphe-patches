/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.shared

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.util.asSequence
import org.w3c.dom.Element

internal fun ResourcePatchContext.stringNamesByEnglishText(): Map<String, List<String>> =
    document("res/values/strings.xml").use { document ->
        document.getElementsByTagName("string").asSequence().map { it as Element }
            .groupBy({ it.textContent }, { it.getAttribute("name") })
    }

internal fun ResourcePatchContext.stringNameByEnglishText(text: String): String =
    stringNamesByEnglishText()[text]?.singleOrNull()
        ?: throw PatchException("Expected one \"$text\" string in res/values/strings.xml")
