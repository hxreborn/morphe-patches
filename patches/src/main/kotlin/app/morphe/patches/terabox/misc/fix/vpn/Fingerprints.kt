/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.fix.vpn

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

private const val CONNECTIVITY_MANAGER = "Landroid/net/ConnectivityManager;"
internal const val NETWORK_CAPABILITIES = "Landroid/net/NetworkCapabilities;"
internal const val TRANSPORT_VPN = 4
private const val TRANSPORT_ETHERNET = 3

internal object NetworkConnectedFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        methodCall(definingClass = "Landroid/content/Context;", name = "getApplicationContext"),
        methodCall(definingClass = CONNECTIVITY_MANAGER, name = "getActiveNetwork"),
        methodCall(definingClass = CONNECTIVITY_MANAGER, name = "getNetworkCapabilities"),
        literal(TRANSPORT_ETHERNET),
        methodCall(definingClass = NETWORK_CAPABILITIES, name = "hasTransport", location = MatchAfterImmediately()),
    ),
)
