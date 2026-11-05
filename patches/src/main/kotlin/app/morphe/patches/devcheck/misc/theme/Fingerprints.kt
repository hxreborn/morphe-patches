/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.devcheck.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import com.android.tools.smali.dexlib2.Opcode

private const val THEME_DIALOG_MIN_SDK = 29L

private fun themeSdkFilters() = listOf(
    resourceLiteral(ResourceType.ID, "action_themes"),
    fieldAccess(definingClass = "Landroid/os/Build\$VERSION;", name = "SDK_INT", type = "I"),
    literal(THEME_DIALOG_MIN_SDK),
)

internal object ThemesMenuFingerprint : Fingerprint(
    name = "onCreateOptionsMenu",
    returnType = "Z",
    parameters = listOf("Landroid/view/Menu;"),
    filters = themeSdkFilters(),
)

internal object ThemesDialogFingerprint : Fingerprint(
    name = "onOptionsItemSelected",
    returnType = "Z",
    parameters = listOf("Landroid/view/MenuItem;"),
    filters = themeSdkFilters() + listOf(
        resourceLiteral(ResourceType.STRING, "sys_theme"),
        methodCall(
            definingClass = "Landroid/app/Dialog;",
            name = "show",
            parameters = emptyList(),
            returnType = "V",
            opcode = Opcode.INVOKE_VIRTUAL,
        ),
    ),
)

internal fun contentViewFingerprint(activityType: String) = Fingerprint(
    definingClass = activityType,
    name = "setContentView",
    returnType = "V",
)
