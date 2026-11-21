/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val imoGmsCoreSupportExtensionPatch = patch {
    extendWith("extensions/hxreborn/imo-gms-core-support.mpe")
}

internal val imoHideVoiceClubTabExtensionPatch = patch {
    extendWith("extensions/hxreborn/imo-hide-voice-club-tab.mpe")
}

internal val imoSendLongMessagesExtensionPatch = patch {
    extendWith("extensions/hxreborn/imo-send-long-messages.mpe")
}
