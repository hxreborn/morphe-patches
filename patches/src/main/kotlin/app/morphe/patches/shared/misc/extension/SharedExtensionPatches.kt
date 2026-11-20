/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val sharedActivityCallbacksAdapterExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/shared-activity-callbacks-adapter.mpe")
}

internal val sharedAdmobExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/shared-admob.mpe")
}

internal val sharedSignInTokenExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/shared-sign-in-token.mpe")
}

internal val sharedDimensionsExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/shared-dimensions.mpe")
}

internal val sharedUnlockPremiumExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/shared-unlock-premium.mpe")
}

internal val sharedGmsSignInExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/shared-gms-sign-in.mpe")
}

internal val sharedInstalledCertificateExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/shared-installed-certificate.mpe")
}

internal val sharedProxyHandlerExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/shared-proxy-handler.mpe")
}

internal val sharedLiappPackageFilterExtensionPatch = bytecodePatch {
    dependsOn(sharedProxyHandlerExtensionPatch)
    extendWith("extensions/hxreborn/shared-liapp-package-filter.mpe")
}

internal val sharedNightModeExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/shared-night-mode.mpe")
}

internal val sharedRevenuecatExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/shared-revenuecat.mpe")
}

internal val sharedSecurityContextExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/shared-security-context.mpe")
}

internal val sharedSignatureExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/shared-signature.mpe")
}

internal val sharedStreamsExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/shared-streams.mpe")
}

internal val sharedRemoveScreenshotRestrictionExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/shared-remove-screenshot-restriction.mpe")
}
