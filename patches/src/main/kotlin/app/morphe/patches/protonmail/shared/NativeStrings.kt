/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.shared

import java.io.File

internal const val RUST_CORE = "libmail_uniffi.so"

internal fun File.replaceAsciiInPlace(old: String, new: String): Boolean {
    require(old.length == new.length) { "Replacement must keep the byte length" }

    val original = readBytes().toString(Charsets.ISO_8859_1)
    val patched = original.replace(old, new)

    if (patched == original) return false

    writeBytes(patched.toByteArray(Charsets.ISO_8859_1))
    return true
}
