/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.firefox.misc.addons

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal object InstallLocalAddonRowFingerprint : Fingerprint(
    definingClass = "Lorg/mozilla/fenix/settings/SettingsFragment;",
    name = "onResume",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.SGET,
            definingClass = "Lorg/mozilla/fenix/R\$string;",
            name = "pref_key_install_local_addon",
        ),
        methodCall(
            definingClass = "Lorg/mozilla/fenix/settings/ExtensionsKt;",
            name = "requirePreference",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        methodCall(
            definingClass = "Lorg/mozilla/fenix/utils/Settings;",
            name = "getShowSecretDebugMenuThisSession",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)
