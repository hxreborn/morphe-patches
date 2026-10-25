/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.castletv.misc.fix.signature

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

internal const val REQUEST_SIGNING_LOG = "differenceTime==="
internal const val SECURITY_KEY_PREFERENCE = "server_security_key"
private const val GUID_PREFERENCE = "service_oauth_device_id"
internal const val STRING_CLASS = "Ljava/lang/String;"
internal const val SIGNATURE_CLASS = "Landroid/content/pm/Signature;"
internal const val CIPHER_CLASS = "Ljavax/crypto/Cipher;"

internal object SignatureDigestFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = STRING_CLASS,
    parameters = listOf(SIGNATURE_CLASS, STRING_CLASS),
    filters = listOf(
        methodCall(definingClass = SIGNATURE_CLASS, name = "toByteArray"),
        methodCall(definingClass = "Ljava/security/MessageDigest;", name = "digest"),
    ),
    strings = listOf("error!"),
)

internal object RepositoryGuidFingerprint : Fingerprint(
    returnType = STRING_CLASS,
    parameters = listOf(),
    filters = listOf(
        string(GUID_PREFERENCE),
        string("getServiceOauthDeviceId Guid="),
    ),
)

internal object ApplicationGuidFingerprint : Fingerprint(
    definingClass = "Lcom/xunbai/daqiantvpro/app/DqApplication;",
    returnType = STRING_CLASS,
    parameters = listOf(),
    filters = listOf(string(GUID_PREFERENCE)),
)

internal object HttpInterceptorFingerprint : Fingerprint(
    name = "intercept",
    filters = listOf(
        string(REQUEST_SIGNING_LOG),
        string(SECURITY_KEY_PREFERENCE),
        methodCall(definingClass = CIPHER_CLASS, name = "doFinal"),
    ),
)
