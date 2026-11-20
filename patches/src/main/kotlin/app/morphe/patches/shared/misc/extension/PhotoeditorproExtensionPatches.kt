/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val photoeditorproSettingsExtensionPatch = bytecodePatch {
    dependsOn(morpheLibraryCoreExtensionPatch, sharedActivityCallbacksAdapterExtensionPatch, sharedDimensionsExtensionPatch, sharedSignatureExtensionPatch)
    extendWith("extensions/hxreborn/photoeditorpro-settings.mpe")
}

internal val photoeditorproInspectAiRequestsExtensionPatch = bytecodePatch {
    dependsOn(photoeditorproSettingsExtensionPatch)
    extendWith("extensions/hxreborn/photoeditorpro-inspect-ai-requests.mpe")
}

internal val photoeditorproPatchGatesExtensionPatch = bytecodePatch {
    dependsOn(morpheLibraryCoreExtensionPatch, photoeditorproSettingsExtensionPatch)
    extendWith("extensions/hxreborn/photoeditorpro-patch-gates.mpe")
}

internal val photoeditorproDisablePushNotificationsExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/photoeditorpro-disable-push-notifications.mpe")
}

internal val photoeditorproHideShareOptionsExtensionPatch = bytecodePatch {
    dependsOn(morpheLibraryCoreExtensionPatch, photoeditorproSettingsExtensionPatch)
    extendWith("extensions/hxreborn/photoeditorpro-hide-share-options.mpe")
}
