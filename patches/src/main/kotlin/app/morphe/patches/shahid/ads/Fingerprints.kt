/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shahid.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patches.shahid.shared.PlayoutMediaToStringFingerprint
import app.morphe.patches.shahid.shared.PlayoutToStringFingerprint
import app.morphe.patches.shahid.shared.fieldGetterFingerprint

internal val PlayoutAdvertisementsFingerprint = fieldGetterFingerprint(PlayoutToStringFingerprint, "advertisements")

internal val PlayoutNativeAdvertisementsFingerprint =
    fieldGetterFingerprint(PlayoutToStringFingerprint, "nativeAdvertisements")

internal val PlayoutMediaSsaiFingerprint = fieldGetterFingerprint(PlayoutMediaToStringFingerprint, "ssai")

internal object AdLoaderLoadFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L"),
    strings = listOf("Failed to load ad."),
)
