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
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.addFreeRegisters
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.p0Register
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val PATCHES_MENU = "Lapp/hxreborn/extension/viber/PatchesMenu;"

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
            get("res/drawable/hx_viber_ic_expand.xml").writeText("""
                <vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
                    <path android:fillColor="@android:color/transparent" android:strokeColor="#FFFFFFFF" android:strokeWidth="2" android:pathData="M6,9 L12,15 L18,9" />
                </vector>
            """.trimIndent())
            mapOf(
                "remove_chat_payments" to "payments",
                "disable_ai_features" to "ai",
                "hide_ads" to "promotions", "hide_more_items" to "more_items",
                "hide_navigation_buttons" to "home_tab", "disable_analytics" to "tracking",
                "unlock_viber_plus" to "premium",
                "unfiled_chats" to "home_tab",
                "filter_business_chats" to "more_items",
                "reset" to "reset", "info" to "info", "patches" to "patches", "back" to "back",
            ).forEach { (target, source) ->
                val stream = SettingsResources::class.java.getResourceAsStream("/viber/drawable/hx_viber_ic_$source.xml")
                    ?: SettingsResources::class.java.getResourceAsStream("/patreon/drawable/hx_patreon_ic_$source.xml")
                    ?: SettingsResources::class.java.getResourceAsStream("/aliexpress/drawable/hx_aliexpress_ic_$source.xml")!!
                stream.use { get("res/drawable/hx_viber_ic_$target.xml").outputStream().use(it::copyTo) }
            }
        }
    })
    execute {
        PlusAdSwitchFingerprint.matchSingle().let { match ->
            val (_, _, setChecked) = match.instructionMatches
            val toggle = match.method.getInstruction<FiveRegisterInstruction>(setChecked.index).registerC
            match.method.addInstruction(
                setChecked.index + 1,
                "invoke-static/range { v$toggle .. v$toggle }, Lapp/hxreborn/extension/viber/PlusAdRow;->bind(Landroid/widget/CompoundButton;)V",
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
