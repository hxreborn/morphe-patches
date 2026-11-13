/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.ads

import app.morphe.patcher.Fingerprint

internal object AdSdkInitFingerprint : Fingerprint(
    definingClass = "Lcom/king/abm/adprovider/AdProviderSDKAndroid;",
    name = "init",
    returnType = "V",
    parameters = listOf("J", "[Ljava/lang/String;"),
)

internal object AdSdkCollectBidderTokenFingerprint : Fingerprint(
    definingClass = "Lcom/king/abm/adprovider/AdProviderSDKAndroid;",
    name = "collectBidderToken",
    returnType = "V",
    parameters = listOf("I"),
)

internal object AdLoadFingerprint : Fingerprint(
    definingClass = "Lcom/king/abm/adprovider/AdProviderModuleAndroid;",
    name = "load",
    returnType = "V",
    parameters = listOf("J", "[Ljava/lang/String;", "[Ljava/lang/String;"),
)
