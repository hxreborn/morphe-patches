/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.util.addFreeRegisters
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.p0Register
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val PATCHES_MENU = "Lapp/hxreborn/extension/viber/PatchesMenu;"

private const val PLUS_AD_ROW = "Lapp/hxreborn/extension/viber/PlusAdRow;"

private const val SETTINGS_STYLE = "Lapp/hxreborn/extension/viber/SettingsStyle;"

private const val ACTIVITY = "app.hxreborn.extension.viber.PatchesSettingsActivity"

internal val patchesSettingsPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")
    dependsOn(resourcePatch {
        execute {
            document("res/values/ids.xml").use { document ->
                val id = document.createElement("item")
                id.setAttribute("type", "id")
                id.setAttribute("name", "hx_viber_patches")
                document.documentElement.appendChild(id)
            }
            document("res/values/strings.xml").use { document ->
                val title = document.createElement("string")
                title.setAttribute("name", "hx_viber_patches")
                title.textContent = "hxreborn patches"
                document.documentElement.appendChild(title)
            }
            val moreIcon = get("res/drawable/more_settings_icon.xml").readText()
                .replace("@drawable/ic_settings_white", "@drawable/hx_viber_ic_patches")
            get("res/drawable/hx_viber_more_patches.xml").writeText(moreIcon)
            document("res/xml/settings_general.xml").use { document ->
                val row = document.createElement("Preference")
                row.setAttribute("android:layout", "@layout/_ics_custom_preference_layout")
                row.setAttribute("android:title", "hxreborn patches")
                row.setAttribute("android:key", "hx_viber_patches")
                val intent = document.createElement("intent")
                intent.setAttribute("android:targetPackage", "com.viber.voip")
                intent.setAttribute("android:targetClass", ACTIVITY)
                row.appendChild(intent)
                val screen = document.documentElement
                screen.insertBefore(row, screen.firstChild)
            }
            document("AndroidManifest.xml").use { document ->
                val application = document.getElementsByTagName("application").item(0) as Element
                val activity = document.createElement("activity")
                activity.setAttribute("android:name", ACTIVITY)
                activity.setAttribute("android:exported", "false")
                activity.setAttribute("android:theme", "@style/Theme.Viber.NoActionBar")
                application.appendChild(activity)
            }
            mapOf(
                "remove_chat_payments" to "money_off",
                "remove_list_gaps" to "vertical_align_center",
                "disable_ai_features" to "auto_awesome",
                "hide_ads" to "ad_off", "hide_more_items" to "more_items",
                "hide_navigation_buttons" to "home_tab", "disable_analytics" to "tracking",
                "unlock_viber_plus" to "premium",
                "unfiled_chats" to "folder_off",
                "filter_business_chats" to "business_messages",
                "select_business_chats" to "checklist",
                "remove_online_status_limit" to "timer_off",
                "hide_promotions" to "promotions",
                "disable_update_prompts" to "update_disabled",
                "link_as_secondary_device" to "devices",
                "raise_folder_limit" to "create_new_folder",
                "start_folder" to "drive_file_move",
                "save_disappearing_media" to "download",
                "reset" to "reset", "info" to "info", "patches" to "patches", "back" to "arrow_back",
                "expand" to "expand_more",
            ).forEach { (target, source) ->
                val stream = SettingsResources::class.java.getResourceAsStream("/viber/drawable/hx_viber_ic_$source.xml")
                    ?: SettingsResources::class.java.getResourceAsStream("/patreon/drawable/hx_patreon_ic_$source.xml")
                    ?: throw PatchException("No hx_viber_ic_$source or hx_patreon_ic_$source drawable in the bundle")
                stream.use { get("res/drawable/hx_viber_ic_$target.xml").outputStream().use(it::copyTo) }
            }
        }
    })
    execute {
        with(mutableClassDefBy(PLUS_AD_ROW)) {
            replaceResourceIdStubs(
                ResourceType.ID,
                mapOf("adsFreeRowId" to "ads_free_btn", "adsFreeTitleViewId" to "ads_free_title"),
            )
            replaceResourceIdStubs(
                ResourceType.STRING,
                mapOf("adsFreeTitleText" to "viber_plus_settings_ads_free_title"),
            )
        }
        val darknightTheme = DarknightThemeFingerprint.matchSingle().originalMethod
        with(mutableClassDefBy(SETTINGS_STYLE)) {
            replaceStub(
                "isDarknightTheme",
                1,
                """
                    invoke-static { }, ${descriptor(darknightTheme)}
                    move-result v0
                    return v0
                """,
            )
            replaceResourceIdStubs(
                ResourceType.STYLE,
                mapOf("lightTheme" to "Theme.Viber.NoActionBar", "darknightTheme" to "Darknight.Theme.Viber.NoActionBar"),
            )
            replaceResourceIdStubs(
                ResourceType.ATTR,
                mapOf(
                    "mainBackgroundAttribute" to "mainBackgroundColor",
                    "secondaryBackgroundAttribute" to "secondaryBackgroundColor",
                    "textPrimaryAttribute" to "textPrimaryColor",
                    "textSecondaryAttribute" to "textSecondaryColor",
                ),
            )
        }
        PlusAdSwitchFingerprint.matchSingle().let { match ->
            val (_, _, setChecked) = match.instructionMatches
            val toggle = match.method.getInstruction<FiveRegisterInstruction>(setChecked.index).registerC
            match.method.addInstruction(
                setChecked.index + 1,
                "invoke-static/range { v$toggle .. v$toggle }, $PLUS_AD_ROW->bind(Landroid/widget/CompoundButton;)V",
            )
        }
        addMoreEntry()
        ApplicationContextFingerprint.matchSingle().method.addInstruction(
            0,
            "invoke-static/range { p1 .. p1 }, $VIBER_SETTINGS_CLASS->initialize(Landroid/content/Context;)V",
        )
    }
}

private fun BytecodePatchContext.addMoreEntry() {
    val settingsItem = SettingsItemFingerprint.matchSingle()
    val (builderCall, titleCall, iconCall, itemCall, _) = settingsItem.instructionMatches
    val builder = builderCall.instruction.getReference<MethodReference>()!!
    val title = titleCall.instruction.getReference<MethodReference>()!!
    val icon = iconCall.instruction.getReference<MethodReference>()!!
    val item = itemCall.instruction.getReference<MethodReference>()!!
    val menu = mutableClassDefBy(PATCHES_MENU)
    menu.interfaces.add("Lcom/viber/voip/user/more/listitems/creators/PreferenceItemCreator;")
    menu.replaceStub("buildItem", 6, """
        new-instance v0, ${builder.definingClass}
        const/4 v1, 0x0
        invoke-direct {v0, p0, p1, v1}, ${descriptor(builder)}
        invoke-virtual {v0, p2}, ${descriptor(title)}
        invoke-virtual {v0, p3}, ${descriptor(icon)}
        new-instance v1, ${item.definingClass}
        invoke-direct {v1, v0}, ${descriptor(item)}
        return-object v1
    """)
    menu.addMethod("create", emptyList(), item.definingClass, AccessFlags.PUBLIC.value, 1, """
        invoke-virtual {p0}, $PATCHES_MENU->createItem()Ljava/lang/Object;
        move-result-object p0
        check-cast p0, ${item.definingClass}
        return-object p0
    """)
    val creators = MoreCreatorsFingerprint.matchSingle()
    val (_, addCall, nextRead) = creators.instructionMatches
    val add = addCall.instruction as FiveRegisterInstruction
    val creatorRegister = add.registerD
    check((nextRead.instruction as TwoRegisterInstruction).registerA == creatorRegister) {
        "Next More field read must overwrite Settings creator register"
    }
    creators.method.addInstructions(addCall.index + 1, """
        new-instance v$creatorRegister, $PATCHES_MENU
        invoke-direct {v$creatorRegister, p1}, $PATCHES_MENU-><init>(Landroid/content/Context;)V
        invoke-virtual {v${add.registerC}, v$creatorRegister}, ${descriptor(addCall.instruction.getReference<MethodReference>()!!)}
    """)
    val click = MoreClickFingerprint.matchSingle()
    val (_, activityResult) = click.instructionMatches
    val activity = (activityResult.instruction as OneRegisterInstruction).registerA
    val id = click.method.p0Register + 1
    val handled = click.method.implementation!!.registerCount
    val (expanded, parameterMoves) = click.method.addFreeRegisters(1)
    val index = activityResult.index + parameterMoves + 1
    expanded.addInstructionsWithLabels(index, """
        invoke-static {v$activity, v$id}, $PATCHES_MENU->openSettings(Landroid/app/Activity;I)Z
        move-result v$handled
        if-eqz v$handled, :native_click
        return-void
    """, ExternalLabel("native_click", expanded.getInstruction(index)))
}

private object SettingsResources
