/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.aitools

import app.morphe.patcher.Fingerprint

internal object UploadChunkSizeFingerprint : Fingerprint(
    strings = listOf("Increasing chunk size to "),
)
