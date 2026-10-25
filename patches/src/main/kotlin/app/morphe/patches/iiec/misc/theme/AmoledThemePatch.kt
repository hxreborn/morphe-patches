/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.iiec.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.Document
import app.morphe.patcher.util.cloneParameters
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.iiec.misc.fix.signature.bypassSignatureCheckPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.util.adoptChild
import app.morphe.util.childElementsSequence
import app.morphe.util.findElementByAttributeValueOrThrow
import app.morphe.util.getNode
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/iiec/AmoledTheme;"
private const val ON_CREATE_METHOD = "onCreate"

private const val AMOLED_VALUE = "amoled"
private const val AMOLED_TITLE = "AMOLED (patch)"
private const val APPLY_AMOLED_METHOD = "applyAmoledBackground"
private const val BLACK_ARGB_SMALI = "-0x1000000"

private const val EDITOR_THEME_TITLES_ARRAY = "pref_appearance_editor_theme_titles"
private const val EDITOR_THEME_VALUES_ARRAY = "pref_appearance_editor_theme_values"
private const val DARK_EDITOR_THEME_TITLES_ARRAY = "pref_appearance_editor_theme_dark_titles"
private const val DARK_EDITOR_THEME_VALUES_ARRAY = "pref_appearance_editor_theme_dark_values"
private const val DARK_EDITOR_THEME_PREFERENCE_KEY = "appearance_editor_theme_dark"

private const val OVERLAY_STYLE = "hx_iiec_amoled_theme_overlay"
private const val BLACK = "#000000"

private val OVERLAY_ITEMS = mapOf(
    "android:colorBackground" to BLACK,
    "android:windowBackground" to BLACK,
    "colorPrimary" to BLACK,
    "colorSurface" to BLACK,
    "elevationOverlayEnabled" to "false",
)

private var applicationClassType: String? = null

private const val HARDCODED_PRIMARY_BACKGROUND = "@color/colorPrimary"
private const val THEME_PRIMARY_BACKGROUND = "?colorPrimary"

private val CHROME_LAYOUT_FILES = listOf(
    "res/layout/nav_header_main.xml",
    "res/layout/editor_buttons_bars_linear_layout.xml",
    "res/layout/buttons_bars_linear_layout.xml",
    "res/layout/autocomplete_item.xml",
    "res/layout/layout_project_info.xml",
)

private fun replaceHardcodedPrimaryBackgrounds(document: Document, path: String) {
    val elements = document.getElementsByTagName("*")
    var replaced = 0

    for (index in 0 until elements.length) {
        val element = elements.item(index) as Element

        if (element.getAttribute("android:background") == HARDCODED_PRIMARY_BACKGROUND) {
            element.setAttribute("android:background", THEME_PRIMARY_BACKGROUND)
            replaced++
        }
    }

    if (replaced == 0) {
        throw PatchException("No $HARDCODED_PRIMARY_BACKGROUND background in $path")
    }
}

private fun cloneArrayWithExtraItem(document: Document, sourceName: String, newName: String, extraItem: String) {
    val source = document.getElementsByTagName("string-array")
        .findElementByAttributeValueOrThrow("name", sourceName)

    document.getNode("resources").adoptChild("string-array") {
        setAttribute("name", newName)

        source.childElementsSequence().forEach { item ->
            adoptChild("item") { textContent = item.textContent }
        }

        adoptChild("item") { textContent = extraItem }
    }
}

private val amoledThemeResourcesPatch = resourcePatch {
    execute {
        document("res/values/arrays.xml").use { document ->
            cloneArrayWithExtraItem(document, EDITOR_THEME_TITLES_ARRAY, DARK_EDITOR_THEME_TITLES_ARRAY, AMOLED_TITLE)
            cloneArrayWithExtraItem(document, EDITOR_THEME_VALUES_ARRAY, DARK_EDITOR_THEME_VALUES_ARRAY, AMOLED_VALUE)
        }

        document("res/xml/pref_appearance.xml").use { document ->
            val darkPreference = document.getElementsByTagName("ListPreference")
                .findElementByAttributeValueOrThrow("android:key", DARK_EDITOR_THEME_PREFERENCE_KEY)

            darkPreference.setAttribute("android:entries", "@array/$DARK_EDITOR_THEME_TITLES_ARRAY")
            darkPreference.setAttribute("android:entryValues", "@array/$DARK_EDITOR_THEME_VALUES_ARRAY")
        }

        document("res/values/styles.xml").use { document ->
            document.getNode("resources").adoptChild("style") {
                setAttribute("name", OVERLAY_STYLE)

                OVERLAY_ITEMS.forEach { (name, value) ->
                    adoptChild("item") {
                        setAttribute("name", name)
                        textContent = value
                    }
                }
            }
        }

        CHROME_LAYOUT_FILES.forEach { path ->
            document(path).use { document -> replaceHardcodedPrimaryBackgrounds(document, path) }
        }

        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element
            val name = application.getAttribute("android:name")

            if (name.isEmpty()) {
                throw PatchException("The manifest does not declare an Application class")
            }

            val packageName = document.documentElement.getAttribute("package")
            val qualifiedName = if (name.startsWith(".")) "$packageName$name" else name

            applicationClassType = "L${qualifiedName.replace('.', '/')};"
        }
    }
}

context(patchContext: BytecodePatchContext)
private fun Method.injectAmoledSchemeSelection(darkSchemeType: String, applyMethodName: String) {
    val tempRegister = implementation!!.registerCount
    val method = cloneParameters()

    val darkKeyIndex = method.indexOfFirstStringInstructionOrThrow(DARK_EDITOR_THEME_PREFERENCE_KEY)
    val insertionIndex = method.indexOfFirstInstructionOrThrow(
        darkKeyIndex,
        methodCall(definingClass = "Ljava/lang/String;", name = "equals"),
    )
    val valueRegister = method.getInstruction<FiveRegisterInstruction>(insertionIndex).registerC

    val invokeDirectIndex = method.indexOfFirstInstructionOrThrow(
        insertionIndex,
        methodCall(definingClass = darkSchemeType, name = "<init>", opcode = Opcode.INVOKE_DIRECT),
    )
    val schemeRegister = method.getInstruction<FiveRegisterInstruction>(invokeDirectIndex).registerC

    val storeIndex = invokeDirectIndex + 1
    val store = method.indexOfFirstInstruction(storeIndex, fieldAccess(opcode = Opcode.IPUT_OBJECT))
        .takeIf { it == storeIndex }
        ?.let { index ->
            val field = method.getInstruction<Instruction>(index).getReference<FieldReference>()!!
            val objectRegister = method.getInstruction<TwoRegisterInstruction>(index).registerB

            "iput-object v$schemeRegister, v$objectRegister, ${descriptor(field)}"
        } ?: ""

    method.addInstructionsWithLabels(
        insertionIndex,
        """
            const-string v$tempRegister, "$AMOLED_VALUE"
            invoke-virtual { v$valueRegister, v$tempRegister }, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
            move-result v$tempRegister
            if-eqz v$tempRegister, :skip_amoled
            new-instance v$schemeRegister, $darkSchemeType
            invoke-direct { v$schemeRegister }, $darkSchemeType-><init>()V
            invoke-virtual { v$schemeRegister }, $darkSchemeType->$applyMethodName()V
            $store
            :skip_amoled
            nop
        """,
    )
}

@Suppress("unused")
val amoledThemePatch = bytecodePatch(
    name = "AMOLED dark theme",
    description = "Adds an AMOLED option to Settings > Appearance > Editor theme (dark). " +
        "Applies only while the Dark theme is active.",
) {
    compatibleWith(*AppCompatibilities.IIEC_APPS)

    dependsOn(bypassSignatureCheckPatch, amoledThemeResourcesPatch)
    extendWith("extensions/extension.mpe")

    execute {
        val constructorMatch = DarkSchemeConstructorFingerprint.matchSingle()
        val darkSchemeType = constructorMatch.classDef.type

        val backgroundField = constructorMatch.instructionMatches.first()
            .getInstruction<Instruction>().getReference<FieldReference>()!!
        val setterMethod = constructorMatch.instructionMatches.last()
            .getInstruction<Instruction>().getReference<MethodReference>()!!

        mutableClassDefBy(constructorMatch.classDef).addMethod(
            APPLY_AMOLED_METHOD,
            emptyList(),
            "V",
            AccessFlags.PUBLIC.value,
            3,
            """
                sget-object v0, ${descriptor(backgroundField)}
                const v1, $BLACK_ARGB_SMALI
                invoke-virtual { p0, v0, v1 }, ${descriptor(setterMethod)}
                return-void
            """,
        )

        EditorThemeSelectionFingerprint.matchAll(2..2).forEach { match ->
            match.method.injectAmoledSchemeSelection(darkSchemeType, APPLY_AMOLED_METHOD)
        }

        val applicationType = applicationClassType
            ?: throw PatchException("Could not determine the Application class")

        mutableClassDefBy(applicationType).methods.single {
            it.name == ON_CREATE_METHOD && it.parameters.isEmpty()
        }.addInstructions(
            0,
            "invoke-static { p0 }, $EXTENSION_CLASS->attach(Landroid/app/Application;)V",
        )
    }
}
