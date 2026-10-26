/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.firefox.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.childElementsSequence
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val OVERLAYABLE_NAME = "HxAccentColor"
private const val ACCENT_COLORS_ARRAY = "hx_accent_colors"
private const val SYSTEM_ACCENT = "system"

private const val VIOLET = "Violet(Desaturated)?[0-9]+(A[0-9]+)?"
private val ACCENT_RESOURCE = Regex("(nova|photon)$VIOLET")
private val ACCENT_FIELD = Regex(VIOLET)
private val HEX_COLOR = Regex("#[0-9A-Fa-f]{6}")

private val accentOverlayResourcePatch = resourcePatch {
    execute {
        val names = document("res/values/colors.xml").use { document ->
            document.documentElement.childElementsSequence()
                .filter { it.tagName == "color" && ACCENT_RESOURCE.matches(it.getAttribute("name")) }
                .map { it.getAttribute("name") }
                .sorted()
                .toList()
        }
        if (names.isEmpty()) throw PatchException("No violet palette colors found")

        val qualified = decode("res/values-*/colors.xml").flatMap { path ->
            document(path).use { document ->
                document.documentElement.childElementsSequence()
                    .filter { it.tagName == "color" && it.getAttribute("name") in names }
                    .map { it.getAttribute("name") }
                    .toList()
            }
        }
        if (qualified.isNotEmpty()) throw PatchException("Violet colors with qualified values: $qualified")

        addOverlayable(OVERLAYABLE_NAME, names)
        addArray("array", ACCENT_COLORS_ARRAY, names.map { "@color/$it" })
    }
}

private fun BytecodePatchContext.transformAccentFields(fingerprint: Fingerprint) {
    val method = fingerprint.matchSingle().method
    val stores = method.instructions.withIndex().filter { (_, instruction) ->
        instruction.opcode == Opcode.SPUT_WIDE &&
            ACCENT_FIELD.matches(instruction.getReference<FieldReference>()!!.name)
    }
    stores.reversed().forEach { (index, instruction) ->
        val register = (instruction as OneRegisterInstruction).registerA
        method.addInstructions(
            index,
            """
                invoke-static/range {v$register .. v${register + 1}}, $ACCENT_COLOR->transformColor(J)J
                move-result-wide v$register
            """,
        )
    }
}

@Suppress("unused")
val accentColorPatch = bytecodePatch(
    name = "Accent color",
    description = "Replaces the violet accent with the wallpaper color or a chosen color. " +
        "The wallpaper color requires Android 12. Settings screens require Android 14.",
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    dependsOn(accentOverlayResourcePatch)

    extendWith("extensions/extension.mpe")

    val accentColor by stringOption(
        key = "accentColor",
        default = SYSTEM_ACCENT,
        values = mapOf(
            "Wallpaper color" to SYSTEM_ACCENT,
            "Blue" to "#1A73E8",
            "Teal" to "#00897B",
            "Green" to "#1E8E3E",
            "Orange" to "#E8710A",
            "Red" to "#D93025",
            "Pink" to "#D81B60",
        ),
        title = "Accent color",
        description = "Wallpaper color or a hex color such as #1A73E8.",
        required = true,
        validator = { it == SYSTEM_ACCENT || (it != null && HEX_COLOR.matches(it.trim())) },
    )

    execute {
        listInSheet("accentColor")

        ChosenAccentColorFingerprint.matchSingle().method.returnEarly(accentColor!!.trim())
        violetColorsFingerprints.forEach(::transformAccentFields)
        ApplicationOnCreateFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static/range {p0 .. p0}, $ACCENT_COLOR->installResourceOverlay(Landroid/app/Application;)V",
        )
    }
}
