/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.movieboxtv.misc.quality

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

private const val RESOLUTION_MANAGER = "Lcom/transsion/tvplayer/TvResolutionManager;"
internal const val LIST = "Ljava/util/List;"
private const val STRING = "Ljava/lang/String;"
private const val STOCK_DEFAULT_RESOLUTION = "480"

internal object CountryHeaderFingerprint : Fingerprint(
    strings = listOf("CountryIsoCodeHeader"),
)

internal object ResponseToStringFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("Response{protocol="),
)

internal object RequestToStringFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("Request{method="),
)

internal object DefaultStreamFingerprint : Fingerprint(
    definingClass = RESOLUTION_MANAGER,
    isStatic = false,
    parameters = listOf(LIST),
    filters = listOf(
        methodCall(definingClass = RESOLUTION_MANAGER, parameters = listOf(LIST), returnType = LIST),
        methodCall(definingClass = RESOLUTION_MANAGER, parameters = listOf(), returnType = STRING),
        methodCall(parameters = listOf(), returnType = STRING),
        string(STOCK_DEFAULT_RESOLUTION),
    ),
)
