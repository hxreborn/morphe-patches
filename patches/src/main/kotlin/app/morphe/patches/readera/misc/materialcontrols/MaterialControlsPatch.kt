/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.materialcontrols

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.patchesSettingsPatch
import app.morphe.patches.readera.shared.readEraResourceRoles
import app.morphe.patches.readera.shared.registerReadEraPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.addStaticMethod
import app.morphe.util.asSequence
import com.android.tools.smali.dexlib2.Opcode
import org.w3c.dom.Element

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/readera/MaterialControls;"
internal const val MATERIAL_CHECK_BOX_TAG = "app.hxreborn.extension.readera.MaterialCheckBox"
internal const val MATERIAL_RADIO_BUTTON_TAG = "app.hxreborn.extension.readera.MaterialRadioButton"
private const val SWITCH_CLASS = "Landroidx/appcompat/widget/SwitchCompat;"
private const val BUTTON_CLASS = "Landroid/widget/CompoundButton;"
private const val DRAWABLE_CLASS = "Landroid/graphics/drawable/Drawable;"
private const val COLOR_STATE_LIST = "Landroid/content/res/ColorStateList;"
private val controlResources = listOf(
    "switch_thumb", "switch_thumb_on", "switch_thumb_off", "switch_track",
    "switch_thumb_disabled_on", "switch_thumb_disabled_off",
    "check", "check_on", "check_off", "radio", "radio_on", "radio_off",
)

private val materialControlsResourcesPatch = resourcePatch {
    execute {
        val roles = readEraResourceRoles()
        val replacements = mapOf("CheckBox" to MATERIAL_CHECK_BOX_TAG, "RadioButton" to MATERIAL_RADIO_BUTTON_TAG)
        val counts = mutableMapOf<String, Int>()
        decode("res/layout*/*.xml").forEach { path ->
            document(path).use { document ->
                replacements.forEach { (tag, materialTag) ->
                    document.getElementsByTagName(tag).asSequence().toList().forEach { node ->
                        val original = node as Element
                        val replacement = document.createElement(materialTag)
                        (0 until original.attributes.length).forEach { index ->
                            val attribute = original.attributes.item(index)
                            replacement.setAttribute(attribute.nodeName, attribute.nodeValue)
                        }
                        if (replacement.hasAttribute(roles.buttonTintAttribute)) {
                            replacement.setAttribute("android:buttonTint", replacement.getAttribute(roles.buttonTintAttribute))
                            replacement.removeAttribute(roles.buttonTintAttribute)
                        }
                        original.parentNode.replaceChild(replacement, original)
                        counts[tag] = (counts[tag] ?: 0) + 1
                    }
                }
            }
        }
        check(counts.keys == replacements.keys) { "Missing framework control layouts: $counts" }
        document("res/values/colors.xml").use { document ->
            mapOf(
                "hx_readera_control_surface" to roles.surface,
                "hx_readera_control_accent" to roles.accent,
                "hx_readera_control_check_accent" to roles.checkboxAccent,
                "hx_readera_control_outline" to "#607075",
                "hx_readera_control_outline_disabled" to "#61607075",
                "hx_readera_control_thumb" to "#b5bcbe",
            ).forEach { (name, value) ->
                document.documentElement.appendChild(document.createElement("color").also {
                    it.setAttribute("name", name)
                    it.textContent = value
                })
            }
        }
        controlResources.forEach { name ->
            val file = "hx_readera_control_$name.xml"
            MaterialControlResources::class.java.getResourceAsStream("/readera/controls/$file")!!.use { input ->
                get("res/drawable/$file").outputStream().use(input::copyTo)
            }
        }
    }
}

@Suppress("unused")
val materialControlsPatch = bytecodePatch(
    name = "Material 3 controls",
    description = "Gives switches, checkboxes and radio buttons in Settings and dialogs the Material 3 look.",
) {
    compatibleWith(AppCompatibilities.READERA)
    availability(requireArm64)
    dependsOn(patchesSettingsPatch, disableSignatureChecksPatch, materialControlsResourcesPatch)

    execute {
        registerReadEraPatch("Material 3 controls", "Gives switches, checkboxes and radio buttons the Material 3 look.")
        val extension = mutableClassDefBy(EXTENSION_CLASS_DESCRIPTOR)
        val styleSwitch = extension.addStaticMethod(
            "styleSwitch", listOf(SWITCH_CLASS), "V", 2,
            """
                const/4 v0, 0x0
                invoke-virtual { p0, v0 }, $SWITCH_CLASS->setThumbTintList($COLOR_STATE_LIST)V
                invoke-virtual { p0, v0 }, $SWITCH_CLASS->setTrackTintList($COLOR_STATE_LIST)V
                invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->thumb($BUTTON_CLASS)$DRAWABLE_CLASS
                move-result-object v0
                invoke-virtual { p0, v0 }, $SWITCH_CLASS->setThumbDrawable($DRAWABLE_CLASS)V
                invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->track($BUTTON_CLASS)$DRAWABLE_CLASS
                move-result-object v0
                invoke-virtual { p0, v0 }, $SWITCH_CLASS->setTrackDrawable($DRAWABLE_CLASS)V
                invoke-virtual { v0 }, $DRAWABLE_CLASS->getIntrinsicWidth()I
                move-result v0
                invoke-virtual { p0, v0 }, $SWITCH_CLASS->setSwitchMinWidth(I)V
                const/4 v0, 0x0
                invoke-virtual { p0, v0 }, $SWITCH_CLASS->setSplitTrack(Z)V
                return-void
            """,
        )
        listOf("SwitchCompat" to "CompoundButton", "AppCompatCheckBox" to "CheckBox", "AppCompatRadioButton" to "RadioButton").forEach { (widget, parent) ->
            val match = controlConstructorFingerprint(widget, parent).matchAll(1..1).single()
            val method = match.method
            check(method.instructions.count { it.opcode == Opcode.RETURN_VOID } == 1)
            val hook = if (widget == "SwitchCompat") {
                "invoke-static/range { p0 .. p0 }, $styleSwitch"
            } else {
                val radio = if (widget == "AppCompatRadioButton") "0x1" else "0x0"
                """
                    const/4 p3, $radio
                    invoke-static { p0, p3 }, $EXTENSION_CLASS_DESCRIPTOR->styleButton(${BUTTON_CLASS}Z)V
                """
            }
            method.addInstructions(method.instructions.lastIndex, hook)
        }
    }
}

private object MaterialControlResources
