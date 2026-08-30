/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allinonecalculator.misc.gms

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal const val DELEGATE_CLASS =
    "Lio/flutter/plugins/googlesignin/GoogleSignInPlugin\$Delegate;"

internal object GetCredentialFingerprint : Fingerprint(
    definingClass = DELEGATE_CLASS,
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    strings = listOf("CredentialManager requires a serverClientId."),
)
