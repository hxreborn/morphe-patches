/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ridmik.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object KeyboardPremiumStatusFingerprint : Fingerprint(
    name = "isCurrentUserActuallySubscribed",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = emptyList(),
)

internal object AppPremiumStatusFingerprint : Fingerprint(
    name = "isUserSubscribed",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Landroid/content/Context;"),
)

internal object EmojiSearchAdLoadFingerprint : Fingerprint(
    name = "manageEmojiSearchAdsLoad",
    returnType = "V",
    parameters = emptyList(),
)

internal object StoreAdRowFingerprint : Fingerprint(
    name = "setAdsEnable",
    returnType = "V",
    parameters = listOf("Z"),
)

internal object GifAdsEnabledFingerprint : Fingerprint(
    filters = listOf(
        string("klipy_gif_ads_enable_key"),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "Z"),
            returnType = "Z",
            location = MatchAfterWithin(2),
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)
