/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bittorrent.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.matchAllMethodIndicesForEach
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.patches.shared.misc.torrent.removeProtectionPatch
import app.morphe.util.getNode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import org.w3c.dom.Document
import org.w3c.dom.Element
import app.morphe.patches.shared.misc.extension.bittorrentSettingsMenuAndAmoledThemeExtensionPatch
import app.morphe.patches.shared.misc.extension.bittorrentSettingsExtensionPatch

private const val EXTENSION_PACKAGE = "Lapp/hxreborn/extension/bittorrent/"
private const val THEME_CLASS = "${EXTENSION_PACKAGE}AmoledTheme;"
private const val SETTINGS_CLASS = "${EXTENSION_PACKAGE}PatchSettings;"
private const val MENU_CLASS = "${EXTENSION_PACKAGE}PatchesMenu;"
private const val SWITCH_CLASS = "${EXTENSION_PACKAGE}HostSwitch;"
private const val SETTINGS_ACTIVITY = "app.hxreborn.extension.bittorrent.PatchesSettingsActivity"
private const val HOST_SWITCH_CLASS = "Lcom/bittorrent/base/view/CustomSwitch;"
private const val SWITCH_LAYOUT_NAME = "hx_bittorrent_switch"
private const val ENTRY_TAG = "hx_bittorrent_patches_entry"
private const val ANCHOR_ROW_ID = "tv_incoming_port_title"
private const val SECTION_HEADER_ID = "tv_others"
private const val FOLDER_TITLE_ID = "tv_default_folder"
private const val FOLDER_VALUE_ID = "tv_download_folder"
private const val ENTRY_TITLE_ID = "hx_bittorrent_patches_title"
private const val ENTRY_CHEVRON_ID = "hx_bittorrent_patches_chevron"
private const val ENTRY_CHEVRON_TAG = "hx_bittorrent_patches_entry_chevron"
private const val ENTRY_TITLE = "hxreborn patches"
private const val SWITCH_LAYOUT = """<?xml version="1.0" encoding="utf-8"?>
<com.bittorrent.base.view.CustomSwitch xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="52.0dip"
    android:layout_height="32.0dip"
    app:closeColor="@color/color_7d7d80"
    app:closeText=""
    app:openColor="@color/color_resume_all"
    app:openText="" />
"""

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
private var amoledDrawableIds = emptyMap<String, Long>()
private var switchLayoutId = 0L

private fun Element.referencesId(attribute: String, id: String) = getAttribute(attribute) == "@id/$id"

private fun Element.elementsByTag(name: String): List<Element> {
    val nodes = getElementsByTagName(name)
    return (0 until nodes.length).map { nodes.item(it) as Element }
}

private fun nextResourceId(declarations: Element, type: String) =
    declarations.elementsByTag("public").filter { it.getAttribute("type") == type }
        .maxOf { it.getAttribute("id").removePrefix("0x").toLong(16) } + 1

private fun Document.declare(type: String, name: String, identifier: Long) {
    val declaration = createElement("public")
    declaration.setAttribute("type", type)
    declaration.setAttribute("name", name)
    declaration.setAttribute("id", "0x${identifier.toString(16)}")
    documentElement.appendChild(declaration)
}

private const val SPLASH_STYLE = "Theme.App.Splash"
private const val NIGHT_STYLES_FILE = "res/values-night/styles.xml"
private const val WHITE = "@android:color/white"
private const val BLACK = "@android:color/black"

private fun ResourcePatchContext.addNightSplash(drawables: List<String>, layouts: List<String>) {
    val (parent, items) = document("res/values-v31/styles.xml").use { styles ->
        val style = styles.documentElement.elementsByTag("style").single { it.getAttribute("name") == SPLASH_STYLE }
        style.getAttribute("parent") to style.elementsByTag("item")
            .map { it.getAttribute("name") to it.textContent.replace(WHITE, BLACK) }
    }
    document(NIGHT_STYLES_FILE).use { night ->
        val style = night.createElement("style")
        style.setAttribute("name", SPLASH_STYLE)
        style.setAttribute("parent", parent)
        items.forEach { (name, value) ->
            val item = night.createElement("item")
            item.setAttribute("name", name)
            item.textContent = value
            style.appendChild(item)
        }
        night.documentElement.appendChild(style)
    }
    drawables.forEach { copyWithBlackBackground("res/drawable/$it.xml", "res/drawable-night/$it.xml") }
    layouts.forEach { copyWithBlackBackground("res/layout/$it.xml", "res/layout-night/$it.xml") }
}

private fun ResourcePatchContext.copyWithBlackBackground(source: String, target: String) {
    val copy = get(target)
    copy.parentFile.mkdirs()
    get(source).copyTo(copy, overwrite = true)
    document(target).use { resource ->
        val root = resource.documentElement
        (listOf(root) + root.elementsByTag("*")).forEach { element ->
            val attributes = element.attributes
            for (index in 0 until attributes.length) {
                val attribute = attributes.item(index)
                if (attribute.nodeValue == WHITE) attribute.nodeValue = BLACK
            }
        }
    }
}

private val amoledResourcesPatch = resourcePatch {
    dependsOn(bittorrentSettingsExtensionPatch)
    execute {
        addNightSplash(listOf("splash_icon", "splash_icon_fix"), listOf("activity_splash"))
        document("res/values/public.xml").use { declarations ->
            var nextDrawable = nextResourceId(declarations.documentElement, "drawable")
            amoledDrawableIds = DARK_DRAWABLES.keys.associateWith { original ->
                val name = "hx_bittorrent_amoled_$original"
                val identifier = nextDrawable++
                declarations.declare("drawable", name, identifier)
                get("res/drawable/$original.xml").copyTo(get("res/drawable/$name.xml"), overwrite = true)
                document("res/drawable/$name.xml").use { drawable ->
                    val fills = drawable.getElementsByTagName("solid")
                    val gradients = drawable.getElementsByTagName("gradient")
                    if (fills.length + gradients.length != 1) {
                        throw PatchException("Expected one background fill in $original")
                    }
                    if (fills.length == 1) {
                        (fills.item(0) as Element).setAttribute("android:color", "#ff000000")
                    } else {
                        val solid = drawable.createElement("solid")
                        solid.setAttribute("android:color", "#ff000000")
                        gradients.item(0).let { it.parentNode.replaceChild(solid, it) }
                    }
                }
                identifier
            }
            switchLayoutId = nextResourceId(declarations.documentElement, "layout")
            declarations.declare("layout", SWITCH_LAYOUT_NAME, switchLayoutId)
        }

        decode("res/layout/activity_setting.xml")
        get("res/layout/$SWITCH_LAYOUT_NAME.xml").writeText(SWITCH_LAYOUT)
        document("res/layout/activity_setting.xml").use { layout ->
            val views = layout.documentElement.elementsByTag("*")
            val title = views.single { it.getAttribute("android:id").endsWith("/$ANCHOR_ROW_ID") }
            val chevron = views.single { it.referencesId("app:layout_constraintTop_toTopOf", ANCHOR_ROW_ID) && it.tagName == "ImageView" }
            val nextRow = views.single { it.referencesId("app:layout_constraintTop_toBottomOf", SECTION_HEADER_ID) }
            val folderValue = views.single { it.getAttribute("android:id").endsWith("/$FOLDER_VALUE_ID") }
            folderValue.removeAttribute("app:layout_constraintTop_toBottomOf")
            folderValue.setAttribute("app:layout_constraintTop_toTopOf", "@id/$FOLDER_TITLE_ID")
            folderValue.setAttribute("app:layout_constraintBottom_toBottomOf", "@id/$FOLDER_TITLE_ID")
            val entryTitle = title.cloneNode(true) as Element
            entryTitle.setAttribute("android:id", "@+id/$ENTRY_TITLE_ID")
            entryTitle.setAttribute("android:text", ENTRY_TITLE)
            entryTitle.setAttribute("android:tag", ENTRY_TAG)
            entryTitle.setAttribute("android:clickable", "true")
            entryTitle.setAttribute("app:layout_constraintTop_toBottomOf", "@id/$SECTION_HEADER_ID")
            val entryChevron = chevron.cloneNode(true) as Element
            entryChevron.setAttribute("android:id", "@+id/$ENTRY_CHEVRON_ID")
            entryChevron.setAttribute("android:tag", ENTRY_CHEVRON_TAG)
            entryChevron.setAttribute("android:clickable", "true")
            entryChevron.setAttribute("app:layout_constraintTop_toTopOf", "@id/$ENTRY_TITLE_ID")
            entryChevron.setAttribute("app:layout_constraintBottom_toBottomOf", "@id/$ENTRY_TITLE_ID")
            nextRow.setAttribute("app:layout_constraintTop_toBottomOf", "@id/$ENTRY_TITLE_ID")
            nextRow.parentNode.insertBefore(entryTitle, nextRow)
            nextRow.parentNode.insertBefore(entryChevron, nextRow)
        }
        document("AndroidManifest.xml").use { document ->
            val activity = document.createElement("activity")
            activity.setAttribute("android:name", SETTINGS_ACTIVITY)
            activity.setAttribute("android:exported", "false")
            activity.setAttribute("android:theme", "@android:style/Theme.DeviceDefault.NoActionBar")
            document.getNode("application").appendChild(activity)
        }
    }
}

@Suppress("unused")
val amoledThemePatch = bytecodePatch(
    name = "AMOLED dark theme",
    description = "Adds a setting that makes dialogs, snackbars, buttons and cards pure black in dark mode.",
) {
    dependsOn(bittorrentSettingsMenuAndAmoledThemeExtensionPatch)
    compatibleWith(AppCompatibilities.BITTORRENT)
    availability(requireArm64)
    dependsOn(removeProtectionPatch, amoledResourcesPatch)

    execute {
        val darkMode = DarkModeFingerprint.matchSingle().method
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
            .associate { resourceId(ResourceType.DRAWABLE, it) to amoledDrawableIds.getValue(it) }
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

        SettingsResumeFingerprint.matchSingle().method.addInstructions(
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
            interfaces.add(listenerType)
            addMethod("invoke", listOf("Ljava/lang/Object;"), "Ljava/lang/Object;", AccessFlags.PUBLIC.value, 3,
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
