/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findElementByAttributeValueOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionReversed
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import java.lang.Long.toHexString

private const val BACKGROUND_NORM = 41
private const val BACKGROUND_STRONG = 44
private const val ITEM_DETAIL_BACKGROUND = 65
private val DARK_BACKGROUND_ROLES = listOf(BACKGROUND_NORM, BACKGROUND_STRONG, ITEM_DETAIL_BACKGROUND)
private const val DARK_BACKGROUND_PALETTE_FIELD = "EerieBlack"
private const val BLACK = 0xFF000000L
private const val COLOR_PACK_SHIFT = 32
private val PACKED_BLACK = toHexString(BLACK shl COLOR_PACK_SHIFT)
private const val THIS_REGISTER_COUNT = 1
private const val LONG_REGISTER_COUNT = 2
private const val NIGHT_STYLES = "res/values-night/styles.xml"
private const val BLACK_RESOURCE = "@android:color/black"

private val NIGHT_BACKGROUND_ITEMS = mapOf(
    "ProtonTheme.Pass" to "proton_background_norm",
    "ProtonTheme.Splash.Pass" to "windowSplashScreenBackground",
)

private val amoledNightStylesPatch = resourcePatch {
    execute {
        document(NIGHT_STYLES).use { document ->
            val styles = document.getElementsByTagName("style")

            NIGHT_BACKGROUND_ITEMS.forEach { (style, item) ->
                styles.findElementByAttributeValueOrThrow("name", style)
                    .getElementsByTagName("item")
                    .findElementByAttributeValueOrThrow("name", item)
                    .textContent = BLACK_RESOURCE
            }
        }
    }
}

private fun RegisterRangeInstruction.argumentRegister(index: Int) =
    startRegister + THIS_REGISTER_COUNT + index * LONG_REGISTER_COUNT

private tailrec fun MutableMethod.requireDarkBackgroundRead(register: Int, before: Int) {
    val index = indexOfFirstInstructionReversed(before - 1) {
        this is OneRegisterInstruction && registerA == register
    }
    if (index < 0) throw PatchException("No write to register v$register before instruction $before")

    val instruction = getInstruction(index)
    when (instruction.opcode) {
        Opcode.MOVE_WIDE, Opcode.MOVE_WIDE_FROM16, Opcode.MOVE_WIDE_16 ->
            requireDarkBackgroundRead((instruction as TwoRegisterInstruction).registerB, index)
        Opcode.SGET_WIDE -> {
            val field = instruction.getReference<FieldReference>()!!.name
            if (field != DARK_BACKGROUND_PALETTE_FIELD) {
                throw PatchException("Register v$register reads PassPalette.$field, expected $DARK_BACKGROUND_PALETTE_FIELD")
            }
        }
        else -> throw PatchException(
            "Expected a palette read into register v$register at instruction $index, found ${instruction.opcode}",
        )
    }
}

@Suppress("unused")
val amoledThemePatch = bytecodePatch(
    name = "AMOLED dark theme",
    description = "Replaces the dark theme background with pure black.",
) {
    compatibleWith(AppCompatibilities.PROTON_PASS)
    dependsOn(amoledNightStylesPatch)

    execute {
        val match = DarkPassColorsFingerprint.matchSingle()
        val constructorCall = match.instructionMatches.first()
        val arguments = constructorCall.getInstruction<RegisterRangeInstruction>()
        val registers = DARK_BACKGROUND_ROLES.map(arguments::argumentRegister)

        with(match.method) {
            registers.forEach { requireDarkBackgroundRead(it, constructorCall.index) }
            addInstructions(
                constructorCall.index,
                registers.joinToString("\n") { "const-wide v$it, 0x${PACKED_BLACK}L" },
            )
        }
    }
}
