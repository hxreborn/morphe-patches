/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.shared

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.decode
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.readera.misc.fullscreen.RootPreferencesFingerprint
import app.morphe.patches.readera.misc.highlightcolors.QuoteColorIconFingerprint
import app.morphe.patches.readera.misc.highlightcolors.prefsReloadFingerprint
import app.morphe.patches.readera.misc.highlightcolors.prefsStoreFingerprint
import app.morphe.patches.shared.misc.hostedcomponents.CORE_COMPONENT_FACTORY
import app.morphe.patches.shared.misc.hostedcomponents.ComponentHost
import app.morphe.patches.shared.misc.hostedcomponents.HostedComponents
import app.morphe.patches.shared.misc.hostedcomponents.addStyle
import app.morphe.patches.shared.misc.hostedcomponents.replaceThemeStub
import app.morphe.patches.shared.misc.smali.replaceMethod
import app.morphe.util.asSequence
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element
import app.morphe.patches.shared.misc.extension.readeraSettingsExtensionPatch

internal const val READERA_SETTINGS = "Lapp/hxreborn/extension/readera/PatchSettings;"
private const val SETTINGS_ACTIVITY = "app.hxreborn.extension.readera.PatchesSettingsActivity"
private const val SETTINGS_ACTIVITY_TYPE = "Lapp/hxreborn/extension/readera/PatchesSettingsActivity;"
private const val SETTINGS_STYLE = "hx_readera_settings"
private const val SETTINGS_THEME = "@android:style/Theme.DeviceDefault.NoActionBar"
private const val PREFERENCES_ACTIVITY = "org.readera.pref.PrefsActivity"
internal val readEraScreenHost = ComponentHost("activity", "org.readera.AboutAppActivity")
internal val readEraHostedComponents = HostedComponents(CORE_COMPONENT_FACTORY)
private const val GENERAL_CATEGORY_TITLE = "General"
private val appliedExplanations = sortedMapOf<String, String>()
private val drawables = listOf(
    "headphones", "footnote", "headers", "omit", "fullscreen", "fullscreenExit",
    "reset", "info", "bundle", "back", "check", "share", "switch_thumb", "switch_thumb_on", "switch_thumb_off", "switch_track",
)

internal fun registerReadEraPatch(name: String, explanation: String) {
    appliedExplanations[name] = explanation
}

internal val patchesSettingsPatch = patch {
    dependsOn(readeraSettingsExtensionPatch)
    dependsOn(
        disableSignatureChecksPatch,
        readEraHostedComponents.patch,
        patch {
            execute {
                val generalTitleName = stringNameByEnglishText(GENERAL_CATEGORY_TITLE)
                val screen = decode("res/xml/*.xml").single { path ->
                    resourceFile(path).readText().contains("<org.readera.pref.AboutReadEraPref")
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
                    activity.setAttribute("android:theme", "@style/$SETTINGS_STYLE")
                    document.getElementsByTagName("application").item(0).appendChild(activity)
                }
                mutableClassDefBy(SETTINGS_ACTIVITY_TYPE)
                    .replaceThemeStub("settingsTheme", addStyle(SETTINGS_STYLE, SETTINGS_THEME))
                drawables.forEach { name ->
                    val path = "readera/drawable/hx_readera_${name.lowercase()}.xml"
                    SettingsResources::class.java.getResourceAsStream("/$path")!!.use { input ->
                        resourceFile("res/drawable/hx_readera_${name.lowercase()}.xml").outputStream().use(input::copyTo)
                    }
                }
            }
        },
    )
    execute {
        appliedExplanations.clear()
        readEraHostedComponents.host(SETTINGS_ACTIVITY, readEraScreenHost)
        readEraHostedComponents.routeLaunchesFrom(PREFERENCES_ACTIVITY)
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
