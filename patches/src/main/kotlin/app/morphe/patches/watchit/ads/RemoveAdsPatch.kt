/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.watchit.ads

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.watchit.misc.piracy.disablePiracyCheckPatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue

private const val SERIALIZED_NAME_TYPE = "Lcom/google/gson/annotations/SerializedName;"

private val playerAdKeys = setOf("video_ads_id", "overlay", "overlays", "pause_ad", "interactive_ads")
private val playerAdAnchorKeys = setOf("video_ads_id", "interactive_ads")
private val advertiserLogoKeys = setOf("bar", "rectangle", "square")

private val Field.serializedName: String?
    get() = annotations.firstOrNull { it.type == SERIALIZED_NAME_TYPE }
        ?.elements?.firstOrNull { it.name == "value" }
        ?.let { (it.value as? StringEncodedValue)?.value }

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes video ads, pause ads, sponsor overlays, interactive ads and advertiser logos.",
) {
    compatibleWith(AppCompatibilities.WATCH_IT)

    dependsOn(disablePiracyCheckPatch)

    execute {
        val transientKeys = mutableSetOf<String>()

        classDefForEach { classDef ->
            val keys = classDef.fields.mapNotNull { it.serializedName }.toSet()
            val adKeys = when {
                keys.any { it in playerAdAnchorKeys } -> playerAdKeys
                keys == advertiserLogoKeys -> advertiserLogoKeys
                else -> return@classDefForEach
            }

            mutableClassDefBy(classDef).fields.forEach { field ->
                val key = field.serializedName?.takeIf { it in adKeys } ?: return@forEach
                field.setAccessFlags(field.accessFlags or AccessFlags.TRANSIENT.value)
                transientKeys += key
            }
        }

        val missingKeys = playerAdKeys + advertiserLogoKeys - transientKeys
        if (missingKeys.isNotEmpty()) throw PatchException("Ad fields not found: $missingKeys")
    }
}
