/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.gstarmc.misc.jiagu

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.packageMetadata
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.morphe.patches.shared.misc.jiagu.asJiaguDex
import app.morphe.patches.shared.misc.jiagu.withLoader
import app.morphe.patches.shared.misc.signature.stockSigningCertificate
import app.morphe.util.inputStreamFromBundledResource

private const val CONFIGURATION_ASSET = "assets/hcfg"

internal fun enableRuntimeFeature(flag: String) {
    val configuration = resourceFile(CONFIGURATION_ASSET, copy = false)
    val enabled = if (configuration.exists()) configuration.readText().lines() else emptyList()

    if (flag in enabled) return

    configuration.writeText((enabled.filter { it.isNotBlank() } + flag).joinToString("\n"))
}

internal fun bundledResource(name: String) =
    (inputStreamFromBundledResource("gstarmc", name)
        ?: throw PatchException("Missing bundled resource: gstarmc/$name"))
        .use { it.readBytes() }

val jiaguRuntimePatch = patch {
    execute {
        val classes = resourceFile("classes.dex")
        classes.writeBytes(classes.readBytes().asJiaguDex().withLoader(bootLoaderSmali))

        resourceFile("assets/h", copy = false).writeBytes(bundledResource("boot.dex"))
        resourceFile("assets/hc", copy = false).writeBytes(packageMetadata.stockSigningCertificate().encoded)
        resourceFile("assets/hl", copy = false).writeBytes(bundledResource("hiddenapi.so"))
    }
}
