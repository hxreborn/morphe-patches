/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from fork-maintainers/iceraven-browser (MPL-2.0):
 * https://github.com/fork-maintainers/iceraven-browser/commit/56c08e8000c4e16c88010ddf2e991016ddb50a8c
 * Commit 56c08e8000c4e16c88010ddf2e991016ddb50a8c (2026-10-20),
 * app/src/main/java/org/mozilla/fenix/browser/TabPreview.kt
 */
package app.morphe.patches.firefox.layout.url

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

private fun displayUrlCall(location: MatchAfterWithin) = listOf(
    methodCall(definingClass = "Lmozilla/components/support/ktx/util/URLStringUtils;", name = "toDisplayUrl", location = location),
    opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
)

internal object ToolbarDisplayUrlFingerprint : Fingerprint(
    filters = listOf(
        string("about:home"),
        methodCall(definingClass = "Lkotlin/text/StringsKt___StringsJvmKt;", name = "isBlank", location = MatchAfterWithin(5)),
    ) + displayUrlCall(MatchAfterWithin(6)),
)

internal object TabPreviewDisplayUrlFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            definingClass = "Lmozilla/components/support/ktx/util/RegistrableDomainSpanKt;",
            name = "applyRegistrableDomainSpan",
        ),
    ) + displayUrlCall(MatchAfterWithin(8)) + fieldAccess(
        opcode = Opcode.SGET,
        definingClass = "Lorg/mozilla/fenix/R\$string;",
        name = "search_hint",
        location = MatchAfterWithin(6),
    ),
)
