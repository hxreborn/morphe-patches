/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.getReference
import app.morphe.patches.bereal.misc.base.beRealBasePatch
import app.morphe.patches.shared.misc.fillPatchDetails
import app.morphe.patches.shared.misc.returnTrueFrom
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element
import app.morphe.patches.shared.misc.extension.berealContactsPermissionExtensionPatch
import app.morphe.patches.shared.misc.extension.berealDataCollectionExtensionPatch
import app.morphe.patches.shared.misc.extension.berealSettingsExtensionPatch
import app.morphe.patches.shared.misc.extension.berealSponsoredPostsExtensionPatch
import app.morphe.patches.shared.misc.extension.berealUpdatePromptsExtensionPatch
import app.morphe.patches.shared.misc.extension.morpheLibraryCoreExtensionPatch
import app.morphe.patches.shared.misc.extension.sharedActivityCallbacksAdapterExtensionPatch
import app.morphe.patches.shared.misc.extension.sharedDimensionsExtensionPatch

internal const val BEREAL_EXTENSION_PACKAGE = "Lapp/hxreborn/extension/bereal/"
private const val PATCH_CONTEXT_CLASS = "${BEREAL_EXTENSION_PACKAGE}PatchContext;"
private const val SETTINGS_ACTIVITY_CLASS = "app.hxreborn.extension.bereal.PatchesSettingsActivity"
private const val GALLERY_PICKER_ACTIVITY_CLASS = "app.hxreborn.extension.bereal.GalleryPickerActivity"
private const val RETAKE_COUNT_ACTIVITY_CLASS = "app.hxreborn.extension.bereal.RetakeCountActivity"
private const val PAIRIP_APPLICATION_CLASS = "Lcom/pairip/application/Application;"
private const val SETTINGS_ROW_TITLE = "hxreborn patches"
private const val SETTINGS_ROW_TAG = "settingsscreen_patches_button"
private const val SETTINGS_ROW_ICON = "ic_gearshape_fill"

// Mount installs keep the stock manifest, so the patch screens open through stock activities with the same
// manifest flags. Keep these in sync with HostedScreens in the extension.
private const val OPAQUE_HOST_ACTIVITY = "com.pairip.licensecheck.LicenseActivity"
private const val TRANSLUCENT_HOST_ACTIVITY = "com.google.android.gms.auth.api.signin.internal.SignInHubActivity"
private const val CORE_COMPONENT_FACTORY_NAME = "androidx.core.app.CoreComponentFactory"
private const val CORE_COMPONENT_FACTORY_CLASS = "Landroidx/core/app/CoreComponentFactory;"
private const val APP_COMPONENT_FACTORY_CLASS = "Landroid/app/AppComponentFactory;"
private const val PATCHES_COMPONENT_FACTORY_CLASS = "${BEREAL_EXTENSION_PACKAGE}PatchesComponentFactory;"

private fun Element.androidAttribute(name: String) = getAttribute("android:$name")

private fun checkHostActivity(activities: List<Element>, name: String, translucent: Boolean) {
    val host = activities.singleOrNull { it.androidAttribute("name") == name }
        ?: throw PatchException("Missing host activity $name")
    val excluded = host.androidAttribute("excludeFromRecents") == "true"
    val ownProcess = host.androidAttribute("process").isNotEmpty()
    val customLaunch = host.androidAttribute("launchMode").let { it.isNotEmpty() && it != "standard" && it != "0" }
    if (host.androidAttribute("exported") == "true" || ownProcess || customLaunch || excluded != translucent) {
        throw PatchException("Host activity $name changed its manifest flags")
    }
}

private fun hostPatchScreens() {
    mutableClassDefBy(CORE_COMPONENT_FACTORY_CLASS).apply {
        if (superclass != APP_COMPONENT_FACTORY_CLASS) {
            throw PatchException("$CORE_COMPONENT_FACTORY_CLASS extends $superclass")
        }
        setSuperClass(PATCHES_COMPONENT_FACTORY_CLASS)

        methods.single { it.name == "<init>" && it.parameterTypes.isEmpty() }.apply {
            val index = instructions.indexOfFirst {
                it.opcode == Opcode.INVOKE_DIRECT &&
                    it.getReference<MethodReference>()?.definingClass == APP_COMPONENT_FACTORY_CLASS
            }
            if (index < 0) throw PatchException("$CORE_COMPONENT_FACTORY_CLASS doesn't call its superclass constructor")
            replaceInstruction(index, "invoke-direct { p0 }, $PATCHES_COMPONENT_FACTORY_CLASS-><init>()V")
        }
    }
}

private fun addSettingsRow() {
    val debugScreenAction = DebugScreenActionToStringFingerprint.match().classDef
    val debugScreenField = debugScreenAction.staticFields.single { it.type == debugScreenAction.type }
    val debugScreenReference = "${debugScreenAction.type}->${debugScreenField.name}:${debugScreenAction.type}"

    val click = settingsActionClickFingerprint(debugScreenAction.type).match()
    val rows = AboutSectionRowsFingerprint.match()
    val rowCall = rows.instructionMatch(Opcode.INVOKE_STATIC_RANGE).getInstruction<RegisterRangeInstruction>()
    val rowFactory = rowCall.getReference<MethodReference>()!!

    val emptySlotWrite = rows.instructionMatch(Opcode.CONST_4).getInstruction<OneRegisterInstruction>()
    val arrayMatch = rows.instructionMatch(Opcode.FILLED_NEW_ARRAY)
    val arrayIndex = arrayMatch.index
    val array = arrayMatch.getInstruction<FiveRegisterInstruction>()
    val emptySlot = emptySlotWrite.registerA
    if (array.registerCount != 5 || array.registerG != emptySlot) {
        throw PatchException("The last About section row is not empty")
    }

    val (_, _, _, _, arrayResult) = rows.instructionMatches
    if (arrayResult.getInstruction<OneRegisterInstruction>().registerA != emptySlot) {
        rows.method.addInstruction(arrayResult.index + 1, "const/16 v$emptySlot, 0x0")
    }

    val first = rowCall.startRegister
    rows.method.addInstructions(
        arrayIndex,
        """
            const v$first, ${resourceId(ResourceType.DRAWABLE, SETTINGS_ROW_ICON)}
            const-string v${first + 1}, "$SETTINGS_ROW_TITLE"
            sget-object v${first + 2}, $debugScreenReference
            const/16 v${first + 3}, 0x0
            const/16 v${first + 4}, 0x0
            const-string v${first + 5}, "$SETTINGS_ROW_TAG"
            invoke-static/range { v$first .. v${first + 5} }, $rowFactory
            move-result-object v$emptySlot
        """,
    )

    val (_, _, _, actionBranch) = click.instructionMatches
    click.method.addInstruction(
        actionBranch.index + 1,
        "invoke-static { }, $PATCH_CONTEXT_CLASS->openSettings()V",
    )
}

internal val settingsPatch = patch {
    dependsOn(berealContactsPermissionExtensionPatch, berealDataCollectionExtensionPatch, berealSettingsExtensionPatch, berealSponsoredPostsExtensionPatch, berealUpdatePromptsExtensionPatch, morpheLibraryCoreExtensionPatch, sharedActivityCallbacksAdapterExtensionPatch, sharedDimensionsExtensionPatch)
    dependsOn(
        beRealBasePatch,
        patch {
            finalize {
                document("AndroidManifest.xml").use { document ->
                    val application = document.getElementsByTagName("application").item(0) as Element
                    val componentFactory = application.getAttribute("android:appComponentFactory")
                    if (componentFactory != CORE_COMPONENT_FACTORY_NAME) {
                        throw PatchException("Unexpected appComponentFactory $componentFactory")
                    }
                    val activities = document.getElementsByTagName("activity").let { nodes ->
                        (0 until nodes.length).map { nodes.item(it) as Element }
                    }
                    checkHostActivity(activities, OPAQUE_HOST_ACTIVITY, translucent = false)
                    checkHostActivity(activities, TRANSLUCENT_HOST_ACTIVITY, translucent = true)

                    val activity = document.createElement("activity")
                    activity.setAttribute("android:name", SETTINGS_ACTIVITY_CLASS)
                    activity.setAttribute("android:exported", "false")
                    activity.setAttribute("android:theme", "@android:style/Theme.DeviceDefault.NoActionBar")
                    application.appendChild(activity)

                    val picker = document.createElement("activity")
                    picker.setAttribute("android:name", GALLERY_PICKER_ACTIVITY_CLASS)
                    picker.setAttribute("android:exported", "false")
                    picker.setAttribute("android:excludeFromRecents", "true")
                    picker.setAttribute("android:theme", "@android:style/Theme.Translucent.NoTitleBar")
                    application.appendChild(picker)

                    val retakes = document.createElement("activity")
                    retakes.setAttribute("android:name", RETAKE_COUNT_ACTIVITY_CLASS)
                    retakes.setAttribute("android:exported", "false")
                    retakes.setAttribute("android:excludeFromRecents", "true")
                    retakes.setAttribute("android:theme", "@android:style/Theme.Translucent.NoTitleBar")
                    application.appendChild(retakes)
                }
            }
        },
    )

    execute {
        mutableClassDefBy(classDefBy(PAIRIP_APPLICATION_CLASS).superclass!!).methods
            .single { it.name == "onCreate" && it.parameterTypes.isEmpty() && it.returnType == "V" }
            .addInstruction(0, "invoke-static/range { p0 .. p0 }, $PATCH_CONTEXT_CLASS->attach(Landroid/app/Application;)V")

        mutableClassDefBy("${BEREAL_EXTENSION_PACKAGE}HostFonts;").replaceResourceIdStubs(
            ResourceType.FONT,
            mapOf(
                "interRegular" to "inter_regular",
                "interMedium" to "inter_medium",
                "interSemibold" to "inter_semibold",
            ),
        )
        addSettingsRow()
        hostPatchScreens()
    }
}

internal fun markFeaturePatched(featureClass: String) =
    returnTrueFrom(featureClass, "isPatchIncluded")

internal fun markFeaturePatched(
    featureClass: String,
    patch: Patch,
) {
    markFeaturePatched(featureClass)
    fillPatchDetails(featureClass, patch, "patchName", "patchDescription")
}
