/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val castleDeviceGuidExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/castle-device-guid.mpe")
}

internal val castleFullHdPlaylistExtensionPatch = bytecodePatch {
    dependsOn(sharedStreamsExtensionPatch)
    extendWith("extensions/hxreborn/castle-full-hd-playlist.mpe")
}
