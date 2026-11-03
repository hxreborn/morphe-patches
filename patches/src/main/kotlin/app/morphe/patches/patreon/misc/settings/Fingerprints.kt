/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.misc.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

internal val settingsNavigationRowParameters = listOf("I", "Lkotlin/jvm/functions/Function0;", "L", "I")

internal object AccountSettingsSectionFingerprint : Fingerprint(
    returnType = "Lkotlin/Unit;",
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            returnType = "V",
            parameters = listOf("I", "Ljava/lang/String;", "Lkotlin/jvm/functions/Function0;", "L", "I"),
        ),
        methodCall(opcode = Opcode.INVOKE_STATIC, returnType = "V", parameters = settingsNavigationRowParameters),
    ),
    custom = { _, classDef -> classDef.sourceFile == "SettingsHomeScreen.kt" },
)

