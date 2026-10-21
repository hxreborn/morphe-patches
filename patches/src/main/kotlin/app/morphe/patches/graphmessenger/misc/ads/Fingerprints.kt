/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.graphmessenger.misc.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patches.graphmessenger.shared.licenseCheck

internal object LicenseCheckFingerprint : Fingerprint(
    filters = listOf(licenseCheck),
)

internal object AdSdkInitFingerprint : Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/applovin/sdk/AppLovinPrivacySettings;",
            name = "setHasUserConsent",
        ),
    ),
)

internal object SponsoredMessagesReadParamsFingerprint : Fingerprint(
    definingClass = "Lorg/telegram/tgnet/TLRPC\$TL_messages_sponsoredMessages;",
    name = "readParams",
)

internal object SponsoredPeersReadParamsFingerprint : Fingerprint(
    definingClass = "Lorg/telegram/tgnet/TLRPC\$TL_contacts_sponsoredPeers;",
    name = "readParams",
)

internal object PromoDataRequestFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(newInstance("Lorg/telegram/tgnet/TLRPC\$TL_help_getPromoData;")),
)
