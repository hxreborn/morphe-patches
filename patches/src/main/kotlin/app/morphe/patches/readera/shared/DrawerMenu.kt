/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.shared

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

internal const val DRAWER_QUOTES_ITEM = 5
internal const val DRAWER_DICTIONARY_ITEM = 8
private const val DRAWER_GROUP_SHOWN_CALL = 2

internal fun BytecodePatchContext.showDrawerItem(item: Int) {
    val drawer = DrawerMenuVisibilityFingerprint
    val visibleRegister = drawer.instructionMatches[DRAWER_GROUP_SHOWN_CALL].getInstruction<FiveRegisterInstruction>().registerE
    val hide = drawer.instructionMatches[item]
    drawer.method.replaceInstruction(
        hide.index,
        "invoke-interface { v${hide.getInstruction<FiveRegisterInstruction>().registerC}, v$visibleRegister }, " +
            "Landroid/view/MenuItem;->setVisible(Z)Landroid/view/MenuItem;",
    )
}
