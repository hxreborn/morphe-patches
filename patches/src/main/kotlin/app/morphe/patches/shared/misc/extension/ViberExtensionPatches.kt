/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val viberSettingsExtensionPatch = patch {
    dependsOn(sharedPatchesMenuExtensionPatch, sharedHostedComponentsExtensionPatch)
    extendWith("extensions/hxreborn/viber-settings.mpe")
}

internal val viberDisableAiFeaturesExtensionPatch = patch {
    dependsOn(viberSettingsExtensionPatch)
    extendWith("extensions/hxreborn/viber-disable-ai-features.mpe")
}

internal val viberFilterBusinessChatsExtensionPatch = patch {
    dependsOn(viberSettingsExtensionPatch)
    extendWith("extensions/hxreborn/viber-filter-business-chats.mpe")
}

internal val viberRemoveChatPaymentActionsExtensionPatch = patch {
    dependsOn(viberSettingsExtensionPatch)
    extendWith("extensions/hxreborn/viber-remove-chat-payment-actions.mpe")
}

internal val viberGmsCoreSupportExtensionPatch = patch {
    extendWith("extensions/hxreborn/viber-gms-core-support.mpe")
}

internal val viberDisappearingMediaPrivacyExtensionPatch = patch {
    dependsOn(viberSettingsExtensionPatch)
    extendWith("extensions/hxreborn/viber-disappearing-media-privacy.mpe")
}

internal val viberRemoveListGapsExtensionPatch = patch {
    dependsOn(viberSettingsExtensionPatch)
    extendWith("extensions/hxreborn/viber-remove-list-gaps.mpe")
}

internal val viberUnfiledChatsExtensionPatch = patch {
    dependsOn(viberSettingsExtensionPatch)
    extendWith("extensions/hxreborn/viber-unfiled-chats.mpe")
}
