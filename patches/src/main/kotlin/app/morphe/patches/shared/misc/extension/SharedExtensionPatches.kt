/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val sharedActivityCallbacksAdapterExtensionPatch = patch {
    extendWith("extensions/hxreborn/shared-activity-callbacks-adapter.mpe")
}

internal val sharedAdmobExtensionPatch = patch {
    extendWith("extensions/hxreborn/shared-admob.mpe")
}

internal val sharedSignInTokenExtensionPatch = patch {
    extendWith("extensions/hxreborn/shared-sign-in-token.mpe")
}

internal val sharedDimensionsExtensionPatch = patch {
    extendWith("extensions/hxreborn/shared-dimensions.mpe")
}

internal val sharedUnlockPremiumExtensionPatch = patch {
    extendWith("extensions/hxreborn/shared-unlock-premium.mpe")
}

internal val sharedGmsSignInExtensionPatch = patch {
    extendWith("extensions/hxreborn/shared-gms-sign-in.mpe")
}

internal val sharedInstalledCertificateExtensionPatch = patch {
    extendWith("extensions/hxreborn/shared-installed-certificate.mpe")
}

internal val sharedProxyHandlerExtensionPatch = patch {
    extendWith("extensions/hxreborn/shared-proxy-handler.mpe")
}

internal val sharedLiappPackageFilterExtensionPatch = patch {
    dependsOn(sharedProxyHandlerExtensionPatch)
    extendWith("extensions/hxreborn/shared-liapp-package-filter.mpe")
}

internal val sharedNightModeExtensionPatch = patch {
    extendWith("extensions/hxreborn/shared-night-mode.mpe")
}

internal val sharedRevenuecatExtensionPatch = patch {
    extendWith("extensions/hxreborn/shared-revenuecat.mpe")
}

internal val sharedSecurityContextExtensionPatch = patch {
    extendWith("extensions/hxreborn/shared-security-context.mpe")
}

internal val sharedSignatureExtensionPatch = patch {
    extendWith("extensions/hxreborn/shared-signature.mpe")
}

internal val sharedStreamsExtensionPatch = patch {
    extendWith("extensions/hxreborn/shared-streams.mpe")
}

internal val sharedRemoveScreenshotRestrictionExtensionPatch = patch {
    extendWith("extensions/hxreborn/shared-remove-screenshot-restriction.mpe")
}

internal val sharedPatchesMenuExtensionPatch = patch {
    extendWith("extensions/hxreborn/shared-patches-menu.mpe")
}
