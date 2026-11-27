/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.utorrent.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.addPatchInterface
import app.morphe.patcher.util.matchAllMethodIndicesForEach
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.hostedcomponents.CORE_COMPONENT_FACTORY
import app.morphe.patches.shared.misc.hostedcomponents.HostedComponents
import app.morphe.patches.shared.misc.hostedcomponents.addStyle
import app.morphe.patches.shared.misc.hostedcomponents.replaceThemeStub
import app.morphe.patches.shared.misc.hostedcomponents.licenseCheckActivityHost
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.patches.shared.misc.torrent.removeProtectionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import app.morphe.patches.shared.misc.extension.utorrentSettingsMenuExtensionPatch
import app.morphe.patches.shared.misc.extension.utorrentSettingsExtensionPatch

private const val EXTENSION_PACKAGE = "Lapp/hxreborn/extension/utorrent/"
private const val THEME_CLASS = "${EXTENSION_PACKAGE}AmoledTheme;"
private const val SETTINGS_CLASS = "${EXTENSION_PACKAGE}PatchSettings;"
private const val MENU_CLASS = "${EXTENSION_PACKAGE}PatchesMenu;"
private const val SWITCH_CLASS = "${EXTENSION_PACKAGE}HostSwitch;"
private const val SETTINGS_ACTIVITY = "app.hxreborn.extension.utorrent.PatchesSettingsActivity"
private const val ANDROID_BLACK = 0x0106000cL

private val DARK_COLORS = mapOf(
    "color_controller_bg_dark" to 1,
    "color_dialog_bg_dark" to 4,
    "color_main_content_dark" to 1,
    "color_status_dark" to 6,
    "color_dialog_edit_bg_dark" to 1,
)
private val DARK_DRAWABLES = mapOf(
    "bg_torrentlist_dark" to 1,
    "bg_dialog_add_torrent_dark" to 1,
    "bg_albumlist_dark" to 1,
    "bg_music_queue_dark" to 1,
    "bg_miniplayer_background_dark" to 2,
    "bg_add_torrent_dilog_edit_dark" to 1,
    "bg_edit_search_torrent_dark" to 1,
    "bg_guild_totop_txt_dark" to 1,
    "bg_updatesuper_top_dark" to 1,
    "bg_main_top_img_dark" to 1,
)
private lateinit var amoledResourceIds: AmoledResourceIds
private val hostedComponents = HostedComponents(CORE_COMPONENT_FACTORY)

private val amoledResourcesPatch = patch {
    dependsOn(utorrentSettingsExtensionPatch, hostedComponents.patch)
    execute {
        amoledResourceIds = addAmoledResources(DARK_DRAWABLES.keys, SETTINGS_ACTIVITY)
        mutableClassDefBy("${EXTENSION_PACKAGE}PatchesSettingsActivity;")
            .replaceThemeStub("menuTheme", addStyle(SETTINGS_STYLE, SETTINGS_THEME))
        hostedComponents.host(SETTINGS_ACTIVITY, licenseCheckActivityHost)
    }
}

@Suppress("unused")
val amoledThemePatch = patch(
    name = "AMOLED dark theme",
    description = "Adds an option to make dark surfaces pure black.",
) {
    dependsOn(utorrentSettingsMenuExtensionPatch)
    compatibleWith(AppCompatibilities.UTORRENT)
    availability(requireArm64)
    dependsOn(removeProtectionPatch, amoledResourcesPatch)

    execute {
        val (drawableIds, switchLayoutId) = amoledResourceIds
        val themeClass = surfaceResourceFingerprint("color_controller_bg_dark").match().classDef.type
        val darkMode = darkModeFingerprint(themeClass).method
        darkMode.addInstructions(0, "invoke-static/range { p0 .. p0 }, $SETTINGS_CLASS->initialize(Landroid/content/Context;)V")
        mutableClassDefBy(SETTINGS_CLASS).replaceStub(
            "isDark", 2,
            """
                invoke-static { p0 }, ${descriptor(darkMode)}
                move-result v0
                return v0
            """,
        )

        val replacements = DARK_COLORS.keys.associate { resourceId(ResourceType.COLOR, it) to ANDROID_BLACK } +
            DARK_DRAWABLES.keys.associate { resourceId(ResourceType.DRAWABLE, it) to drawableIds.getValue(it) }
        val entries = replacements.entries.sortedBy { it.key }
        mutableClassDefBy(THEME_CLASS).replaceStub(
            "resourceId", 2,
            buildString {
                appendLine("invoke-static { }, $SETTINGS_CLASS->isEnabled()Z")
                appendLine("move-result v0")
                appendLine("if-eqz v0, :stock")
                appendLine("sparse-switch p0, :mapping")
                appendLine(":stock")
                appendLine("return p0")
                entries.forEachIndexed { index, entry ->
                    appendLine(":surface_$index")
                    appendLine("const p0, ${entry.value}")
                    appendLine("return p0")
                }
                appendLine(":mapping")
                appendLine(".sparse-switch")
                entries.forEachIndexed { index, entry -> appendLine("${entry.key} -> :surface_$index") }
                appendLine(".end sparse-switch")
            },
        )
        (DARK_COLORS + DARK_DRAWABLES).forEach { (resourceName, expectedReads) ->
            var reads = 0
            surfaceResourceFingerprint(resourceName).matchAllMethodIndicesForEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                addInstructions(index + 1, """
                    invoke-static/range { v$register .. v$register }, $THEME_CLASS->resourceId(I)I
                    move-result v$register
                """)
                reads++
            }
            if (reads != expectedReads) {
                throw PatchException("Expected $expectedReads background reads for $resourceName, found $reads")
            }
        }

        val banner = LowPowerBannerFingerprint.method
        banner.addInstructions(banner.implementation!!.instructions.lastIndex,
            "invoke-static/range { p0 .. p0 }, $THEME_CLASS->lowPowerBanner(Landroid/view/View;)V")

        val settingsTheme = SettingsThemeFingerprint.method
        val returns = settingsTheme.implementation!!.instructions.withIndex()
            .filter { it.value.opcode == Opcode.RETURN_VOID }.map { it.index }
        if (returns.size != 1) throw PatchException("Expected one settings theme return")
        settingsTheme.addInstructions(returns.single(),
            "invoke-static/range { p0 .. p0 }, $MENU_CLASS->attach(Landroid/app/Activity;)V")
        mutableClassDefBy(MENU_CLASS).replaceResourceIdStubs(ResourceType.ID, mapOf(
            "downloadFolderRowId" to "rl_download_folder", "downloadFolderLabelId" to "tv_default_folder",
        ))
        mutableClassDefBy("${EXTENSION_PACKAGE}SettingsStyle;").apply {
            replaceResourceIdStubs(ResourceType.COLOR, mapOf(
                "darkBackgroundId" to "color_controller_bg_dark",
                "darkDividerId" to "color_dialog_line_dark", "lightDividerId" to "color_dialog_line",
                "darkTitleId" to "color_dialog_title_dark", "lightTitleId" to "color_dialog_title",
                "darkLabelId" to "color_unselect_tab_dark", "lightLabelId" to "color_unselect_tab",
                "darkAccentId" to "primaryColor_dark", "lightAccentId" to "primaryColor",
            ))
            replaceResourceIdStubs(ResourceType.DRAWABLE, mapOf(
                "themeIconId" to "icon_theme", "resetIconId" to "icon_music_mode_recycle",
                "infoIconId" to "icon_about", "patchesIconId" to "icon_setting",
                "backIconId" to "icon_back_white", "chevronIconId" to "icon_music_retract_dark",
            ))
        }
        val hostSwitchType = "Lcom/bittorrent/app/view/CustomSwitch;"
        val hostSwitch = classDefBy(hostSwitchType)
        val setter = hostSwitch.methods.single { it.name == "setOnCheckedChangeListener" }
        val listenerType = setter.parameterTypes.single().toString()
        val callback = classDefBy(listenerType).methods.single {
            it.parameterTypes == listOf("Z") && it.returnType == "V"
        }
        mutableClassDefBy(SWITCH_CLASS).apply {
            val change = methods.single { it.name == "change" && it.parameterTypes == listOf("Z") }
            addPatchInterface(listenerType, callback.name, change, listOf("Z"))
            replaceStub("create", 5, """
                invoke-static { p0 }, Landroid/view/LayoutInflater;->from(Landroid/content/Context;)Landroid/view/LayoutInflater;
                move-result-object v0
                const v1, $switchLayoutId
                const/4 v2, 0x0
                invoke-virtual { v0, v1, v2 }, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;)Landroid/view/View;
                move-result-object v0
                check-cast v0, $hostSwitchType
                invoke-virtual { v0, p1 }, $hostSwitchType->setOpenColor(I)V
                return-object v0
            """)
            replaceStub("setChecked", 2, """
                check-cast p0, $hostSwitchType
                invoke-virtual { p0, p1 }, $hostSwitchType->setChecked(Z)V
                return-void
            """)
            replaceStub("bind", 2, """
                check-cast p0, $hostSwitchType
                invoke-virtual { p0, p1 }, ${descriptor(setter)}
                return-void
            """)
        }
    }
}
