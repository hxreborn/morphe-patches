/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.perplexity.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags

internal fun BytecodePatchContext.returnUnitEarly(method: MutableMethod) {
    val unitClass = KotlinUnitToStringFingerprint.matchSingle().classDef
    val unitType = unitClass.type
    val instanceField = unitClass.fields.single {
        AccessFlags.STATIC.isSet(it.accessFlags) && it.type == unitType
    }

    method.addInstructions(
        0,
        """
            sget-object v0, $unitType->${instanceField.name}:$unitType
            return-object v0
        """,
    )
}
