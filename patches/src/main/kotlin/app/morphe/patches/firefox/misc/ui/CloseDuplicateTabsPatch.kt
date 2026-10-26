/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * patches/src/main/kotlin/app/ftl/patches/FireFox/CloseDuplicateTabsPatch.kt
 */
package app.morphe.patches.firefox.misc.ui

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val DUP_TAB_MENU = "Lapp/hxreborn/extension/firefox/TabMenu;"

private object TabMenuItemsFingerprint : Fingerprint(
    definingClass = TOOLBAR_KT,
    name = "FloatingToolbarActions",
    filters = listOf(
        string("tabstray.banner.threeDotButton.closeAllTabs"),
        fieldAccess(definingClass = "Lkotlin/collections/EmptyList;", name = "INSTANCE", opcode = Opcode.SGET_OBJECT),
    ),
)

private fun menuJoin(method: MutableMethod, emptyIndex: Int): Int {
    var index = emptyIndex + 1
    var hops = 0
    while (method.getInstruction(index).opcode.name.startsWith("goto") && hops < 4) {
        index = method.jumpTarget(index)
        hops++
    }
    if (method.getInstruction(index).location.labels.isEmpty()) {
        throw PatchException("Menu list join point not found after EmptyList at $emptyIndex")
    }
    return index
}

private fun BytecodePatchContext.hookMenuItems() {
    val fingerprint = TabMenuItemsFingerprint
    val method = fingerprint.method
    val (_, emptyList) = fingerprint.instructionMatches
    val listRegister = (emptyList.instruction as OneRegisterInstruction).registerA
    val joinIndex = menuJoin(method, emptyList.index)

    if (!Liveness(method).isLive(joinIndex, listRegister)) {
        throw PatchException("Menu list register v$listRegister is not live at the join point")
    }

    val stateRegister = parameterRegister(method, 0)
    method.applyEdits(
        insert(
            joinIndex,
            """
                invoke-static/range {v$listRegister .. v$listRegister}, $DUP_TAB_MENU->withDuplicates(Ljava/lang/Object;)Ljava/util/List;
                move-result-object v$listRegister
            """,
        ),
        insert(
            0,
            "invoke-static/range {v$stateRegister .. v$stateRegister}, " +
                "$DUP_TAB_MENU->remember(Ljava/lang/Object;)V",
        ),
    )
}

@Suppress("unused")
val closeDuplicateTabsPatch = bytecodePatch(
    name = "Close duplicate tabs",
    description = "Adds Close duplicate tabs to the tab tray menu.",
    default = true,
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("closeDuplicates")

        hookMenuItems()
    }
}
