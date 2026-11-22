/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.gms

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal const val LISTENER_CLASS = "Lcom/mars/united/international/passport/OnThirdLoginResultListener;"

internal object GoogleLoginFingerprint : Fingerprint(
    definingClass = "Lcom/mars/united/international/passport/service/GoogleSSOLoginService;",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", LISTENER_CLASS),
)

internal object CredentialLoginFingerprint : Fingerprint(
    definingClass = "Lcom/mars/united/international/passport/service/CredentialManagerSSOLoginService;",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", LISTENER_CLASS, "Z", "J"),
)

internal object AutomaticGoogleLoginFingerprint : Fingerprint(
    definingClass = "Lcom/dubox/drive/login/ui/fragment/AccountFragment;",
    name = "autoGoogleCredentialLogin",
    returnType = "V",
    parameters = listOf("Lcom/dubox/drive/login/ui/activity/AccountWebViewActivity;"),
)
