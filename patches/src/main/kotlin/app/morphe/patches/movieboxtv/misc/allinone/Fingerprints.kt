/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.movieboxtv.misc.allinone

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.reference.StringReference

internal const val COUNTRY_HEADER_TAG = "CountryIsoCodeHeader"
internal const val RESPONSE_TEXT = "Response{protocol="
internal const val REQUEST_TEXT = "Request{method="

internal val qualityStreamAnchors = listOf(COUNTRY_HEADER_TAG, RESPONSE_TEXT, REQUEST_TEXT)

internal fun List<ClassDef>.classWithStringLiteral(value: String): ClassDef {
    val matches = flatMap { classDef ->
        classDef.methods.filter { method ->
            method.implementation?.instructions?.any { it.getReference<StringReference>()?.string == value } == true
        }.map { classDef }
    }
    return matches.singleOrNull()
        ?: throw PatchException("Expected one payload method loading \"$value\", found ${matches.size}")
}
