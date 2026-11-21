/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val allvideoplayerResumeVideosOpenedFromOtherAppsExtensionPatch = patch {
    extendWith("extensions/hxreborn/allvideoplayer-resume-videos-opened-from-other-apps.mpe")
}
