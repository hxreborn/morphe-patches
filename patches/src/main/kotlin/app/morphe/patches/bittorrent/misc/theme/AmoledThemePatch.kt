/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bittorrent.misc.theme

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
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import app.morphe.patches.shared.misc.extension.bittorrentSettingsMenuAndAmoledThemeExtensionPatch
import app.morphe.patches.shared.misc.extension.bittorrentSettingsExtensionPatch

private const val EXTENSION_PACKAGE = "Lapp/hxreborn/extension/bittorrent/"
private const val THEME_CLASS = "${EXTENSION_PACKAGE}AmoledTheme;"
private const val SETTINGS_CLASS = "${EXTENSION_PACKAGE}PatchSettings;"
private const val MENU_CLASS = "${EXTENSION_PACKAGE}PatchesMenu;"
private const val SWITCH_CLASS = "${EXTENSION_PACKAGE}HostSwitch;"
private const val SETTINGS_ACTIVITY = "app.hxreborn.extension.bittorrent.PatchesSettingsActivity"
private const val HOST_SWITCH_CLASS = "Lcom/bittorrent/base/view/CustomSwitch;"
private val DARK_DRAWABLES = mapOf(
    "bg_dialog_body_dark" to 4,
    "bg_snackbar_delete_dark" to 3,
    "bg_none_list_dark" to 3,
    "bg_torrent_detail_button_dark" to 2,
    "bg_torrent_resume_all_dark" to 1,
    "bg_torrent_manage_dark" to 1,
    "bg_torrent_bottom_add_dark" to 1,
    "bg_mini_player_dark" to 1,
    "bg_artist_icon_dark" to 1,
)
private lateinit var amoledResourceIds: AmoledResourceIds
private val hostedComponents = HostedComponents(CORE_COMPONENT_FACTORY)

private val amoledResourcesPatch = patch {
    dependsOn(bittorrentSettingsExtensionPatch, hostedComponents.patch)
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
    description = "Adds a setting that makes dialogs, snackbars, buttons and cards pure black in dark mode.",
) {
    dependsOn(bittorrentSettingsMenuAndAmoledThemeExtensionPatch)
    compatibleWith(AppCompatibilities.BITTORRENT)
    availability(requireArm64)
    dependsOn(removeProtectionPatch, amoledResourcesPatch)

    execute {
        val (drawableIds, switchLayoutId) = amoledResourceIds
        val darkMode = DarkModeFingerprint.method
        darkMode.addInstructions(0, "invoke-static/range { p0 .. p0 }, $SETTINGS_CLASS->initialize(Landroid/content/Context;)V")
        mutableClassDefBy(SETTINGS_CLASS).replaceStub(
            "isDark", 2,
            """
                invoke-static { p0 }, ${descriptor(darkMode)}
                move-result v0
                return v0
            """,
        )

        val replacements = DARK_DRAWABLES.keys
            .associate { resourceId(ResourceType.DRAWABLE, it) to drawableIds.getValue(it) }
            .entries.sortedBy { it.key }
        mutableClassDefBy(THEME_CLASS).replaceStub(
            "resourceId", 2,
            buildString {
                appendLine("invoke-static { }, $SETTINGS_CLASS->isEnabled()Z")
                appendLine("move-result v0")
                appendLine("if-eqz v0, :stock")
                appendLine("sparse-switch p0, :mapping")
                appendLine(":stock")
                appendLine("return p0")
                replacements.forEachIndexed { index, entry ->
                    appendLine(":surface_$index")
                    appendLine("const p0, ${entry.value}")
                    appendLine("return p0")
                }
                appendLine(":mapping")
                appendLine(".sparse-switch")
                replacements.forEachIndexed { index, entry -> appendLine("${entry.key} -> :surface_$index") }
                appendLine(".end sparse-switch")
            },
        )
        DARK_DRAWABLES.forEach { (resourceName, expectedReads) ->
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

        SettingsResumeFingerprint.method.addInstructions(
            0,
            "invoke-static/range { p0 .. p0 }, $MENU_CLASS->attach(Landroid/app/Activity;)V",
        )
        mutableClassDefBy(MENU_CLASS).replaceResourceIdStubs(ResourceType.ID, mapOf("incomingPortTitleId" to ANCHOR_ROW_ID))
        mutableClassDefBy("${EXTENSION_PACKAGE}SettingsStyle;").apply {
            replaceResourceIdStubs(ResourceType.COLOR, mapOf(
                "darkBackgroundId" to "color_000000",
                "darkDividerId" to "color_dialog_line_dark", "lightDividerId" to "color_dialog_line",
                "darkTitleId" to "color_dialog_title_dark", "lightTitleId" to "color_dialog_title",
                "darkLabelId" to "color_unselect_tab_dark", "lightLabelId" to "color_unselect_tab",
                "darkAccentId" to "primaryColor_dark", "lightAccentId" to "primaryColor",
            ))
            replaceResourceIdStubs(ResourceType.DRAWABLE, mapOf(
                "themeIconId" to "icon_theme", "resetIconId" to "ic_round_replay_24",
                "infoIconId" to "icon_about", "patchesIconId" to "icon_setting",
                "backIconId" to "icon_back_white"
            ))
        }

        val setter = classDefBy(HOST_SWITCH_CLASS).methods.single { it.name == "setCheckedChangeListener" }
        val listenerType = setter.parameterTypes.single().toString()
        mutableClassDefBy(SWITCH_CLASS).apply {
            addPatchInterface(listenerType, "invoke",
                """
                    check-cast p1, Ljava/lang/Boolean;
                    invoke-virtual { p1 }, Ljava/lang/Boolean;->booleanValue()Z
                    move-result p1
                    invoke-virtual { p0, p1 }, $SWITCH_CLASS->change(Z)V
                    const/4 v0, 0x0
                    return-object v0
                """)
            replaceStub("create", 5, """
                invoke-static { p0 }, Landroid/view/LayoutInflater;->from(Landroid/content/Context;)Landroid/view/LayoutInflater;
                move-result-object v0
                const v1, $switchLayoutId
                const/4 v2, 0x0
                const/4 v3, 0x0
                invoke-virtual { v0, v1, v2, v3 }, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;
                move-result-object v0
                check-cast v0, $HOST_SWITCH_CLASS
                invoke-virtual { v0, p1 }, $HOST_SWITCH_CLASS->setOpenColor(I)V
                return-object v0
            """)
            replaceStub("setChecked", 2, """
                check-cast p0, $HOST_SWITCH_CLASS
                invoke-virtual { p0, p1 }, $HOST_SWITCH_CLASS->setChecked(Z)V
                return-void
            """)
            replaceStub("bind", 2, """
                check-cast p0, $HOST_SWITCH_CLASS
                invoke-virtual { p0, p1 }, ${descriptor(setter)}
                return-void
            """)
        }
    }
}
