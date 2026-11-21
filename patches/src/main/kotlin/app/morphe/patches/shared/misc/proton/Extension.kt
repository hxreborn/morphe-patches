/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.misc.fillAppliedPatch
import app.morphe.patches.shared.misc.fillPatchMetadata
import app.morphe.patches.shared.misc.returnTrueFrom
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.patches.shared.misc.extension.morpheLibraryCoreExtensionPatch
import app.morphe.patches.shared.misc.extension.protonPatchApplicationExtensionPatch
import app.morphe.patches.shared.misc.extension.protonSettingsThemeExtensionPatch
import app.morphe.patches.shared.misc.extension.protonSettingsExtensionPatch
import app.morphe.patches.shared.misc.extension.sharedNightModeExtensionPatch
import app.morphe.patches.shared.misc.extension.sharedProxyHandlerExtensionPatch

internal const val PROTON_EXTENSION_PACKAGE = "Lapp/hxreborn/extension/proton/"
internal const val ACCENT_COLOR_CLASS = "${PROTON_EXTENSION_PACKAGE}AccentColor;"
internal const val AMOLED_THEME_CLASS = "${PROTON_EXTENSION_PACKAGE}AmoledTheme;"
internal const val MATERIAL_SWITCHES_CLASS = "${PROTON_EXTENSION_PACKAGE}MaterialSwitches;"
internal const val UPSELLING_VISIBILITY_CLASS = "${PROTON_EXTENSION_PACKAGE}UpsellingVisibility;"
internal const val PATCHES_MENU_CLASS = "${PROTON_EXTENSION_PACKAGE}PatchesMenu;"
internal const val MAIL_SETTINGS_CLASS = "Lapp/hxreborn/extension/protonmail/MailSettings;"
internal const val PASS_SETTINGS_CLASS = "Lapp/hxreborn/extension/protonpass/PassSettings;"
internal const val VPN_SETTINGS_CLASS = "Lapp/hxreborn/extension/protonvpn/VpnSettings;"
internal const val AUTHENTICATOR_SETTINGS_CLASS = "Lapp/hxreborn/extension/protonauthenticator/AuthenticatorSettings;"
private const val APP_SETTINGS_BINDING_CLASS = "${PROTON_EXTENSION_PACKAGE}AppSettingsBinding;"
private const val PATCH_CONTEXT_CLASS = "${PROTON_EXTENSION_PACKAGE}PatchContext;"
private const val APPLIED_PATCHES_CLASS = "${PROTON_EXTENSION_PACKAGE}AppliedPatches;"
private const val PATCHES_SETTINGS_ACTIVITY_CLASS = "${PROTON_EXTENSION_PACKAGE}PatchesSettingsActivity;"
private const val PATCHES_COMPONENT_FACTORY_CLASS = "${PROTON_EXTENSION_PACKAGE}PatchesComponentFactory;"

internal fun attachPatchContext(applicationClass: String) =
    mutableClassDefBy(applicationClass).methods
        .single { it.name == "onCreate" && it.parameterTypes.isEmpty() && it.returnType == "V" }
        .addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $PATCH_CONTEXT_CLASS->attach(Landroid/app/Application;)V",
        )

internal fun markFeaturePatched(featureClass: String) = returnTrueFrom(featureClass, "isPatched")

internal fun forwardStub(stubClass: String, stubName: String, targetClass: String, targetName: String = "add") {
    val stubs = mutableClassDefBy(stubClass)
    val parameterTypes = stubs.methods.single { it.name == stubName }.parameterTypes
    val registers = parameterTypes.indices.joinToString { "p$it" }
    stubs.replaceStub(
        stubName,
        parameterTypes.size,
        """
            invoke-static { $registers }, $targetClass->$targetName(${parameterTypes.joinToString("")})V
            return-void
        """,
    )
}

internal fun showSettingsRows(stubName: String, rowsClass: String) =
    forwardStub(PATCHES_SETTINGS_ACTIVITY_CLASS, stubName, rowsClass)

internal fun markUpgradePromotionsPatched() {
    markFeaturePatched(UPSELLING_VISIBILITY_CLASS)
    returnTrueFrom(APPLIED_PATCHES_CLASS, "hideUpgradePromotions")
    showSettingsRows("addUpgradePromotionRows", "${PROTON_EXTENSION_PACKAGE}UpgradePromotionRows;")
}

internal fun markLoadingIndicatorPatched() {
    markFeaturePatched(LOADING_INDICATOR_CLASS)
    returnTrueFrom(APPLIED_PATCHES_CLASS, "loadingIndicator")
    showSettingsRows("addLoadingIndicatorRows", "${PROTON_EXTENSION_PACKAGE}LoadingIndicatorRows;")
}

internal fun markFloatingNavigationBarPatched() {
    markFeaturePatched(FLOATING_NAVIGATION_BAR_CLASS)
    returnTrueFrom(APPLIED_PATCHES_CLASS, "floatingNavigationBar")
    showSettingsRows("addNavigationRows", "${PROTON_EXTENSION_PACKAGE}NavigationBarRows;")
    forwardStub(PATCHES_COMPONENT_FACTORY_CLASS, "watchActivity", "${PROTON_EXTENSION_PACKAGE}NavigationBarBackdrop;", "watch")
}

internal fun MutableMethod.returnVoidWhenUpsellingHidden() = returnVoidWhen("$UPSELLING_VISIBILITY_CLASS->isHidden()Z")

internal fun MutableMethod.returnVoidWhen(condition: String) {
    val free = getFreeRegisterProvider(0, 1).getFreeRegister()

    addInstructionsWithLabels(
        0,
        """
            invoke-static { }, $condition
            move-result v$free
            if-eqz v$free, :show
            return-void
        """,
        ExternalLabel("show", getInstruction(0)),
    )
}

internal fun markPatchApplied(settingsClass: String, methodName: String, patch: Patch) =
    fillAppliedPatch(settingsClass, methodName, patch, withDescription = false)

internal fun appliedPatchMarkerPatch(settingsClass: String, methodName: String, patch: () -> Patch) = patch {
    dependsOn(morpheLibraryCoreExtensionPatch, protonPatchApplicationExtensionPatch, protonSettingsThemeExtensionPatch, protonSettingsExtensionPatch, sharedNightModeExtensionPatch, sharedProxyHandlerExtensionPatch)
    execute {
        markPatchApplied(settingsClass, methodName, patch())
    }
}

internal fun markPatchMetadata(
    methodName: String,
    patch: Patch,
    withDescription: Boolean = false,
    settingsClass: String = APPLIED_PATCHES_CLASS,
) = fillPatchMetadata(settingsClass, methodName, patch, withDescription)

internal fun bindAppSettings(settingsClass: String) =
    mutableClassDefBy(APP_SETTINGS_BINDING_CLASS).methods.single { it.name == "current" }.addInstructions(
        0,
        """
            new-instance v0, $settingsClass
            invoke-direct { v0 }, $settingsClass-><init>()V
            return-object v0
        """,
    )
