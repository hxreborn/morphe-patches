/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from MorpheApp/morphe-patches:
 * https://github.com/MorpheApp/morphe-patches/commit/99d0f41e5ca1a043d8b512556b077acf35ba5db6
 * Commit 99d0f41e5ca1a043d8b512556b077acf35ba5db6 (2026-07-25),
 * patches/src/main/kotlin/app/morphe/patches/reddit/misc/fix/signature/SpoofSignaturePatch.kt
 */
package app.morphe.patches.protonmail.misc.fix.signature

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.spoofSignature

private const val APPLICATION_CLASS = "Lch/protonmail/android/App;"

val spoofSignaturePatch = bytecodePatch {
    compatibleWith(AppCompatibilities.PROTON_MAIL)
    extendWith("extensions/extension.mpe")

    execute {
        spoofSignature(APPLICATION_CLASS)
    }
}
