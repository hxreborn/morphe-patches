/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.alpinequest.misc.fix.signature

import app.morphe.patcher.Fingerprint

internal object HasValidSignatureFingerprint : Fingerprint(
    strings = listOf("doesHaveValidSignature"),
)
