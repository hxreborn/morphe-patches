/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.fonts

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.patchesSettingsPatch
import app.morphe.patches.readera.shared.registerReadEraPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.addMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val LOAD_CUSTOM_FONTS_METHOD = "loadCustomFonts"
private const val FONT_FOLDER_PICKER_REQUEST = 63555

@Suppress("unused")
val customFontsPatch = bytecodePatch(
    name = "Custom fonts",
    description = "Adds reading fonts from a folder on the device. " +
        "The folder is picked on ReadEra's Fonts screen.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(patchesSettingsPatch, disableSignatureChecksPatch)

    execute {
        registerReadEraPatch("Custom fonts", "Adds reading fonts from a folder picked on the Fonts screen.")
        val (settingsCall, _, fontFolderRead) = SaveCustomFontsCacheFingerprint.instructionMatches
        val settings = settingsCall.instruction.getReference<MethodReference>()!!
        val fontFolder = fontFolderRead.instruction.getReference<FieldReference>()!!
        val saveCustomCache = SaveCustomFontsCacheFingerprint.method
        val systemFonts = SystemFontsFingerprint.instructionMatches
        val readCache = systemFonts[SYSTEM_FONTS_READ_CACHE_MATCH].instruction.getReference<MethodReference>()!!
        val createAnalyzer = systemFonts[SYSTEM_FONTS_CREATE_ANALYZER_MATCH].instruction.getReference<MethodReference>()!!
        val readLigatureMaps = systemFonts[SYSTEM_FONTS_READ_LIGATURE_MAPS_MATCH].instruction.getReference<MethodReference>()!!
        val applyLigatureMaps = systemFonts[SYSTEM_FONTS_APPLY_LIGATURE_MAPS_MATCH].instruction.getReference<MethodReference>()!!
        val close = systemFonts[SYSTEM_FONTS_CLOSE_MATCH].instruction.getReference<MethodReference>()!!
        val fontType = systemFonts[SYSTEM_FONTS_FONT_CAST_MATCH].instruction.getReference<TypeReference>()!!.type
        val prepareFont = systemFonts[SYSTEM_FONTS_PREPARE_FONT_MATCH].instruction.getReference<MethodReference>()!!
        val (isCacheCurrent, isCachePathCurrent) = SystemFontsCacheValidFingerprint.instructionMatches.map {
            it.instruction.getReference<MethodReference>()!!
        }
        val (_, scanFolderCall) = SystemFontsScanFingerprint.instructionMatches
        val scanFolder = scanFolderCall.instruction.getReference<MethodReference>()!!

        val fontsModel = SaveCustomFontsCacheFingerprint.classDef
        val loadCustomFonts = fontsModel.addMethod(
            LOAD_CUSTOM_FONTS_METHOD,
            emptyList(),
            "Ljava/util/List;",
            AccessFlags.STATIC.value,
            4,
            """
                const-string v0, "custom"
                invoke-static { v0 }, $isCacheCurrent
                move-result v1
                if-eqz v1, :scan
                invoke-static { }, $settings
                move-result-object v1
                iget-object v1, v1, $fontFolder
                invoke-static { v0, v1 }, $isCachePathCurrent
                move-result v1
                if-eqz v1, :scan
                invoke-static { v0 }, $readCache
                move-result-object v1
                if-eqz v1, :scan
                invoke-interface { v1 }, Ljava/util/List;->isEmpty()Z
                move-result v2
                if-eqz v2, :prepare
                :scan
                invoke-static { }, $createAnalyzer
                move-result-object v0
                invoke-static { }, $settings
                move-result-object v1
                iget-object v1, v1, $fontFolder
                invoke-static { v0, v1 }, $scanFolder
                move-result-object v1
                invoke-static { }, $readLigatureMaps
                move-result-object v2
                invoke-static { v1, v2 }, $applyLigatureMaps
                invoke-static { v1 }, $saveCustomCache
                invoke-static { v0 }, $close
                :prepare
                invoke-interface { v1 }, Ljava/util/List;->iterator()Ljava/util/Iterator;
                move-result-object v2
                :next
                invoke-interface { v2 }, Ljava/util/Iterator;->hasNext()Z
                move-result v3
                if-eqz v3, :done
                invoke-interface { v2 }, Ljava/util/Iterator;->next()Ljava/lang/Object;
                move-result-object v3
                check-cast v3, $fontType
                invoke-virtual { v3 }, $prepareFont
                goto :next
                :done
                return-object v1
            """,
        )

        listOf(FontsByCategoryFingerprint, FontNamesFingerprint).forEach { fingerprint ->
            fingerprint.let {
                val (_, _, emptyListCreation, _) = it.instructionMatches
                val emptyListIndex = emptyListCreation.index
                val listRegister = it.method.getInstruction<OneRegisterInstruction>(emptyListIndex).registerA
                it.method.removeInstructions(emptyListIndex, 2)
                it.method.addInstructions(
                    emptyListIndex,
                    """
                        invoke-static { }, $loadCustomFonts
                        move-result-object v$listRegister
                    """,
                )
            }
        }

        FontsUpdateFingerprint.let {
            val (_, _, _, customListCall) = it.instructionMatches
            val customListIndex = customListCall.index
            val customListRegister = it.method.getInstruction<FiveRegisterInstruction>(customListIndex).registerD
            it.method.addInstructions(
                customListIndex,
                """
                    invoke-static { }, $loadCustomFonts
                    move-result-object v$customListRegister
                """,
            )
        }

        LoadFontsFingerprint.let {
            val (firstLoadCall, _, _, _) = it.instructionMatches
            val firstLoadIndex = firstLoadCall.index
            val loadCategory = it.method.getInstruction<FiveRegisterInstruction>(firstLoadIndex)
            val loadCategoryMethod = firstLoadCall.instruction.getReference<MethodReference>()!!
            val categoryIndex = firstLoadIndex - 1
            check(it.method.getInstruction<OneRegisterInstruction>(categoryIndex).registerA == loadCategory.registerC) {
                "Font category is not set right before the first load call"
            }
            it.method.addInstructions(
                categoryIndex,
                """
                    const/4 v${loadCategory.registerC}, 0x3
                    invoke-static { v${loadCategory.registerC}, v${loadCategory.registerD} }, $loadCategoryMethod
                """,
            )
        }

        FontFolderRowFingerprint.let {
            val (_, _, setTextCall) = it.instructionMatches
            val setTextIndex = setTextCall.index
            val setText = it.method.getInstruction<FiveRegisterInstruction>(setTextIndex)
            it.method.replaceInstruction(
                setTextIndex,
                "invoke-virtual { v${setText.registerC}, v${setText.registerD} }, " +
                    "Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V",
            )
            it.method.replaceInstruction(setTextIndex - 1, "invoke-static { }, $settings")
            it.method.addInstructions(
                setTextIndex,
                """
                    move-result-object v${setText.registerD}
                    iget-object v${setText.registerD}, v${setText.registerD}, $fontFolder
                """,
            )
        }

        FontFolderClickFingerprint.method.apply {
            removeInstructions(0, implementation!!.instructions.count())
            addInstructions(
                0,
                """
                    new-instance p1, Landroid/content/Intent;
                    const-class v0, Lorg/readera/FilepickerActivity;
                    invoke-direct { p1, p0, v0 }, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
                    const v0, $FONT_FOLDER_PICKER_REQUEST
                    invoke-virtual { p0, p1, v0 }, Landroid/app/Activity;->startActivityForResult(Landroid/content/Intent;I)V
                    return-void
                """,
            )
        }
    }
}
