/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.shortcuts

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.findFreeRegister
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/terabox/HomeShortcutFilter;"

private const val HOME_SHORTCUTS_CONFIG_KEY = "na_ai_new_home_search_tool_lv1"
private const val MORE_SHORTCUTS_CONFIG_KEY = "na_ai_new_home_search_tool_lv2"
private const val SEARCH_SHORTCUTS_CONFIG_KEY = "na_ai_search_tool_config_new_convenience"

private enum class Shortcut(
    val optionKey: String,
    val label: String,
    val types: List<String>,
    val groupTitleKey: String? = null,
    val hiddenByDefault: Boolean = true,
) {
    CREATIVE_GENERATION(
        "hideCreativeGeneration",
        "Creative Generation",
        listOf("beautify", "ai_video", "ai_transcribe", "horoscope", "tarot", "mbti", "dream", "love_luck"),
        "home_lv2_group_creation",
        hiddenByDefault = false,
    ),
    TRANSLATE("hideTranslate", "Translate", listOf("ai_translate")),
    SHORT_DRAMA("hideShortDrama", "Short Drama", listOf("drama")),
    TRENDING("hideTrending", "Trending", listOf("hotlist")),
}

@Suppress("unused")
val hideHomeShortcutsPatch = bytecodePatch(
    name = "Hide home shortcuts",
    description = "Hides Translate, Short Drama and Trending from the home screen and the More screen. " +
        "An option hides the Creative Generation tools too.",
) {
    compatibleWith(AppCompatibilities.TERABOX)

    extendWith("extensions/extension.mpe")

    val hideShortcutOptions = Shortcut.entries.associateWith { shortcut ->
        booleanOption(
            key = shortcut.optionKey,
            default = shortcut.hiddenByDefault,
            title = "Hide ${shortcut.label}",
            description = "Hides ${shortcut.label} from the home screen and the More screen.",
        )
    }

    execute {
        val hidden = hideShortcutOptions.filterValues { it.value == true }.keys
        if (hidden.isEmpty()) return@execute

        val types = hidden.flatMap { it.types }.joinToString(",")
        val groupTitleKeys = hidden.mapNotNull { it.groupTitleKey }.joinToString(",")

        listOf(HOME_SHORTCUTS_CONFIG_KEY, MORE_SHORTCUTS_CONFIG_KEY, SEARCH_SHORTCUTS_CONFIG_KEY).forEach { configKey ->
            shortcutConfigFingerprint(configKey).matchSingle().apply {
                val returnIndex = instructionMatches.last().index
                val configRegister = instructionMatches.last().getInstruction<OneRegisterInstruction>().registerA
                val valueRegister = method.findFreeRegister(returnIndex, configRegister)

                val filterCalls = buildList {
                    add(
                        "$EXTENSION_CLASS->removeShortcuts(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;" to
                            types,
                    )
                    if (configKey == MORE_SHORTCUTS_CONFIG_KEY && groupTitleKeys.isNotEmpty()) {
                        add(
                            "$EXTENSION_CLASS->removeShortcutGroups(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;" to
                                groupTitleKeys,
                        )
                    }
                }

                method.addInstructions(
                    returnIndex,
                    filterCalls.joinToString("\n") { (filterMethod, values) ->
                        """
                            const-string v$valueRegister, "$values"
                            invoke-static { v$configRegister, v$valueRegister }, $filterMethod
                            move-result-object v$configRegister
                        """
                    },
                )
            }
        }
    }
}
