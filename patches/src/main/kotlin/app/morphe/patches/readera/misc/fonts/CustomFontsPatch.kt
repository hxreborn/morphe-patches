/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.fonts

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private const val LOAD_CUSTOM_FONTS_METHOD = "loadCustomFonts"
private const val FONT_FOLDER_PICKER_REQUEST = 63555

@Suppress("unused")
val customFontsPatch = bytecodePatch(
    name = "Custom fonts",
    description = "Adds fonts from a folder you choose to the reading fonts.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(disableSignatureChecksPatch)

    execute {
        val (settings, fontFolder) = SaveCustomFontsCacheFingerprint.instructionMatches.let {
            it[0].instruction.getReference<MethodReference>()!! to it[2].instruction.getReference<FieldReference>()!!
        }
        val saveCustomCache = SaveCustomFontsCacheFingerprint.method
        val systemFonts = SystemFontsFingerprint.instructionMatches.map { it.instruction }
        val readCache = systemFonts[1].getReference<MethodReference>()!!
        val createAnalyzer = systemFonts[2].getReference<MethodReference>()!!
        val readLigatureMaps = systemFonts[4].getReference<MethodReference>()!!
        val applyLigatureMaps = systemFonts[5].getReference<MethodReference>()!!
        val close = systemFonts[7].getReference<MethodReference>()!!
        val fontType = systemFonts[8].getReference<TypeReference>()!!.type
        val prepareFont = systemFonts[9].getReference<MethodReference>()!!
        val (isCacheCurrent, isCachePathCurrent) = SystemFontsCacheValidFingerprint.instructionMatches.map {
            it.instruction.getReference<MethodReference>()!!
        }
        val scanFolder = SystemFontsScanFingerprint.instructionMatches.last().instruction.getReference<MethodReference>()!!

        val fontsModel = SaveCustomFontsCacheFingerprint.classDef
        val loadCustomFonts = ImmutableMethod(
            fontsModel.type,
            LOAD_CUSTOM_FONTS_METHOD,
            listOf(),
            "Ljava/util/List;",
            AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(4),
        ).toMutable().apply {
            addInstructionsWithLabels(
                0,
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
        }
        fontsModel.methods.add(loadCustomFonts)

        listOf(FontsByCategoryFingerprint to 2, FontNamesFingerprint to 2).forEach { (fingerprint, emptyListMatch) ->
            fingerprint.let {
                val emptyListIndex = it.instructionMatches[emptyListMatch].index
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
            val customListIndex = it.instructionMatches.last().index
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
            val firstLoadIndex = it.instructionMatches.first().index
            val loadCategory = it.method.getInstruction<FiveRegisterInstruction>(firstLoadIndex)
            val loadCategoryMethod = it.instructionMatches.first().instruction.getReference<MethodReference>()!!
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
            val setTextIndex = it.instructionMatches.last().index
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
