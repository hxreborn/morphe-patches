/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.toJavaClassName
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val THEME = "MyApplicationTheme"
private const val HOST_PALETTE_CLASS = "Lapp/hxreborn/extension/netanalyzer/HostPalette;"
private const val CAPTURE_METHOD = "hxCapturePalette"
private const val REGISTRATION_SEPARATOR = ";"
private const val LIST_SEPARATOR = ","
private const val NAME_SEPARATOR = "="
private const val THEME_PALETTES = 2
private const val MIN_COLOR_SCHEME_ROLES = 30
private const val CAPTURE_REGISTERS = 2

private val colorSchemeConstructor = InstructionFilter { _, instruction ->
    val constructor = instruction.getReference<MethodReference>()
    instruction.opcode == Opcode.INVOKE_DIRECT_RANGE && constructor?.name == "<init>" &&
        constructor.parameterTypes.size >= MIN_COLOR_SCHEME_ROLES && constructor.parameterTypes.all { it == "J" }
}

private fun Map<String, String>.toRegistration() =
    entries.joinToString(LIST_SEPARATOR) { (property, field) -> "$property$NAME_SEPARATOR$field" }

internal val hostPalettePatch = bytecodePatch {
    dependsOn(stringKeyPatch)
    extendWith("extensions/extension.mpe")

    execute {
        val theme = Fingerprint(filters = listOf(composable(THEME), colorSchemeConstructor)).matchSingle()
        val constructorCallIndex = theme.instructionMatches.last().index
        val constructorCall = theme.method.getInstruction<RegisterRangeInstruction>(constructorCallIndex)
        val schemeType = constructorCall.getReference<MethodReference>()!!.definingClass

        val paletteType = CustomColorsPaletteToStringFingerprint.matchSingle().originalMethod.definingClass
        val paletteInstances = theme.method.instructions.filter { it.opcode == Opcode.SGET_OBJECT }
            .map { it.getReference<FieldReference>()!! }.filter { it.type == paletteType }.distinct()
        if (paletteInstances.size != THEME_PALETTES) {
            throw PatchException("$THEME reads ${paletteInstances.size} $paletteType instances, expected $THEME_PALETTES")
        }
        val paletteRegistration = listOf(
            paletteInstances.first().definingClass.toJavaClassName(),
            paletteInstances.joinToString(LIST_SEPARATOR) { it.name },
            classDefBy(paletteType).propertyFields().toRegistration(),
        ).joinToString(REGISTRATION_SEPARATOR)
        val schemeFields = classDefBy(schemeType).propertyFields().toRegistration()

        val captureMethod = theme.classDef.addMethod(
            CAPTURE_METHOD,
            listOf(schemeType),
            "V",
            AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
            CAPTURE_REGISTERS,
            """
                const-string v0, "$paletteRegistration"
                invoke-static { v0 }, $HOST_PALETTE_CLASS->registerCustomColors(Ljava/lang/String;)V
                const-string v0, "$schemeFields"
                invoke-static { p0, v0 }, $HOST_PALETTE_CLASS->capture(Ljava/lang/Object;Ljava/lang/String;)V
                return-void
            """,
        )

        theme.method.addInstructions(
            constructorCallIndex + 1,
            "invoke-static/range { v${constructorCall.startRegister} .. v${constructorCall.startRegister} }, ${descriptor(captureMethod)}",
        )
    }
}
