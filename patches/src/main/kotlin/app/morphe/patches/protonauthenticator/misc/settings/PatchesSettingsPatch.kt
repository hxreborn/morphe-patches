/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonauthenticator.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.protonauthenticator.shared.themeColorReadInstructions
import app.morphe.patches.shared.misc.proton.COMPOSER_TYPE
import app.morphe.patches.shared.misc.proton.PATCHES_MENU_CLASS
import app.morphe.patches.shared.misc.proton.SETTINGS_ROW_TITLE
import app.morphe.patches.shared.misc.proton.attachPatchContext
import app.morphe.patches.shared.misc.proton.injectAppCompatDefaultNightMode
import app.morphe.patches.shared.misc.proton.injectBundleVersion
import app.morphe.patches.shared.misc.proton.literalWrittenTo
import app.morphe.patches.shared.misc.proton.patchesSettingsActivityPatch
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val HOST_ACTIVITY = "proton.android.authenticator.app.ui.MainActivity"
private const val APPLICATION_CLASS = "Lproton/android/authenticator/app/App;"
private const val SETTINGS_ROW_METHOD = "patchesSettingsRow"
private const val TITLE_PARAMETER = 0
private const val ON_CLICK_PARAMETER = 1
private const val COMPOSER_PARAMETER = 5
private const val CHANGED_PARAMETER = 6
private const val DEFAULTS_PARAMETER = 7
private const val ROW_REGISTER_COUNT = 9
private const val MENU_LIST_BORDER_COLOR = "menuListBorder"

private fun BytecodePatchContext.addPatchesSettingsRow() {
    val row = ViewLogsRowFingerprint.matchSingle()
    val rowIndex = row.instructionMatches.last().index
    val rowCall = row.method.getInstruction<RegisterRangeInstruction>(rowIndex)
    val navigationRow = rowCall.getReference<MethodReference>()!!
    val titleType = navigationRow.parameterTypes[TITLE_PARAMETER].toString()
    val dynamicTitleType = titleType.removeSuffix(";") + "\$Dynamic;"
    classDefByOrNull(dynamicTitleType) ?: throw PatchException("No class $dynamicTitleType")
    val onClickType = navigationRow.parameterTypes[ON_CLICK_PARAMETER].toString()
    val changedFlags = row.method.literalWrittenTo(rowCall.startRegister + CHANGED_PARAMETER, rowIndex)
    val defaultsMask = row.method.literalWrittenTo(rowCall.startRegister + DEFAULTS_PARAMETER, rowIndex)

    val settingsRow = row.classDef.addMethod(
        SETTINGS_ROW_METHOD,
        listOf(COMPOSER_TYPE),
        "V",
        AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
        ROW_REGISTER_COUNT,
        """
            ${themeColorReadInstructions(MENU_LIST_BORDER_COLOR)}
            move-wide v2, v0
            const/4 v0, 0x0
            const/4 v1, 0x0
            move-object v4, p0
            const/4 v5, 0x0
            const/4 v6, 0x3
            invoke-static/range { v0 .. v6 }, ${descriptor(HorizontalDividerFingerprint.method)}
            new-instance v0, $dynamicTitleType
            const-string v1, "$SETTINGS_ROW_TITLE"
            invoke-static { }, Ljava/util/Collections;->emptyList()Ljava/util/List;
            move-result-object v2
            invoke-direct { v0, v1, v2 }, $dynamicTitleType-><init>(Ljava/lang/String;Ljava/util/List;)V
            const-class v1, $onClickType
            invoke-static { v1 }, $PATCHES_MENU_CLASS->settingsRowOnClick(Ljava/lang/Class;)Ljava/lang/Object;
            move-result-object v1
            check-cast v1, $onClickType
            const/4 v2, 0x0
            const/4 v3, 0x0
            const/4 v4, 0x1
            move-object v5, p0
            const v6, $changedFlags
            const v7, $defaultsMask
            invoke-static/range { v0 .. v7 }, ${descriptor(navigationRow)}
            return-void
        """,
    )

    val composer = rowCall.startRegister + COMPOSER_PARAMETER
    row.method.addInstruction(
        rowIndex + 1,
        "invoke-static/range { v$composer .. v$composer }, ${descriptor(settingsRow)}",
    )
}

internal val patchesSettingsPatch = bytecodePatch {
    dependsOn(patchesSettingsActivityPatch("@style/Theme.ProtonAuthenticator", HOST_ACTIVITY))
    extendWith("extensions/extension.mpe")

    execute {
        injectBundleVersion()
        injectAppCompatDefaultNightMode()
        attachPatchContext(APPLICATION_CLASS)
        addPatchesSettingsRow()
    }
}
