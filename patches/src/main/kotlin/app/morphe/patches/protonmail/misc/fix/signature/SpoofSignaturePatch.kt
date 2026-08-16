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

private const val APPLICATION_CLASS = "Lch/protonmail/android/App;"
private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/protonmail/SpoofSignature;"

@Suppress("unused")
val spoofSignaturePatch = bytecodePatch(
    name = "Spoof signature",
    description = "Restores push notifications by spoofing the original app signature.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)
    extendWith("extensions/extension.mpe")

    execute {
        mutableClassDefBy(APPLICATION_CLASS).setSuperClass(EXTENSION_CLASS)
    }
}
