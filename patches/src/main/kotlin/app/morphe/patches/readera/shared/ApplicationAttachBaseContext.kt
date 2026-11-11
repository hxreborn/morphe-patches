/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.shared

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.misc.smali.addMethod
import com.android.tools.smali.dexlib2.AccessFlags

private const val APPLICATION_CLASS = "Lorg/readera/App;"
internal const val CONTEXT_CLASS = "Landroid/content/Context;"

internal fun BytecodePatchContext.getOrAddApplicationAttachBaseContext(): MutableMethod =
    mutableClassDefBy(APPLICATION_CLASS).let { application ->
        application.methods.singleOrNull { method ->
            method.name == "attachBaseContext" && method.parameterTypes.map(CharSequence::toString) == listOf(CONTEXT_CLASS)
        } ?: application.addMethod(
            "attachBaseContext", listOf(CONTEXT_CLASS), "V", AccessFlags.PROTECTED.value, 2,
            """
                invoke-super { p0, p1 }, ${application.superclass}->attachBaseContext($CONTEXT_CLASS)V
                return-void
            """,
        )
    }
