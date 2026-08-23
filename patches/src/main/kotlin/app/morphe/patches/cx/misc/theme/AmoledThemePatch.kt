/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.cx.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.adoptChild
import app.morphe.util.findElementByAttributeValueOrThrow
import app.morphe.util.findMutableMethodOf
import app.morphe.util.getNode
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.ClassDef

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/cx/AmoledTheme;"
private const val STRING_CLASS = "Ljava/lang/String;"
private const val APP_CLASS_PREFIX = "Lcom/alphainventor/filemanager/"
private const val ACTIVITY_CLASS = "Landroid/app/Activity;"
private const val BUNDLE_CLASS = "Landroid/os/Bundle;"
private const val ON_CREATE_METHOD = "onCreate"
private const val PREFERENCE_CHANGED_METHOD = "onSharedPreferenceChanged"
private const val MAX_HIERARCHY_DEPTH = 16

private const val BLACK = "#000000"
private const val OVERLAY_STYLE = "hx_amoled_theme_overlay"
private const val THEME_PREFERENCE_KEY = "night_mode"
private const val AMOLED_PREFERENCE_KEY = "hx_amoled"

// Not localized in all languages
private const val AMOLED_TITLE = "Prefer pure black in dark theme"

private val OVERLAY_ITEMS = mapOf(
    "android:colorBackground" to BLACK,
    "android:windowBackground" to BLACK,
)

private fun ClassDef.onCreateOrNull() = methods.firstOrNull {
    it.name == ON_CREATE_METHOD &&
        it.parameterTypes.map(CharSequence::toString) == listOf(BUNDLE_CLASS)
}

private fun BytecodePatchContext.extendsActivity(classDef: ClassDef) =
    generateSequence(classDef.superclass) { classDefByOrNull(it)?.superclass }
        .take(MAX_HIERARCHY_DEPTH)
        .any { it == ACTIVITY_CLASS }

private val amoledThemeResourcesPatch = resourcePatch {
    execute {
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

        document("res/xml/settings.xml").use { document ->
            val themePreference = document.getElementsByTagName("ListPreference")
                .findElementByAttributeValueOrThrow("android:key", THEME_PREFERENCE_KEY)

            val amoledPreference = document.createElement("CheckBoxPreference").apply {
                setAttribute("android:key", AMOLED_PREFERENCE_KEY)
                setAttribute("android:title", AMOLED_TITLE)
                setAttribute("android:defaultValue", "false")
                setAttribute("app:iconSpaceReserved", "false")
            }

            themePreference.parentNode.insertBefore(amoledPreference, themePreference.nextSibling)
        }
    }
}

@Suppress("unused")
val amoledThemePatch = bytecodePatch(
    name = "AMOLED dark theme",
    description = "Adds a pure black option to the dark theme.",
) {
    compatibleWith(AppCompatibilities.CX_FILE_EXPLORER)

    dependsOn(darkThemePatch, amoledThemeResourcesPatch)
    extendWith("extensions/extension.mpe")

    execute {
        val settingsClassDef = ThemePreferenceFingerprint.matchSingle().classDef
        val preferenceChanged = settingsClassDef.methods.single {
            it.name == PREFERENCE_CHANGED_METHOD
        }

        mutableClassDefBy(settingsClassDef).findMutableMethodOf(preferenceChanged).addInstructions(
            0,
            """
                invoke-static { p2 }, $EXTENSION_CLASS->themeKey($STRING_CLASS)$STRING_CLASS
                move-result-object p2
            """,
        )

        var themedActivities = 0

        classDefForEach { classDef ->
            if (!classDef.type.startsWith(APP_CLASS_PREFIX)) return@classDefForEach

            val onCreate = classDef.onCreateOrNull() ?: return@classDefForEach
            if (!extendsActivity(classDef)) return@classDefForEach

            mutableClassDefBy(classDef).findMutableMethodOf(onCreate).addInstructions(
                0,
                "invoke-static { p0 }, $EXTENSION_CLASS->applyOverlay($ACTIVITY_CLASS)V",
            )
            themedActivities++
        }

        if (themedActivities == 0) {
            throw PatchException("Could not find any activity to apply the AMOLED overlay to")
        }
    }
}
