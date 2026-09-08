/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.gstarmc.misc.jiagu

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.misc.jiagu.asJiaguDex
import app.morphe.patches.shared.misc.signature.stockSigningCertificate
import app.morphe.util.inputStreamFromBundledResource
import java.security.MessageDigest

private const val STOCK_STUB_SHA256 =
    "a52a950fc2a7a89aac937fc73b6f9a7fcd54fd9a7d75a669fff9c2ff9855b183"

private const val CONFIGURATION_ASSET = "assets/hcfg"

internal fun ResourcePatchContext.enableRuntimeFeature(flag: String) {
    val configuration = get(CONFIGURATION_ASSET, copy = false)
    val enabled = if (configuration.exists()) configuration.readText().lines() else emptyList()

    if (flag in enabled) return

    configuration.writeText((enabled.filter { it.isNotBlank() } + flag).joinToString("\n"))
}

internal fun bundledResource(name: String) =
    (inputStreamFromBundledResource("gstarmc", name)
        ?: throw PatchException("The bundle is missing gstarmc/$name"))
        .use { it.readBytes() }

val jiaguRuntimePatch = rawResourcePatch {
    execute {
        val dex = get("classes.dex").let { it to it.readBytes().asJiaguDex() }

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(dex.second.stub)
            .joinToString("") { "%02x".format(it) }

        if (digest != STOCK_STUB_SHA256) {
            throw PatchException(
                "The packer stub does not match the app version this patch was built for",
            )
        }

        dex.first.writeBytes(dex.second.withStub(bundledResource("stub.dex")))

        get("assets/h", copy = false).writeBytes(bundledResource("boot.dex"))
        get("assets/hc", copy = false).writeBytes(packageMetadata.stockSigningCertificate().encoded)
    }
}
