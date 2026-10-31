/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pixlr.layout.home

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/pixlr/HomeSection;"

private enum class HomeSection(
    val sectionName: String,
    val optionKey: String,
    val title: String,
    val description: String,
) {
    FEATURED("features", "hideFeatured", "Hide featured carousel", "Hides the featured carousel at the top."),
    AI_CREATE("ai_create", "hideAiCreate", "Hide AI Create", "Hides the AI Create row."),
    AI_TOOLS("ai_tools", "hideAiTools", "Hide AI Tools", "Hides the AI Tools row."),
}

@Suppress("unused")
val hideHomeSectionsPatch = bytecodePatch(
    name = "Hide home sections",
    description = "Hides the selected sections from the home screen.",
) {
    compatibleWith(AppCompatibilities.PIXLR)
    extendWith("extensions/extension.mpe")

    val hideSectionOptions = HomeSection.entries.associateWith { section ->
        booleanOption(
            key = section.optionKey,
            default = true,
            title = section.title,
            description = section.description,
        )
    }

    val hideAiVideoOption = booleanOption(
        key = "hideAiVideo",
        default = true,
        title = "Hide AI Video",
        description = "Hides the AI Video banner and tiles.",
    )

    execute {
        val hiddenSections = hideSectionOptions.filterValues { it.value == true }.keys
        val hideAiVideo = hideAiVideoOption.value == true
        if (hiddenSections.isEmpty() && !hideAiVideo) return@execute

        HomeSectionConstructorFingerprint.matchSingle().method.apply {
            val nameRegister = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

            val disableSectionCalls = hiddenSections.joinToString("\n") { section ->
                """
                    const-string v$nameRegister, "${section.sectionName}"
                    invoke-static { p1, v$nameRegister }, $EXTENSION_CLASS_DESCRIPTOR->disable(Lorg/json/JSONObject;Ljava/lang/String;)V
                """
            }
            val removeAiVideoCall = if (hideAiVideo) {
                "invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->removeAiVideo(Lorg/json/JSONObject;)V"
            } else {
                ""
            }

            addInstructions(0, "$removeAiVideoCall\n$disableSectionCalls")
        }
    }
}
