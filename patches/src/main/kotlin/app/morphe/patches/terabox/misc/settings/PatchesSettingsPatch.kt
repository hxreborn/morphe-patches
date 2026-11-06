/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.returnEarly
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import app.morphe.patches.terabox.misc.fix.signature.spoofSignaturePatch
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources
import com.android.tools.smali.util.StringUtils.escapeString

internal const val TERABOX_PATCH_SETTINGS_CLASS = "Lapp/hxreborn/extension/terabox/PatchSettings;"
internal const val PREMIUM_ENABLED = "$TERABOX_PATCH_SETTINGS_CLASS->isPremiumUnlocked()Z"
internal const val PROMOTIONS_HIDDEN = "$TERABOX_PATCH_SETTINGS_CLASS->isPromotionsHidden()Z"
internal const val VIDEO_RECOMMENDATIONS_HIDDEN = "$TERABOX_PATCH_SETTINGS_CLASS->isVideoRecommendationsHidden()Z"
private const val TERABOX_APPLIED_PATCHES_CLASS = "Lapp/hxreborn/extension/terabox/AppliedPatches;"
private const val SETTINGS_STYLE_CLASS = "Lapp/hxreborn/extension/terabox/SettingsStyle;"
private const val TERABOX_PATCHES_MENU_CLASS = "Lapp/hxreborn/extension/terabox/PatchesMenu;"
private val ICON_RESOURCES = listOf("ads", "promotions", "shortcuts", "video", "premium", "recent", "reset", "info", "patches", "expand")
    .map { "hx_terabox_ic_$it" }

internal fun BytecodePatchContext.markPatchApplied(methodName: String, patch: BytecodePatch) {
    val methods = mutableClassDefBy(TERABOX_APPLIED_PATCHES_CLASS).methods
    methods.single { it.name == methodName }.returnEarly(true)
    methods.single { it.name == methodName + "Name" }.returnEarly(escapeString(checkNotNull(patch.name)))
    methods.single { it.name == methodName + "Description" }.returnEarly(escapeString(checkNotNull(patch.description)))
}

internal fun MutableMethod.overrideWhenEnabled(settingCall: String, override: MutableMethod.() -> Unit) {
    val register = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
    val original = getInstruction(0)
    override()
    addInstructionsWithLabels(
        0,
        """
            invoke-static {}, $settingCall
            move-result v$register
            if-eqz v$register, :original
        """,
        ExternalLabel("original", original),
    )
}

internal val patchesSettingsPatch = bytecodePatch {
    dependsOn(
        spoofSignaturePatch,
        resourcePatch {
            execute {
                copyResources("terabox", ResourceGroup("drawable", *ICON_RESOURCES.map { "$it.xml" }.toTypedArray()))
            }
        },
    )
    extendWith("extensions/extension.mpe")

    execute {
        mutableClassDefBy(TERABOX_PATCHES_MENU_CLASS).replaceResourceIdStubs(
            ResourceType.ID,
            mapOf("templateRowId" to "setting_security"),
        )
        mutableClassDefBy(SETTINGS_STYLE_CLASS).apply {
            replaceResourceIdStubs(
                ResourceType.COLOR,
                mapOf(
                    "pageBackgroundColor" to "bg_dn_home_page",
                    "cardBackgroundColor" to "background_card",
                    "dividerColor" to "color_GC04_alpha40",
                    "primaryTextColor" to "color_GC01",
                    "secondaryTextColor" to "color_GC02",
                ),
            )
            replaceResourceIdStubs(
                ResourceType.DRAWABLE,
                mapOf("switchButtonDrawable" to "chb_on_off_selector", "backIconDrawable" to "ic_arrow_back_black_24"),
            )
        }
        ApplicationCreateFingerprint.matchSingle().method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, Lapp/morphe/extension/shared/Utils;->setContext(Landroid/content/Context;)V",
        )
        SettingsViewFingerprint.matchSingle().method.apply {
            addInstruction(
                implementation!!.instructions.lastIndex,
                "invoke-static/range { p0 .. p0 }, $TERABOX_PATCHES_MENU_CLASS->attach(Landroid/app/Activity;)V",
            )
        }
    }
}
