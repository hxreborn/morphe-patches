/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val viberSettingsExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/viber-settings.mpe")
}

internal val viberDisableAiFeaturesExtensionPatch = bytecodePatch {
    dependsOn(viberSettingsExtensionPatch)
    extendWith("extensions/hxreborn/viber-disable-ai-features.mpe")
}

internal val viberFilterBusinessChatsExtensionPatch = bytecodePatch {
    dependsOn(viberSettingsExtensionPatch)
    extendWith("extensions/hxreborn/viber-filter-business-chats.mpe")
}

internal val viberRemoveChatPaymentActionsExtensionPatch = bytecodePatch {
    dependsOn(viberSettingsExtensionPatch)
    extendWith("extensions/hxreborn/viber-remove-chat-payment-actions.mpe")
}

internal val viberGmsCoreSupportExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/viber-gms-core-support.mpe")
}

internal val viberDisappearingMediaPrivacyExtensionPatch = bytecodePatch {
    dependsOn(viberSettingsExtensionPatch)
    extendWith("extensions/hxreborn/viber-disappearing-media-privacy.mpe")
}

internal val viberRemoveListGapsExtensionPatch = bytecodePatch {
    dependsOn(viberSettingsExtensionPatch)
    extendWith("extensions/hxreborn/viber-remove-list-gaps.mpe")
}

internal val viberUnfiledChatsExtensionPatch = bytecodePatch {
    dependsOn(viberSettingsExtensionPatch)
    extendWith("extensions/hxreborn/viber-unfiled-chats.mpe")
}
