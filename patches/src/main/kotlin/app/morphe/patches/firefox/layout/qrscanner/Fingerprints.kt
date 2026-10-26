/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from fork-maintainers/iceraven-browser (MPL-2.0):
 * https://github.com/fork-maintainers/iceraven-browser/commit/56c08e8000c4e16c88010ddf2e991016ddb50a8c
 * Commit 56c08e8000c4e16c88010ddf2e991016ddb50a8c (2026-10-20),
 * app/src/main/java/org/mozilla/fenix/search/BrowserToolbarSearchMiddleware.kt
 */
package app.morphe.patches.firefox.layout.qrscanner

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

internal const val LIST_BUILDER = "Lkotlin/collections/builders/ListBuilder;"

internal object QrScannerButtonFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(
            opcode = Opcode.SGET,
            definingClass = "Lmozilla/components/ui/icons/R\$drawable;",
            name = "mozac_ic_qr_code_24",
        ),
        methodCall(
            definingClass = "Lmozilla/components/compose/browser/toolbar/concept/Action\$ActionButtonRes;",
            name = "<init>",
            location = MatchAfterWithin(10),
        ),
        methodCall(definingClass = LIST_BUILDER, name = "add", location = MatchAfterImmediately()),
    ),
)
