/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

internal const val BEREAL_EXTENSION_PACKAGE = "Lapp/hxreborn/extension/bereal/"
private const val PATCH_CONTEXT_CLASS = "${BEREAL_EXTENSION_PACKAGE}PatchContext;"
private const val SETTINGS_ACTIVITY_CLASS = "app.hxreborn.extension.bereal.PatchesSettingsActivity"
private const val GALLERY_PICKER_ACTIVITY_CLASS = "app.hxreborn.extension.bereal.GalleryPickerActivity"
private const val RETAKE_COUNT_ACTIVITY_CLASS = "app.hxreborn.extension.bereal.RetakeCountActivity"
private const val PAIRIP_APPLICATION_CLASS = "Lcom/pairip/application/Application;"
private const val SETTINGS_ROW_TITLE = "hxreborn patches"
private const val SETTINGS_ROW_TAG = "settingsscreen_patches_button"
private const val SETTINGS_ROW_ICON = "ic_gearshape_fill"

private fun BytecodePatchContext.addSettingsRow() {
    val debugScreenAction = DebugScreenActionToStringFingerprint.matchSingle().classDef
    val debugScreenField = debugScreenAction.staticFields.single { it.type == debugScreenAction.type }
    val debugScreenReference = "${debugScreenAction.type}->${debugScreenField.name}:${debugScreenAction.type}"

    val click = settingsActionClickFingerprint(debugScreenAction.type).matchSingle()
    val rows = AboutSectionRowsFingerprint.matchSingle()
    val rowCall = rows.instructionMatch(Opcode.INVOKE_STATIC_RANGE).getInstruction<RegisterRangeInstruction>()
    val rowFactory = rowCall.getReference<MethodReference>()!!

    val emptySlotWrite = rows.instructionMatch(Opcode.CONST_4).getInstruction<OneRegisterInstruction>()
    val arrayMatch = rows.instructionMatch(Opcode.FILLED_NEW_ARRAY)
    val arrayIndex = arrayMatch.index
    val array = arrayMatch.getInstruction<FiveRegisterInstruction>()
    val emptySlot = emptySlotWrite.registerA
    if (array.registerCount != 5 || array.registerG != emptySlot) {
        throw PatchException("The last About section row is not empty")
    }

    val arrayResult = rows.instructionMatches.last()
    if (arrayResult.getInstruction<OneRegisterInstruction>().registerA != emptySlot) {
        rows.method.addInstruction(arrayResult.index + 1, "const/16 v$emptySlot, 0x0")
    }

    val first = rowCall.startRegister
    rows.method.addInstructions(
        arrayIndex,
        """
            const v$first, ${resourceId(ResourceType.DRAWABLE, SETTINGS_ROW_ICON)}
            const-string v${first + 1}, "$SETTINGS_ROW_TITLE"
            sget-object v${first + 2}, $debugScreenReference
            const/16 v${first + 3}, 0x0
            const/16 v${first + 4}, 0x0
            const-string v${first + 5}, "$SETTINGS_ROW_TAG"
            invoke-static/range { v$first .. v${first + 5} }, $rowFactory
            move-result-object v$emptySlot
        """,
    )

    click.method.addInstruction(
        click.instructionMatches.last().index + 1,
        "invoke-static { }, $PATCH_CONTEXT_CLASS->openSettings()V",
    )
}

internal val settingsPatch = bytecodePatch {
    dependsOn(
        removePairipProtectionPatch,
        resourcePatch {
            finalize {
                document("AndroidManifest.xml").use { document ->
                    val application = document.getElementsByTagName("application").item(0) as Element
                    val activity = document.createElement("activity")
                    activity.setAttribute("android:name", SETTINGS_ACTIVITY_CLASS)
                    activity.setAttribute("android:exported", "false")
                    activity.setAttribute("android:theme", "@android:style/Theme.DeviceDefault.NoActionBar")
                    application.appendChild(activity)

                    val picker = document.createElement("activity")
                    picker.setAttribute("android:name", GALLERY_PICKER_ACTIVITY_CLASS)
                    picker.setAttribute("android:exported", "false")
                    picker.setAttribute("android:excludeFromRecents", "true")
                    picker.setAttribute("android:theme", "@android:style/Theme.Translucent.NoTitleBar")
                    application.appendChild(picker)

                    val retakes = document.createElement("activity")
                    retakes.setAttribute("android:name", RETAKE_COUNT_ACTIVITY_CLASS)
                    retakes.setAttribute("android:exported", "false")
                    retakes.setAttribute("android:excludeFromRecents", "true")
                    retakes.setAttribute("android:theme", "@android:style/Theme.Translucent.NoTitleBar")
                    application.appendChild(retakes)
                }
            }
        },
    )
    extendWith("extensions/extension.mpe")

    execute {
        mutableClassDefBy(classDefBy(PAIRIP_APPLICATION_CLASS).superclass!!).methods
            .single { it.name == "onCreate" && it.parameterTypes.isEmpty() && it.returnType == "V" }
            .addInstruction(0, "invoke-static/range { p0 .. p0 }, $PATCH_CONTEXT_CLASS->attach(Landroid/app/Application;)V")

        addSettingsRow()
    }
}
