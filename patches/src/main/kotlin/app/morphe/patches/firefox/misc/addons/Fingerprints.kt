/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from fork-maintainers/iceraven-browser (MPL-2.0):
 * https://github.com/fork-maintainers/iceraven-browser/commit/56c08e8000c4e16c88010ddf2e991016ddb50a8c
 * Commit 56c08e8000c4e16c88010ddf2e991016ddb50a8c (2026-10-20),
 * app/src/main/java/io/github/forkmaintainers/iceraven/components/PagedAMOAddonsProvider.kt
 */
package app.morphe.patches.firefox.misc.addons

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object CollectionPageFingerprint : Fingerprint(
    filters = listOf(
        string("/addons/?page_size=50&sort="),
        methodCall(definingClass = "Lmozilla/components/concept/fetch/Response\$Body;", name = "string"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.NEW_INSTANCE, MatchAfterImmediately()),
        methodCall(definingClass = "Lorg/json/JSONObject;", name = "<init>", location = MatchAfterImmediately()),
    ),
)

internal object DefaultCollectionNameFingerprint : Fingerprint(
    filters = listOf(
        string("Extensions-for-Android"),
        methodCall(definingClass = "Lmozilla/components/feature/addons/amo/AMOAddonsProvider;", name = "<init>"),
    ),
)

internal object DefaultCollectionUserFingerprint : Fingerprint(
    filters = listOf(
        string("mozilla"),
        methodCall(definingClass = "Lmozilla/components/feature/addons/amo/AMOAddonsProvider;", name = "<init>"),
    ),
)
