/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.analytics

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val APPSFLYER_CLASS = "Lcom/appsflyer/AppsFlyerLib;"

internal object AppsFlyerStartFingerprint : Fingerprint(
    definingClass = "Lcom/imo/android/",
    filters = listOf(methodCall(definingClass = APPSFLYER_CLASS, name = "start")),
)

internal object AppsFlyerEventFingerprint : Fingerprint(
    definingClass = "Lcom/imo/android/",
    filters = listOf(methodCall(definingClass = APPSFLYER_CLASS, name = "logEvent")),
)

internal object FirebaseCollectionSettingFingerprint : Fingerprint(
    definingClass = "Lcom/imo/android/imoim/setting/BootAlwaysSettingsDelegate;",
    name = "getAnalyticsCollectionenabled",
)

internal object MonitorEventFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/util/HashMap;", "L"),
    strings = listOf("monitor", "log_event"),
)

internal object StatEventFingerprint : Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    strings = listOf("send_media_im", "bundle_stat", "msg_type"),
)

internal object AdvertisingIdUploadFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(fieldAccess(name = "AD_ID_LAST_CHECK_TS", opcode = Opcode.SGET_OBJECT)),
)
