/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.asSequence
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val OVERLAYABLE_NAME = "HxAccentColor"
private const val BRAND_COLORS_ARRAY = "hx_accent_brand_colors"
private const val COLOR_REFERENCE = "@color/"

private fun ResourcePatchContext.valuesFiles(name: String) =
    get("res").listFiles { file -> file.isDirectory && file.name.startsWith("values") }.orEmpty()
        .filter { it.resolve(name).exists() }
        .map { "res/${it.name}/$name" }

private fun ResourcePatchContext.elements(path: String, tag: String) =
    document(path).use { document -> document.getElementsByTagName(tag).asSequence().map { it as Element }.toList() }

private fun ResourcePatchContext.appendToValues(file: String, build: (Document) -> Element) {
    val path = "res/values/$file"
    get(path).takeUnless { it.exists() }?.writeText("<resources />")
    document(path).use { document -> document.documentElement.appendChild(build(document)) }
}

internal val accentOverlayPatch = resourcePatch {
    execute {
        val names = valuesFiles("styles.xml")
            .flatMap { elements(it, "item") }
            .filter { it.getAttribute("name").startsWith("brand_") && it.textContent.startsWith(COLOR_REFERENCE) }
            .map { it.textContent.removePrefix(COLOR_REFERENCE) }
            .distinct()
            .sorted()
        if (names.isEmpty()) {
            throw PatchException("No brand_* theme item references a color resource")
        }

        val qualified = valuesFiles("colors.xml")
            .filter { it != "res/values/colors.xml" }
            .flatMap { elements(it, "color") }
            .map { it.getAttribute("name") }
            .filter { it in names }
            .distinct()
        if (qualified.isNotEmpty()) {
            throw PatchException("Brand colors with qualified values cannot share one overlay: $qualified")
        }

        appendToValues("overlayable.xml") { document ->
            document.createElement("overlayable").apply {
                setAttribute("name", OVERLAYABLE_NAME)
                appendChild(
                    document.createElement("policy").apply {
                        setAttribute("type", "public")
                        names.forEach { name ->
                            appendChild(
                                document.createElement("item").apply {
                                    setAttribute("type", "color")
                                    setAttribute("name", name)
                                },
                            )
                        }
                    },
                )
            }
        }
        appendToValues("arrays.xml") { document ->
            document.createElement("array").apply {
                setAttribute("name", BRAND_COLORS_ARRAY)
                names.forEach { name ->
                    appendChild(document.createElement("item").apply { textContent = "$COLOR_REFERENCE$name" })
                }
            }
        }
    }
}
