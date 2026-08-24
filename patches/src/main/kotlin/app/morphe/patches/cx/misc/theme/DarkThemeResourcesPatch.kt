/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.cx.misc.theme

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.adoptChild
import app.morphe.util.childElementsSequence
import app.morphe.util.findElementByAttributeValueOrThrow
import app.morphe.util.getNode
import app.morphe.util.insertFirst

private const val THEME_PREFERENCE_KEY = "night_mode"
private const val DARK_COLOR_PREFIX = "dark_"

// Not localized in all languages
private const val APPEARANCE_CATEGORY_TITLE = "Appearance"

private val NIGHT_STYLES = mapOf(
    "App.Theme.BottomSheetDialog.DayNight" to "@style/Theme.Design.BottomSheetDialog",
    "ToolbarStyle.Default" to "@style/ToolbarStyle.Dark",
    "ToolbarSubtitleStyle.Default" to "@style/ToolbarSubtitleStyle.Dark",
    "ToolbarTitleStyle.Default" to "@style/ToolbarTitleStyle.Dark",
)

private val NIGHT_STYLE_ITEMS = mapOf(
    "App.Theme.BottomSheetDialog.DayNight" to mapOf(
        "android:colorBackground" to "@color/dark_color_surface_container_high",
    ),
)

private val CHROME_COLORS = mapOf(
    "actionbar_foreground_color" to "@color/dark_primary_text",
    "actionbar_light_popup_background" to "@color/dark_color_surface_container_high",
    "actionbar_separator" to "#33ffffff",
    "backup_color_normal" to "@color/dark_primary_text",
    "batch_rename_list_background" to "@color/dark_color_surface_container",
    "box_background" to "@color/dark_color_surface_container",
    "box_hint_foreground" to "#80ffffff",
    "box_primary_text" to "#deffffff",
    "cardfragment_background" to "@color/dark_color_surface",
    "cardview_background" to "@color/dark_color_surface_container",
    "cardview_light_background" to "@color/dark_color_surface_container",
    "cx_ftp_server_content_background" to "@color/dark_color_surface",
    "desktop2_tab_background_border" to "#33ffffff",
    "desktop2_tab_text_normal" to "#88ffffff",
    "desktop_native_ads_background" to "@color/dark_color_surface_container",
    "dialog_tab_text_default" to "#f0ffffff",
    "dialog_tab_text_disabled" to "#80ffffff",
    "fast_scrollbar_thumb_normal" to "#8affffff",
    "file_list_checkbox_blank" to "#ccffffff",
    "icon_home_background" to "@color/dark_color_surface_container_high",
    "icon_home_border" to "#33ffffff",
    "main_background" to "@color/main_background_dark",
    "main_primary_text" to "@color/dark_primary_text",
    "main_secondary_text" to "@color/dark_secondary_text",
    "media_thumb_background" to "#dd000000",
    "media_thumb_border" to "#666666",
    "music_icon_playing" to "@color/dark_primary_text",
    "nav_background" to "@color/dark_color_surface_container_low",
    "nav_label_background" to "@color/dark_color_surface_container",
    "nav_list_separator" to "#33ffffff",
    "nav_primary_text_default" to "@color/dark_primary_text",
    "nav_primary_text_disabled" to "#50ffffff",
    "nav_tab_background" to "@color/dark_color_surface",
    "nav_tab_separator" to "#33ffffff",
    "pathbar_background" to "@color/dark_color_surface_container",
    "pathbar_border" to "#33ffffff",
    "pathbar_button_text" to "@color/dark_secondary_text",
    "pathbar_icon" to "#f0ffffff",
    "pathbar_separator" to "#bbffffff",
    "pathbar_text_selected" to "@color/dark_primary_text",
    "plain_text_background" to "#88000000",
    "selection_background" to "@color/dark_color_surface_container_highest",
    "statusbar_color" to "@color/dark_actionbar_color",
    "statusbar_color_before_23" to "@color/dark_actionbar_color",
    "view_setting_icon" to "@color/dark_primary_text",
)

internal val darkThemeResourcesPatch = resourcePatch {
    execute {
        val palette = document("res/values/colors.xml").use { document ->
            val names = document.getNode("resources")
                .childElementsSequence()
                .map { it.getAttribute("name") }
                .toSet()

            names.filter {
                it.startsWith(DARK_COLOR_PREFIX) && it.removePrefix(DARK_COLOR_PREFIX) in names
            }.associate { it.removePrefix(DARK_COLOR_PREFIX) to "@color/$it" }
        }

        if (palette.isEmpty()) {
            throw PatchException("Could not find the app's dark colour palette")
        }

        document("res/values-night/colors.xml").use { document ->
            val resources = document.getNode("resources")
            val declared = resources.childElementsSequence()
                .map { it.getAttribute("name") }
                .toSet()

            (palette + CHROME_COLORS).forEach { (name, value) ->
                if (name in declared) return@forEach

                resources.adoptChild("color") {
                    setAttribute("name", name)
                    textContent = value
                }
            }
        }

        document("res/values-night/styles.xml").use { document ->
            val resources = document.getNode("resources")
            val declared = resources.childElementsSequence()
                .map { it.getAttribute("name") }
                .toSet()

            NIGHT_STYLES.forEach { (name, parent) ->
                if (name in declared) return@forEach

                resources.adoptChild("style") {
                    setAttribute("name", name)
                    setAttribute("parent", parent)

                    NIGHT_STYLE_ITEMS[name]?.forEach { (item, value) ->
                        adoptChild("item") {
                            setAttribute("name", item)
                            textContent = value
                        }
                    }
                }
            }
        }

        document("res/xml/settings.xml").use { document ->
            val themePreference = document.getElementsByTagName("ListPreference")
                .findElementByAttributeValueOrThrow("android:key", THEME_PREFERENCE_KEY)

            val appearanceCategory = document.createElement("PreferenceCategory").apply {
                setAttribute("android:title", APPEARANCE_CATEGORY_TITLE)
                setAttribute("app:iconSpaceReserved", "false")
                appendChild(themePreference)
            }

            document.getNode("PreferenceScreen").insertFirst(appearanceCategory)
        }
    }
}
