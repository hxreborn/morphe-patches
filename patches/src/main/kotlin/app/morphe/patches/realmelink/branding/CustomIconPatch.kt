/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.realmelink.branding

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.listDecodableFiles
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.compat.AppCompatibilities
import java.io.File
import net.coobird.thumbnailator.Thumbnails

private const val FOREGROUND_ICONS = "res/mipmap-*/ic_launcher_me_foreground.png"

private val FOREGROUND_SIZES = mapOf(
    "mdpi" to 108,
    "hdpi" to 162,
    "xhdpi" to 216,
    "xxhdpi" to 324,
    "xxxhdpi" to 432,
)

private fun density(path: String) = path.substringAfter("mipmap-").substringBefore('/').substringBefore('-')

@Suppress("unused")
val customIconPatch = patch(
    name = "Custom icon",
    description = "Replaces the launcher icon with one square PNG, scaled to every screen density.",
) {
    compatibleWith(AppCompatibilities.REALME_LINK)

    val iconPath by stringOption(
        key = "iconPath",
        title = "Icon",
        description = "Path to a square PNG. Leave empty to use the stock icon.",
    )

    execute {
        val icons = listDecodableFiles(FOREGROUND_ICONS).associateWith { FOREGROUND_SIZES[density(it)] }
        val largest = icons.maxByOrNull { it.value ?: 0 }?.key
            ?: throw PatchException("Could not find the launcher icon")
        val source = iconPath?.takeIf(String::isNotBlank)?.let(::File)?.readBytes()
            ?: resourceFile(largest).readBytes()

        icons.forEach { (path, size) ->
            if (size == null) return@forEach
            Thumbnails.of(source.inputStream()).size(size, size).outputFormat("png").toFile(resourceFile(path))
        }
    }
}
