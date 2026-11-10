/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patches.ultimea.misc.settings.patchesSettingsPatch
import app.morphe.patches.ultimea.misc.settings.FlutterInitFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle

private const val THEME_ARTWORK_CLASS = "Lapp/hxreborn/extension/ultimea/ThemeArtwork;"
internal const val THEME_SETTINGS_CLASS = "Lapp/hxreborn/extension/ultimea/ThemeSettings;"

internal val themeSettingsPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")
    dependsOn(patchesSettingsPatch)

    execute {
        FlutterInitFingerprint.matchSingle().method.addInstructions(0,
            "invoke-static { p1 }, $THEME_SETTINGS_CLASS->prepare(Landroid/content/Context;)V")
        val brightness = PlatformBrightnessFingerprint.matchSingle()
        val (_, putBrightness) = brightness.instructionMatches
        val brightnessRegister = brightness.method.getInstruction<com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction>(putBrightness.index).registerE
        brightness.method.addInstructions(putBrightness.index, "const-string v$brightnessRegister, \"dark\"")
        val imageDecoder = ImageDecoderFingerprint.matchSingle().method
        val headerCallback = ImageHeaderCallbackFingerprint.matchSingle().method
        mutableClassDefBy(THEME_ARTWORK_CLASS).replaceStub("onHeader", 4,
            "invoke-static { p0, p1, p2, p3 }, $headerCallback\nreturn-void")
        val bitmapRegister = imageDecoder.getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
        imageDecoder.addInstructionsWithLabels(0,
            "invoke-static { p0, p1, p2 }, $THEME_ARTWORK_CLASS->decodeHero(Ljava/nio/ByteBuffer;J)Landroid/graphics/Bitmap;\n" +
                "move-result-object v$bitmapRegister\nif-eqz v$bitmapRegister, :stock_decoder\nreturn-object v$bitmapRegister",
            ExternalLabel("stock_decoder", imageDecoder.getInstruction(0)))
        val systemBarStyle = SystemBarStyleFingerprint.matchSingle().method
        val jsonParameter = systemBarStyle.parameterTypes.indexOf("Lorg/json/JSONObject;")
        check(jsonParameter >= 0) { "The Flutter system bar parser has no JSON argument" }
        val parameter = jsonParameter + if (systemBarStyle.accessFlags and AccessFlags.STATIC.value != 0) 0 else 1
        systemBarStyle.addInstructions(0, "invoke-static { p$parameter }, $THEME_SETTINGS_CLASS->rewriteSystemBars(Lorg/json/JSONObject;)V")
        ActivityCreateFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p0 }, $THEME_SETTINGS_CLASS->initialize(Landroid/app/Activity;)V",
        )
        mutableClassDefBy("Lapp/hxreborn/extension/ultimea/PatchesSettingsActivity;")
            .methods.single { it.name == "onCreate" }.addInstructions(0,
                "invoke-static { p0 }, $THEME_SETTINGS_CLASS->initialize(Landroid/app/Activity;)V")
        val style = mutableClassDefBy("Lapp/hxreborn/extension/ultimea/SettingsStyle;")
            .methods.single { it.name == "<init>" }
        style.implementation!!.instructions.withIndex().filter { (_, instruction) ->
            instruction is ReferenceInstruction && instruction.reference is FieldReference &&
                instruction.getReference<FieldReference>()!!.definingClass == style.definingClass &&
                instruction.getReference<FieldReference>()!!.name in setOf("dark", "screenBackground") &&
                instruction.opcode in setOf(Opcode.IPUT_BOOLEAN, Opcode.IPUT)
        }.map { it.index }.reversed().forEach { index ->
            val instruction = style.getInstruction<TwoRegisterInstruction>(index)
            val field = instruction.getReference<FieldReference>()!!
            val register = instruction.registerA
            val receiver = instruction.registerB
            if (field.name == "dark") {
                style.replaceInstruction(index, "const/16 v$register, 0x1")
                style.addInstructions(index + 1, "iput-boolean v$register, v$receiver, $field")
            } else {
                style.replaceInstruction(index, "invoke-static {}, $THEME_SETTINGS_CLASS->pageBackground()I")
                style.addInstructions(index + 1, "move-result v$register\niput v$register, v$receiver, $field")
            }
        }
        val content = mutableClassDefBy("Lapp/hxreborn/extension/ultimea/PatchesSettingsActivity;")
            .methods.single { it.name == "content" }
        val instructions = content.implementation!!.instructions.toList()
        val cardCall = instructions.indexOfFirst { instruction ->
            instruction is ReferenceInstruction && instruction.reference is MethodReference &&
                instruction.getReference<MethodReference>()!!.definingClass == "Lapp/hxreborn/extension/ultimea/SettingsRows;" &&
                instruction.getReference<MethodReference>()!!.name == "card"
        }
        check(cardCall >= 0) { "Could not find the patches menu App card" }
        val cardResult = cardCall + 1
        val cardRegister = content.getInstruction<OneRegisterInstruction>(cardResult).registerA
        val listConstructor = instructions.indexOfFirst { instruction ->
            instruction is ReferenceInstruction && instruction.reference is MethodReference &&
                instruction.getReference<MethodReference>()!!.definingClass == "Ljava/util/ArrayList;" &&
                instruction.getReference<MethodReference>()!!.name == "<init>"
        }
        check(listConstructor >= 0) { "Could not find the applied patches list" }
        val listRegister = (instructions[listConstructor] as com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction).registerC
        val activityRegister = content.getFreeRegisterProvider(cardResult + 1, 1, cardRegister).getFreeRegister4Bit()
        listOf(
            listConstructor + 1 to "invoke-static { v$listRegister }, $THEME_SETTINGS_CLASS->addAppliedNames(Ljava/util/List;)V",
            cardResult + 1 to "move-object/from16 v$activityRegister, p0\ninvoke-static { v$activityRegister, v$cardRegister }, $THEME_SETTINGS_CLASS->addMenuRow(Landroid/app/Activity;Landroid/widget/LinearLayout;)V",
        ).sortedByDescending { it.first }.forEach { (index, instructions) -> content.addInstructions(index, instructions) }
    }
}
