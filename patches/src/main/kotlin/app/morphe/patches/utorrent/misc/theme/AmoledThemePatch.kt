/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.utorrent.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.listDecodableFiles
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.matchAllMethodIndicesForEach
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.patches.shared.misc.torrent.removeProtectionPatch
import app.morphe.util.getNode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import org.w3c.dom.Element
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
private var amoledDrawableIds = emptyMap<String, Long>()
private var switchLayoutId = 0L

private fun Element.elementsByTag(name: String): List<Element> {
    val nodes = getElementsByTagName(name)
    return (0 until nodes.length).map { nodes.item(it) as Element }
}

private const val SPLASH_STYLE = "Theme.App.Splash"
private const val NIGHT_STYLES_FILE = "res/values-night/styles.xml"
private const val WHITE = "@android:color/white"
private const val BLACK = "@android:color/black"

private fun addNightSplash(drawables: List<String>, layouts: List<String>) {
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

private fun copyWithBlackBackground(source: String, target: String) {
    val copy = resourceFile(target)
    copy.parentFile.mkdirs()
    resourceFile(source).copyTo(copy, overwrite = true)
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

private val amoledResourcesPatch = patch {
    dependsOn(utorrentSettingsExtensionPatch)
    execute {
        addNightSplash(listOf("splash_icon", "splash_icon_fix"), listOf())
        amoledDrawableIds = document("res/values/public.xml").use { declarationsDocument ->
            val declarations = declarationsDocument.getElementsByTagName("public")
            var nextId = (0 until declarations.length).map { declarations.item(it) as Element }
                .filter { it.getAttribute("type") == "drawable" }
                .maxOf { it.getAttribute("id").removePrefix("0x").toLong(16) } + 1
            switchLayoutId = (0 until declarations.length).map { declarations.item(it) as Element }
                .filter { it.getAttribute("type") == "layout" }
                .maxOf { it.getAttribute("id").removePrefix("0x").toLong(16) } + 1
            val switchDeclaration = declarationsDocument.createElement("public")
            switchDeclaration.setAttribute("type", "layout")
            switchDeclaration.setAttribute("name", "hx_utorrent_amoled_switch")
            switchDeclaration.setAttribute("id", "0x${switchLayoutId.toString(16)}")
            declarationsDocument.documentElement.appendChild(switchDeclaration)
            resourceFile("res/layout/hx_utorrent_amoled_switch.xml").writeText("""
                <com.bittorrent.app.view.CustomSwitch
                    xmlns:android="http://schemas.android.com/apk/res/android"
                    xmlns:app="http://schemas.android.com/apk/res-auto"
                    android:layout_width="40dp" android:layout_height="21dp"
                    app:openText="" app:closeText="" app:closeColor="#ff777777"
                    app:openColor="@color/primaryColor" />
            """.trimIndent())
            DARK_DRAWABLES.keys.associateWith { original ->
                val name = "hx_utorrent_amoled_$original"
                val identifier = nextId++
                val declaration = declarationsDocument.createElement("public")
                declaration.setAttribute("type", "drawable")
                declaration.setAttribute("name", name)
                declaration.setAttribute("id", "0x${identifier.toString(16)}")
                declarationsDocument.documentElement.appendChild(declaration)
                if (original == "bg_main_top_img_dark") {
                    if (listDecodableFiles("res/drawable*/$original.webp").isEmpty()) {
                        throw PatchException("Missing dark header bitmap")
                    }
                    resourceFile("res/drawable/$name.xml").writeText("""
                        <bitmap xmlns:android="http://schemas.android.com/apk/res/android"
                            android:src="@drawable/$original" android:tint="#ff000000" />
                    """.trimIndent())
                } else {
                    resourceFile("res/drawable/$original.xml").copyTo(resourceFile("res/drawable/$name.xml"), overwrite = true)
                    document("res/drawable/$name.xml").use { drawable ->
                    val fills = drawable.getElementsByTagName("solid")
                    val gradients = drawable.getElementsByTagName("gradient")
                    if (fills.length + gradients.length != 1) {
                        throw PatchException("Expected one background fill in $original")
                    }
                    if (fills.length == 1) {
                        (fills.item(0) as Element).setAttribute("android:color", "#ff000000")
                    } else {
                        val gradient = gradients.item(0)
                        val solid = drawable.createElement("solid")
                        solid.setAttribute("android:color", "#ff000000")
                        gradient.parentNode.replaceChild(solid, gradient)
                    }
                }
                }
                identifier
            }
        }
        document("res/values/colors.xml").use { colors ->
            val names = setOf("color_dialog_content_dark", "color_notorrent_txt_dark", "color_unselect_tab_dark", "color_search_hint_dark")
            val entries = colors.getElementsByTagName("color")
            val matched = (0 until entries.length).map { entries.item(it) as Element }
                .filter { it.getAttribute("name") in names }
            if (matched.size != names.size) throw PatchException("Expected four dark label colors")
            matched.forEach { it.textContent = "#ffb3b3b3" }
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
val amoledThemePatch = patch(
    name = "AMOLED dark theme",
    description = "Adds an option to make dark surfaces pure black.",
) {
    dependsOn(utorrentSettingsMenuExtensionPatch)
    compatibleWith(AppCompatibilities.UTORRENT)
    availability(requireArm64)
    dependsOn(removeProtectionPatch, amoledResourcesPatch)

    execute {
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
            DARK_DRAWABLES.keys.associate { resourceId(ResourceType.DRAWABLE, it) to amoledDrawableIds.getValue(it) }
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
            interfaces.add(listenerType)
            addMethod(callback.name, listOf("Z"), "V", AccessFlags.PUBLIC.value, 2,
                """
                    invoke-virtual { p0, p1 }, $SWITCH_CLASS->change(Z)V
                    return-void
                """)
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
