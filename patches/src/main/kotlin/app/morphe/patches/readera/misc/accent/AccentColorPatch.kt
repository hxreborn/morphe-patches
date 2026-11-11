/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.accent

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.util.getReference
import app.morphe.patches.readera.shared.AppCompatAttachBaseContextFingerprint
import app.morphe.patches.readera.shared.CONTEXT_CLASS
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.getOrAddApplicationAttachBaseContext
import app.morphe.patches.readera.shared.patchesSettingsPatch
import app.morphe.patches.readera.shared.readEraAccent
import app.morphe.patches.readera.shared.registerReadEraPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import app.morphe.util.ResourceGroup
import app.morphe.util.asSequence
import app.morphe.util.copyResources
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/readera/AccentColor;"
private const val MENU_SWITCH_TRACK = "res/drawable/hx_readera_switch_track.xml"
private const val COLOR_REFERENCE_PREFIX = "@color/"
private const val MENU_TRACK_CHECKED_FILL = "android:color=\"#ffc107\""

private lateinit var accentColorName: String

private val accentColorResourcesPatch = resourcePatch {
    dependsOn(patchesSettingsPatch)

    execute {
        val accent = readEraAccent()
        if (!accent.startsWith(COLOR_REFERENCE_PREFIX)) throw PatchException("Expected the accent to be a color, found $accent")
        accentColorName = accent.removePrefix(COLOR_REFERENCE_PREFIX)

        val qualifiedDefinitions = decode("res/values-*/colors.xml").filter { path ->
            document(path).use { document ->
                document.getElementsByTagName("color").asSequence().any { (it as Element).getAttribute("name") == accentColorName }
            }
        }
        if (qualifiedDefinitions.isNotEmpty()) {
            throw PatchException("Accent color $accentColorName is also defined in $qualifiedDefinitions")
        }

        val trackFile = get(MENU_SWITCH_TRACK)
        val track = trackFile.readText()
        val checkedFillCount = track.split(MENU_TRACK_CHECKED_FILL).size - 1
        if (checkedFillCount != 1) {
            throw PatchException("Expected one $MENU_TRACK_CHECKED_FILL in $MENU_SWITCH_TRACK, found $checkedFillCount")
        }
        trackFile.writeText(track.replace(MENU_TRACK_CHECKED_FILL, "android:color=\"$accent\""))

        copyResources("readera", ResourceGroup("drawable", "hx_readera_palette.xml"))
    }
}

@Suppress("unused")
val accentColorPatch = bytecodePatch(
    name = "Accent color",
    description = "Replaces ReadEra's yellow with the wallpaper color on Android 12 and later, or green below 12. " +
        "Another color can be picked in the patches menu.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(patchesSettingsPatch, disableSignatureChecksPatch, accentColorResourcesPatch)

    execute {
        registerReadEraPatch("Accent color", "Replaces ReadEra's yellow on switches, sliders, buttons and headings.")
        mutableClassDefBy(EXTENSION_CLASS_DESCRIPTOR).replaceResourceIdStubs(
            ResourceType.COLOR,
            mapOf("accentColorId" to accentColorName),
        )

        getOrAddApplicationAttachBaseContext().apply {
            val superCall = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_SUPER && getReference<MethodReference>()?.name == "attachBaseContext"
            }
            val context = getInstruction<FiveRegisterInstruction>(superCall).registerD
            addInstruction(superCall, "invoke-static { v$context }, $EXTENSION_CLASS_DESCRIPTOR->applyToApplication($CONTEXT_CLASS)V")
        }

        AppCompatAttachBaseContextFingerprint.matchAll(1..1).single().let { match ->
            val (_, superCall) = match.instructionMatches
            match.method.addInstruction(
                superCall.index + 1,
                "invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->applyToActivity($CONTEXT_CLASS)V",
            )
        }

        AppCompatConfigurationChangedFingerprint.matchAll(1..1).single().method.addInstruction(
            0,
            "invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->applyToActivity($CONTEXT_CLASS)V",
        )

        JumpBarColorFingerprint.matchAll(1..1).single().let { match ->
            val colorLiteralCount = match.method.implementation!!.instructions.count {
                (it as? WideLiteralInstruction)?.wideLiteral == JUMP_BAR_COLOR.toLong()
            }
            if (colorLiteralCount != 1) {
                throw PatchException("Expected one jump bar color literal in ${match.method}, found $colorLiteralCount")
            }
            val (colorLiteral) = match.instructionMatches
            val color = colorLiteral.getInstruction<OneRegisterInstruction>().registerA
            match.method.addInstructions(
                colorLiteral.index + 1,
                """
                    invoke-static { v$color }, $EXTENSION_CLASS_DESCRIPTOR->jumpBarColor(I)I
                    move-result v$color
                """,
            )
        }
    }
}
