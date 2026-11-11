/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.shared

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.readera.misc.fullscreen.RootPreferencesFingerprint
import app.morphe.patches.readera.misc.highlightcolors.QuoteColorIconFingerprint
import app.morphe.patches.readera.misc.highlightcolors.prefsReloadFingerprint
import app.morphe.patches.readera.misc.highlightcolors.prefsStoreFingerprint
import app.morphe.patches.shared.misc.smali.replaceMethod
import app.morphe.util.asSequence
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

internal const val READERA_SETTINGS = "Lapp/hxreborn/extension/readera/PatchSettings;"
private const val SETTINGS_ACTIVITY = "app.hxreborn.extension.readera.PatchesSettingsActivity"
private const val GENERAL_CATEGORY_TITLE = "General"
private val appliedExplanations = sortedMapOf<String, String>()
private val drawables = listOf(
    "footnote", "headers", "omit", "fullscreen", "fullscreenExit",
    "reset", "info", "bundle", "back", "check", "switch_thumb", "switch_thumb_on", "switch_thumb_off", "switch_track",
)

internal fun registerReadEraPatch(name: String, explanation: String) {
    appliedExplanations[name] = explanation
}

internal val patchesSettingsPatch = bytecodePatch {
    dependsOn(
        disableSignatureChecksPatch,
        resourcePatch {
            execute {
                val generalTitleName = stringNameByEnglishText(GENERAL_CATEGORY_TITLE)
                val screen = decode("res/xml/*.xml").single { path ->
                    get(path).readText().contains("<org.readera.pref.AboutReadEraPref")
                }
                document(screen).use { document ->
                    val general = document.documentElement.childNodes.asSequence().filterIsInstance<Element>()
                        .firstOrNull { it.tagName == "PreferenceCategory" }
                        ?: throw PatchException("No settings category in $screen")
                    val firstCategoryTitle = general.getAttribute("android:title")
                    if (firstCategoryTitle != "@string/$generalTitleName") {
                        throw PatchException(
                            "First settings category of $screen has title $firstCategoryTitle, " +
                                "expected @string/$generalTitleName ($GENERAL_CATEGORY_TITLE)",
                        )
                    }
                    val about = document.getElementsByTagName("org.readera.pref.AboutReadEraPref").asSequence().single() as Element
                    val row = document.createElement("Preference")
                    row.setAttribute("android:title", "hxreborn patches")
                    row.setAttribute("android:key", "hx_readera_patches_menu")
                    (0 until about.attributes.length).map { about.attributes.item(it) }.filter { it.nodeName.startsWith("app:") }.forEach {
                        row.setAttribute(it.nodeName, it.nodeValue)
                    }
                    val intent = document.createElement("intent")
                    intent.setAttribute("android:targetPackage", "org.readera")
                    intent.setAttribute("android:targetClass", SETTINGS_ACTIVITY)
                    row.appendChild(intent)
                    general.insertBefore(row, general.firstChild)
                }
                document("AndroidManifest.xml").use { document ->
                    val activity = document.createElement("activity")
                    activity.setAttribute("android:name", SETTINGS_ACTIVITY)
                    activity.setAttribute("android:exported", "false")
                    activity.setAttribute("android:theme", "@android:style/Theme.DeviceDefault.NoActionBar")
                    document.getElementsByTagName("application").item(0).appendChild(activity)
                }
                drawables.forEach { name ->
                    val path = "readera/drawable/hx_readera_${name.lowercase()}.xml"
                    SettingsResources::class.java.getResourceAsStream("/$path")!!.use { input ->
                        get("res/drawable/hx_readera_${name.lowercase()}.xml").outputStream().use(input::copyTo)
                    }
                }
            }
        },
    )
    extendWith("extensions/extension.mpe")

    execute {
        appliedExplanations.clear()
        RootPreferencesFingerprint.matchAll(1..1)
        val (settingsAccessor) = QuoteColorIconFingerprint.matchAll(1..1).single().instructionMatches
        val settingsClass = settingsAccessor.instruction.getReference<MethodReference>()!!.definingClass
        val store = prefsStoreFingerprint(settingsClass).matchAll(1..1).single().method
        val reload = prefsReloadFingerprint(settingsClass).matchAll(1..1).single().method
        mutableClassDefBy(READERA_SETTINGS).apply {
            replaceMethod(
                "nativePreferences", 1,
                """
                    invoke-static { }, $store
                    move-result-object v0
                    return-object v0
                """,
            )
            replaceMethod(
                "reloadNativeSettings", 0,
                """
                    invoke-static { }, $reload
                    return-void
                """,
            )
        }
    }

    finalize {
        mutableClassDefBy(READERA_SETTINGS).methods.single { it.name == "appliedPatches" }
            .returnEarly(appliedExplanations.entries.joinToString("\\n") { (name, explanation) -> "$name\\t$explanation" })
    }
}

private object SettingsResources
