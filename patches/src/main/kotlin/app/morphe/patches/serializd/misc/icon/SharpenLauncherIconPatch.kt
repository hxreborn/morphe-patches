/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.serializd.misc.icon

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.listApkEntries
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.png.PngImage

private const val SPLASH_IMAGE = "res/drawable/splashscreen_image.png"
private const val FOREGROUND_FILE = "/ic_launcher_foreground.png"

private val densityQualifier = Regex("mipmap-([a-z]+dpi)")

private val foregroundSizeByDensity = mapOf(
    "mdpi" to 108,
    "hdpi" to 162,
    "xhdpi" to 216,
    "xxhdpi" to 324,
    "xxxhdpi" to 432,
)

@Suppress("unused")
val sharpenLauncherIconPatch = patch(
    name = "Sharpen launcher icon",
    description = "Sharpens the default launcher icon. Alternate icons are unchanged.",
) {
    compatibleWith(AppCompatibilities.SERIALIZD)

    execute {
        val splash = PngImage.read(resourceFile(SPLASH_IMAGE))
        val mark = splash.findLogoMark()

        val foregrounds = listApkEntries("res/mipmap-")
            .filter { it.endsWith(FOREGROUND_FILE) }
            .mapNotNull { entry -> foregroundSize(entry)?.let { entry to it } }
        if (foregrounds.isEmpty()) throw PatchException("Could not find the launcher icon foreground")

        foregrounds.forEach { (entry, size) -> splash.resampleLayer(mark, size).write(resourceFile(entry)) }
    }
}

private fun foregroundSize(entry: String): Int? =
    foregroundSizeByDensity[densityQualifier.find(entry)?.groupValues?.get(1)]
