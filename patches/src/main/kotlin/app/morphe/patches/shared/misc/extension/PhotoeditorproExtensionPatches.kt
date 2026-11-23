/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val photoeditorproSettingsExtensionPatch = patch {
    dependsOn(sharedPatchesMenuExtensionPatch, morpheLibraryCoreExtensionPatch, sharedActivityCallbacksAdapterExtensionPatch, sharedDimensionsExtensionPatch, sharedSignatureExtensionPatch)
    extendWith("extensions/hxreborn/photoeditorpro-settings.mpe")
}

internal val photoeditorproInspectAiRequestsExtensionPatch = patch {
    dependsOn(photoeditorproSettingsExtensionPatch)
    extendWith("extensions/hxreborn/photoeditorpro-inspect-ai-requests.mpe")
}

internal val photoeditorproPatchGatesExtensionPatch = patch {
    dependsOn(morpheLibraryCoreExtensionPatch, photoeditorproSettingsExtensionPatch)
    extendWith("extensions/hxreborn/photoeditorpro-patch-gates.mpe")
}

internal val photoeditorproDisablePushNotificationsExtensionPatch = patch {
    extendWith("extensions/hxreborn/photoeditorpro-disable-push-notifications.mpe")
}

internal val photoeditorproHideShareOptionsExtensionPatch = patch {
    dependsOn(morpheLibraryCoreExtensionPatch, photoeditorproSettingsExtensionPatch)
    extendWith("extensions/hxreborn/photoeditorpro-hide-share-options.mpe")
}
