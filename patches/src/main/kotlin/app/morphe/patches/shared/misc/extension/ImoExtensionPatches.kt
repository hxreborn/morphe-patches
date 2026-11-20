/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val imoGmsCoreSupportExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/imo-gms-core-support.mpe")
}

internal val imoHideVoiceClubTabExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/imo-hide-voice-club-tab.mpe")
}

internal val imoSendLongMessagesExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/imo-send-long-messages.mpe")
}
