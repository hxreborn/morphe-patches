/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.movieboxtv.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

private const val SERVICE_LOCATOR = "Lcom/transsion/tvdata/TvServiceLocator;"
internal const val BEAN_PACKAGE = "Lcom/transsion/tvdata/bean/"
internal const val BOOLEAN = "Ljava/lang/Boolean;"

internal object ServiceLocatorIsVipFingerprint : Fingerprint(
    definingClass = SERVICE_LOCATOR,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(methodCall(returnType = "Z", parameters = listOf())),
)

internal object VipHeadersFingerprint : Fingerprint(
    definingClass = SERVICE_LOCATOR,
    returnType = "V",
    parameters = listOf(BOOLEAN, "Ljava/lang/String;"),
    filters = listOf(string("x-vip"), string("x-vip-level")),
)

internal object VipGetterFingerprint : Fingerprint(
    name = "isVip",
    parameters = listOf(),
    custom = { _, classDef -> classDef.type.startsWith(BEAN_PACKAGE) },
)

internal object RegionBlockFingerprint : Fingerprint(
    definingClass = "${BEAN_PACKAGE}BffVisitorLoginData;",
    name = "getRegionBlock",
    returnType = BOOLEAN,
    parameters = listOf(),
)

internal fun vipStateInitialValueFingerprint(state: String) = Fingerprint(
    definingClass = state,
    name = "<clinit>",
    filters = listOf(fieldAccess(definingClass = BOOLEAN, name = "FALSE", type = BOOLEAN)),
)

internal fun vipStateWriterFingerprint(state: String) = Fingerprint(
    definingClass = state,
    returnType = "V",
    parameters = listOf("Z"),
)
