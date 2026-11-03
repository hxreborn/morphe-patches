/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.duolingo.misc.icon

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

private const val APP_ICON_PACKAGE = "Lcom/duolingo/feature/appicon"

private const val DEFAULT_DISPLAY_WRITE_WINDOW = 2

internal object AppIconSwitchFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("L", "$APP_ICON_PACKAGE/AppIconHelper\$Origin;", "L"),
    filters = listOf(
        methodCall(definingClass = "Landroid/content/pm/PackageManager;", name = "setComponentEnabledSetting"),
    ),
)

internal object DefaultAppDisplayFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET_OBJECT, definingClass = "$APP_ICON_PACKAGE/AppIconType;", name = "DEFAULT"),
        fieldAccess(
            opcode = Opcode.SGET_OBJECT,
            definingClass = "$APP_ICON_PACKAGE/AppDisplayName;",
            name = "DEFAULT",
            location = MatchAfterImmediately(),
        ),
        fieldAccess(opcode = Opcode.SPUT_OBJECT, location = MatchAfterWithin(DEFAULT_DISPLAY_WRITE_WINDOW)),
    ),
)
