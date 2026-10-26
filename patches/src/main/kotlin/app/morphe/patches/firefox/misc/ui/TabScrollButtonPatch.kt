/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * patches/src/main/kotlin/app/ftl/patches/FireFox/TabScrollButtonPatch.kt
 */
package app.morphe.patches.firefox.misc.ui

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.returnEarly
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val SB_TAB_SCROLL = "Lapp/hxreborn/extension/firefox/TabScroll;"
private const val SB_TAB_SCROLL_CLICK = "Lapp/hxreborn/extension/firefox/TabScrollClick;"


private fun BytecodePatchContext.hookScrollState() {
    val method = TabLayoutScrollHelperFingerprint.method
    val state = parameterRegister(method, 0)
    method.addInstructions(
        0,
        "invoke-static/range {v$state .. v$state}, $SB_TAB_SCROLL->track(Ljava/lang/Object;)V",
    )
}

private fun BytecodePatchContext.insertScrollButton() {
    val tabFab = mutableClassDefBy(TAB_TOOLBAR)
    fun stub(name: String) = tabFab.methods.firstOrNull { it.name == name }
        ?: throw PatchException("TabToolbar.$name not found. Apply \"New tab next to current tab\" first")

    stub("scrollButtonsApplied").returnEarly(true)

    val trayPill = stub("trayPill")
    val visibleIndex = trayPill.indexOfFirstInstructionOrThrow {
        opcode == Opcode.INVOKE_STATIC && getReference<MethodReference>()?.name == "pillVisible"
    }
    val state = trayPill.getInstruction<FiveRegisterInstruction>(visibleIndex).registerC
    trayPill.addInstructions(visibleIndex, "invoke-static {v$state}, $SB_TAB_SCROLL->tray(Ljava/lang/Object;)V")

    val content = stub("pillContent")
    val at = content.indexOfFirstInstructionOrThrow {
        opcode == Opcode.INVOKE_STATIC && getReference<MethodReference>()?.name == "endVisible"
    }
    val smali = $$$"""
        invoke-static {v13}, $$${TAB_TOOLBAR}->scrollVisible(Ljava/lang/Object;)Z
        move-result v0
        if-eqz v0, :hx_s_skip
        const v2, 0x5a11b0c4
        invoke-interface {v12, v2}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V
        $$${iconButton(
            click = "invoke-static {}, $SB_TAB_SCROLL->up()$SB_TAB_SCROLL_CLICK",
            icon = "invoke-static {}, $SB_TAB_SCROLL->upIcon()I",
            key = 0x5a11b0c6,
            label = "Scroll to first tab",
        )}
        $$${iconButton(
            click = "invoke-static {}, $SB_TAB_SCROLL->down()$SB_TAB_SCROLL_CLICK",
            icon = "invoke-static {}, $SB_TAB_SCROLL->downIcon()I",
            key = 0x5a11b0c7,
            label = "Scroll to last tab",
        )}
        invoke-interface {v12}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V
        goto :hx_s_done
        :hx_s_skip
        const v2, 0x5a11b0c5
        invoke-interface {v12, v2}, Landroidx/compose/runtime/Composer;->startReplaceGroup(I)V
        invoke-interface {v12}, Landroidx/compose/runtime/Composer;->endReplaceGroup()V
        :hx_s_done
        nop
    """

    content.applyEdits(insert(at, smali))
}

@Suppress("unused")
val tabScrollButtonPatch = bytecodePatch(
    name = "Tab scroll button",
    description = "Adds buttons beside + in the tab tray that jump to the first and last tab.",
    default = true,
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    dependsOn(tabNextToCurrentPatch)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("tabScroll")

        hookScrollState()
        insertScrollButton()
    }
}
