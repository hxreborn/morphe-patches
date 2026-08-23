/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.cx.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.resourceLiteral
import com.android.tools.smali.dexlib2.Opcode

internal object ThemePreferenceFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            parameters = listOf("Landroid/content/Context;"),
            returnType = "I",
            opcode = Opcode.INVOKE_STATIC,
        ),
        resourceLiteral(ResourceType.ARRAY, "night_mode_entries_no_auto"),
    ),
)
