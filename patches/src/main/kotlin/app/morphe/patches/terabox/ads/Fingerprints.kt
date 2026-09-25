/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object HasPrivilegeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("I"),
    filters = listOf(
        methodCall(definingClass = "Lcom/dubox/drive/vip/model/VipInfo;", name = "getPrivileges"),
        methodCall(definingClass = "Lcom/dubox/drive/vip/domain/job/server/response/PrivilegeInfo;", name = "getType"),
        methodCall(definingClass = "Lcom/dubox/drive/vip/domain/job/server/response/PrivilegeInfo;", name = "isValid"),
    ),
)

internal object AdSdkInitFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("key_ad_init_count"),
)
