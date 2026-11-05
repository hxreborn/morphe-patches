/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger
import org.w3c.dom.Element

internal const val PATREON_EXTENSION_PACKAGE = "Lapp/hxreborn/extension/patreon/"
private const val PATCHES_MENU_CLASS = "${PATREON_EXTENSION_PACKAGE}PatchesMenu;"
private const val PATCHES_COMPONENT_FACTORY_CLASS = "${PATREON_EXTENSION_PACKAGE}PatchesComponentFactory;"
private const val SETTINGS_ACTIVITY_CLASS = "app.hxreborn.extension.patreon.PatchesSettingsActivity"
private const val SETTINGS_ROW_TITLE = "hxreborn patches"
private const val SETTINGS_ROW_METHOD = "patchesSettingsRow"
private val ICON_RESOURCES = listOf(
    "hx_patreon_ic_patches", "hx_patreon_ic_recommended", "hx_patreon_ic_home_tab", "hx_patreon_ic_feed",
    "hx_patreon_ic_library", "hx_patreon_ic_quip", "hx_patreon_ic_community", "hx_patreon_ic_notifications",
    "hx_patreon_ic_info", "hx_patreon_ic_reset", "hx_patreon_ic_promotions",
    "hx_patreon_ic_prompts", "hx_patreon_ic_tracking", "hx_patreon_ic_quips", "hx_patreon_ic_search",
)
private const val HOST_ACTIVITY = "com.patreon.android.screens.profile.edit.EditProfileActivity"
private const val CORE_COMPONENT_FACTORY_CLASS = "Landroidx/core/app/CoreComponentFactory;"
private const val CORE_COMPONENT_FACTORY_NAME = "androidx.core.app.CoreComponentFactory"
private const val APP_COMPONENT_FACTORY_CLASS = "Landroid/app/AppComponentFactory;"
private const val ROW_TITLE_PARAMETER = 0
private const val ROW_ON_CLICK_PARAMETER = 5
private const val ROW_COMPOSER_PARAMETER = 6

private val logger = Logger.getLogger("app.morphe.patches.patreon.misc.settings")

internal fun BytecodePatchContext.markFeaturePatched(featureClass: String) =
    mutableClassDefBy(featureClass).methods.single { it.name == "isPatched" }.returnEarly(true)

private fun MethodReference.isSettingsNavigationRow() =
    returnType == "V" && parameterTypes.map(CharSequence::toString).let { types ->
        types.size == settingsNavigationRowParameters.size &&
            types.zip(settingsNavigationRowParameters).all { (type, expected) -> type.startsWith(expected) }
    }

private fun BytecodePatchContext.addPatchesSettingsRow() {
    val section = AccountSettingsSectionFingerprint.method
    val rowIndex = section.indexOfFirstInstructionReversedOrThrow {
        opcode == Opcode.INVOKE_STATIC && getReference<MethodReference>()?.isSettingsNavigationRow() == true
    }
    val rowCall = section.getInstruction<FiveRegisterInstruction>(rowIndex)
    val navigationRow = rowCall.getReference<MethodReference>()!!
    val composer = rowCall.registerE

    val rowClass = mutableClassDefBy(navigationRow.definingClass)
    val navigationRowMethod = rowClass.methods.single { descriptor(it) == descriptor(navigationRow) }
    val templateIndex = navigationRowMethod.indexOfFirstInstructionOrThrow(Opcode.INVOKE_STATIC_RANGE)
    val template = navigationRowMethod.getInstruction<RegisterRangeInstruction>(templateIndex)
    val templateRow = template.getReference<MethodReference>()!!
    val parameterTypes = templateRow.parameterTypes.map(CharSequence::toString)
    if (parameterTypes[ROW_TITLE_PARAMETER] != "Ljava/lang/String;" ||
        parameterTypes[ROW_ON_CLICK_PARAMETER] != "Lkotlin/jvm/functions/Function0;" ||
        parameterTypes.any { it == "J" || it == "D" }
    ) {
        throw PatchException("Unexpected settings row signature ${descriptor(templateRow)}")
    }

    val maskParameter = parameterTypes.lastIndex
    val maskRegister = template.startRegister + maskParameter
    val maskIndex = navigationRowMethod.instructions.withIndex().last { (index, instruction) ->
        index < templateIndex && instruction is OneRegisterInstruction && instruction.registerA == maskRegister
    }.index
    val mask = (navigationRowMethod.getInstruction(maskIndex) as? WideLiteralInstruction)?.wideLiteral
        ?: throw PatchException("No default-arguments mask before ${descriptor(templateRow)}")

    val onClickType = parameterTypes[ROW_ON_CLICK_PARAMETER]
    val assigned = setOf(ROW_TITLE_PARAMETER, ROW_ON_CLICK_PARAMETER, ROW_COMPOSER_PARAMETER, maskParameter)
    val settingsRow = rowClass.addMethod(
        SETTINGS_ROW_METHOD,
        listOf(parameterTypes[ROW_COMPOSER_PARAMETER]),
        "V",
        AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
        parameterTypes.size + 1,
        """
            ${(parameterTypes.indices - assigned).joinToString("\n") { "const/4 v$it, 0x0" }}
            const-string v$ROW_TITLE_PARAMETER, "$SETTINGS_ROW_TITLE"
            const-class v$ROW_ON_CLICK_PARAMETER, $onClickType
            invoke-static/range { v$ROW_ON_CLICK_PARAMETER .. v$ROW_ON_CLICK_PARAMETER }, $PATCHES_MENU_CLASS->settingsRowOnClick(Ljava/lang/Class;)Ljava/lang/Object;
            move-result-object v$ROW_ON_CLICK_PARAMETER
            check-cast v$ROW_ON_CLICK_PARAMETER, $onClickType
            move-object/from16 v$ROW_COMPOSER_PARAMETER, p0
            const v$maskParameter, $mask
            invoke-static/range { v0 .. v$maskParameter }, ${descriptor(templateRow)}
            return-void
        """,
    )

    section.addInstruction(rowIndex + 1, "invoke-static/range { v$composer .. v$composer }, ${descriptor(settingsRow)}")
}

internal val patchesSettingsPatch = bytecodePatch {
    dependsOn(
        resourcePatch {
            execute {
                ICON_RESOURCES.forEach { name ->
                    val icon = PatreonResources::class.java.getResourceAsStream("/patreon/drawable/$name.xml")
                        ?: throw PatchException("Missing icon resource $name")
                    icon.use { get("res/drawable/$name.xml").outputStream().use(it::copyTo) }
                }
            }
            finalize {
                document("AndroidManifest.xml").use { document ->
                    val activities = document.getElementsByTagName("activity")
                    val hostDeclared = (0 until activities.length).any {
                        (activities.item(it) as Element).getAttribute("android:name") == HOST_ACTIVITY
                    }
                    if (!hostDeclared) throw PatchException("Missing host activity $HOST_ACTIVITY")

                    val application = document.getElementsByTagName("application").item(0) as Element
                    val componentFactory = application.getAttribute("android:appComponentFactory")
                    if (componentFactory != CORE_COMPONENT_FACTORY_NAME) {
                        throw PatchException("Unexpected appComponentFactory $componentFactory")
                    }

                    val activity = document.createElement("activity")
                    activity.setAttribute("android:name", SETTINGS_ACTIVITY_CLASS)
                    activity.setAttribute("android:exported", "false")
                    activity.setAttribute("android:theme", "@android:style/Theme.DeviceDefault.NoActionBar")
                    application.appendChild(activity)
                }
            }
        },
    )
    extendWith("extensions/extension.mpe")

    execute {
        mutableClassDefBy(PATCHES_MENU_CLASS).methods.single { it.name == "hostActivity" }.returnEarly(HOST_ACTIVITY)

        mutableClassDefBy(CORE_COMPONENT_FACTORY_CLASS).apply {
            if (superclass != APP_COMPONENT_FACTORY_CLASS) {
                throw PatchException("$CORE_COMPONENT_FACTORY_CLASS extends $superclass")
            }
            setSuperClass(PATCHES_COMPONENT_FACTORY_CLASS)
            methods.single { it.name == "<init>" && it.parameterTypes.isEmpty() }.apply {
                val index = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_DIRECT &&
                        getReference<MethodReference>()?.definingClass == APP_COMPONENT_FACTORY_CLASS
                }
                replaceInstruction(index, "invoke-direct { p0 }, $PATCHES_COMPONENT_FACTORY_CLASS-><init>()V")
            }
        }

        try {
            addPatchesSettingsRow()
        } catch (ex: PatchException) {
            logger.warning("Skipped the patches settings row: ${ex.message}")
        }
    }
}

private object PatreonResources
