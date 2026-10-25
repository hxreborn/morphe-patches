/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import org.w3c.dom.Element

internal const val FEATURES_CLASS = "Lapp/hxreborn/extension/netanalyzer/Features;"
internal const val EXTENSION_PACKAGE = "app.hxreborn.extension.netanalyzer"
private const val ACTIVITY_THEME = "@android:style/Theme.DeviceDefault.DayNight"
private const val PATCHES_SECTION_METHOD = "hxPatchesSection"
private const val PATCHES_SECTION_HEADER = "Patches"
private const val PATCHES_MENU_TITLE = "hxreborn patches"
private const val PATCHES_MENU_ACTIVITY = "PatchesMenuActivity"
private const val MENU_ENTRY_SEPARATOR = ";"
private const val MENU_FIELD_SEPARATOR = "|"
private const val ABOUT_SECTION_BELOW = "Support"

private const val ROW_TITLE = "keyText"
private const val ROW_VALUE = "valueText"
private const val ROW_DIVIDER = "showDivider"
private const val ROW_ON_CLICK = "onClick"
private const val HEADER_TITLE = "title"
private const val COMPOSE_CHANGED_UNCERTAIN = 0
private const val SYNTHETIC_COMPOSE_PARAMETERS = 3
private const val NO_DIVIDER = 0

internal enum class MenuSection(val title: String) {
    NETWORK("Network"),
    TOOLS("Tools"),
    SETTINGS("Settings"),
}

private class MenuEntry(val section: MenuSection, val title: String, val activity: String)

private val menuEntries = mutableListOf<MenuEntry>()

private val featureActivities = mutableListOf<String>()

internal fun addMenuEntry(section: MenuSection, title: String, activity: String) {
    menuEntries += MenuEntry(section, title, "$EXTENSION_PACKAGE.$activity")
    addFeatureActivity(activity)
}

internal fun addFeatureActivity(activity: String) {
    featureActivities += "$EXTENSION_PACKAGE.$activity"
}

private const val EXPORT_FILE_PROVIDER = "ExportFileProvider"
private const val EXPORT_PROVIDER_AUTHORITY_SUFFIX = ".netanalyzer-an.provider"

private val featureActivitiesPatch = resourcePatch {
    finalize {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element
            featureActivities.distinct().forEach { name ->
                val activity = document.createElement("activity")
                activity.setAttribute("android:name", name)
                activity.setAttribute("android:exported", "false")
                activity.setAttribute("android:theme", ACTIVITY_THEME)
                application.appendChild(activity)
            }
            val provider = document.createElement("provider")
            provider.setAttribute("android:name", "$EXTENSION_PACKAGE.$EXPORT_FILE_PROVIDER")
            provider.setAttribute("android:authorities", document.documentElement.getAttribute("package") + EXPORT_PROVIDER_AUTHORITY_SUFFIX)
            provider.setAttribute("android:exported", "false")
            provider.setAttribute("android:grantUriPermissions", "true")
            application.appendChild(provider)
        }
    }
}

internal val patchesMenuPatch = bytecodePatch {
    dependsOn(disableTamperDetectionPatch, hostPalettePatch, featureActivitiesPatch)
    extendWith("extensions/extension.mpe")

    execute {
        menuEntries.clear()
        featureActivities.clear()
        addFeatureActivity(PATCHES_MENU_ACTIVITY)
        MainActivityOnCreateFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p0 }, $FEATURES_CLASS->attach(Landroid/app/Activity;)V",
        )
    }

    finalize {
        if (menuEntries.isEmpty()) return@finalize

        val headerMatch = HeaderRowFingerprint.matchSingle()
        val rowMatch = ClickableRowFingerprint.matchSingle()
        val header = headerMatch.originalMethod
        val row = rowMatch.originalMethod
        val headerParameters = headerMatch.composableParameters()
        val rowParameters = rowMatch.composableParameters()
        header.requireParameterCount(HEADER_ROW, headerParameters)
        row.requireParameterCount(CLICKABLE_ROW, rowParameters)

        val aboutSection = Fingerprint(
            filters = listOf(decryptedString(ABOUT_SECTION_BELOW), methodCall(reference = header)),
        ).matchSingle()
        val sectionLabelIndex = aboutSection.instructionMatches.first().index
        val composerRegister = aboutSection.method.getInstruction<FiveRegisterInstruction>(
            aboutSection.instructionMatches.last().index,
        ).registerD
        val composerType = row.parameterTypes[rowParameters.size].toString()
        val onClickType = row.parameterTypes[rowParameters.indexOf(ROW_ON_CLICK)].toString()

        val callRegisters = rowParameters.size + SYNTHETIC_COMPOSE_PARAMETERS
        val rowsMethod = aboutSection.classDef.addMethod(
            PATCHES_SECTION_METHOD,
            listOf(composerType),
            "V",
            AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
            callRegisters + 1,
            buildString {
                append(
                    composableCall(header, headerParameters, emptyMap(), "p0") { parameter ->
                        if (parameter == HEADER_TITLE) "const-string v%d, \"$PATCHES_SECTION_HEADER\"" else null
                    },
                )
                val encodedEntries = menuEntries.sortedWith(compareBy({ it.section.ordinal }, { it.title }))
                    .joinToString(MENU_ENTRY_SEPARATOR) { entry ->
                        listOf(entry.section.title, entry.title, entry.activity).joinToString(MENU_FIELD_SEPARATOR)
                    }
                append(
                    composableCall(row, rowParameters, mapOf(ROW_VALUE to true), "p0") { parameter ->
                        when (parameter) {
                            ROW_TITLE -> "const-string v%d, \"$PATCHES_MENU_TITLE\""
                            ROW_DIVIDER -> "const/4 v%d, $NO_DIVIDER"
                            ROW_ON_CLICK -> """
                                const-class v%1${'$'}d, $onClickType
                                const-string v${callRegisters - 1}, "$encodedEntries"
                                invoke-static { v%1${'$'}d, v${callRegisters - 1} }, $FEATURES_CLASS->onMenuClick(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Object;
                                move-result-object v%1${'$'}d
                                check-cast v%1${'$'}d, $onClickType
                            """.trimIndent()
                            else -> null
                        }
                    },
                )
                appendLine("return-void")
            },
        )

        aboutSection.method.insertBeforeStringLoad(
            sectionLabelIndex,
            "invoke-static/range { v$composerRegister .. v$composerRegister }, ${descriptor(rowsMethod)}",
        )
    }
}

private fun Method.requireParameterCount(name: String, parameters: List<String>) {
    if (parameterTypes.size != parameters.size + SYNTHETIC_COMPOSE_PARAMETERS &&
        parameterTypes.size != parameters.size + SYNTHETIC_COMPOSE_PARAMETERS - 1
    ) {
        throw PatchException("Composable $name declares $parameters but takes ${parameterTypes.size} parameters")
    }
}

private fun composableCall(
    composable: Method,
    parameters: List<String>,
    defaulted: Map<String, Boolean>,
    composer: String,
    load: (String) -> String?,
): String = buildString {
    parameters.forEachIndexed { register, parameter ->
        appendLine(load(parameter)?.format(register) ?: "const/4 v$register, 0x0")
    }
    val composerRegister = parameters.size
    appendLine("move-object v$composerRegister, $composer")
    appendLine("const/4 v${composerRegister + 1}, $COMPOSE_CHANGED_UNCERTAIN")
    val hasDefaults = composable.parameterTypes.size == parameters.size + SYNTHETIC_COMPOSE_PARAMETERS
    if (hasDefaults) {
        val mask = defaulted.keys.fold(0) { bits, parameter -> bits or (1 shl parameters.indexOf(parameter)) }
        appendLine("const/16 v${composerRegister + 2}, $mask")
    }
    val lastRegister = composerRegister + if (hasDefaults) 2 else 1
    appendLine("invoke-static/range { v0 .. v$lastRegister }, ${descriptor(composable)}")
}
