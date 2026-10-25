/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.internetspeedmeter.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.internetspeedmeter.misc.protection.removeAppProtectionPatch
import app.morphe.patches.internetspeedmeter.misc.settings.removeSettingsRow
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.asSequence
import app.morphe.util.removeFromParent
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import org.w3c.dom.Element

private const val PROMPT_ATTEMPTS_PAST_LIMIT = 6
private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/internetspeedmeter/AdPreference;"

private const val APPLOVIN_INIT_PROVIDER = "com.applovin.sdk.AppLovinInitProvider"
private const val AUDIENCE_NETWORK_PROVIDER = "com.facebook.ads.AudienceNetworkContentProvider"
private const val UNITY_ADS_INITIALIZER = "com.unity3d.services.core.configuration.AdsSdkInitializer"

private val AD_SDK_STARTUP_ENTRIES = listOf(
    "provider" to APPLOVIN_INIT_PROVIDER,
    "provider" to AUDIENCE_NETWORK_PROVIDER,
    "meta-data" to UNITY_ADS_INITIALIZER,
)

private val removeAdSdkStartupPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            AD_SDK_STARTUP_ENTRIES.forEach { (tag, name) ->
                document.getElementsByTagName(tag).asSequence().filterIsInstance<Element>()
                    .singleOrNull { it.getAttribute("android:name") == name }?.removeFromParent()
                    ?: throw PatchException("Expected exactly one <$tag> $name in the manifest")
            }
        }
    }
}

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes ads and the prompts to allow them.",
) {
    compatibleWith(AppCompatibilities.INTERNET_SPEED_METER_LITE)

    dependsOn(removeAppProtectionPatch, removeAdSdkStartupPatch)

    execute {
        LoadAdsFingerprint.method.returnEarly()

        AllowAdsPromptFingerprint.method.apply {
            val attemptsIndex = AllowAdsPromptFingerprint.instructionMatches.last().index + 1
            val attemptsRegister = getInstruction<OneRegisterInstruction>(attemptsIndex).registerA
            addInstruction(attemptsIndex + 1, "const/4 v$attemptsRegister, $PROMPT_ATTEMPTS_PAST_LIMIT")
        }

        FirstRunAdNoticeFingerprint.let { it.method.removeInstruction(it.instructionMatches.last().index) }

        ApplicationOnCreateFingerprint.method.addInstructions(
            0,
            "invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->markAdsEnabled(Landroid/content/Context;)V",
        )

        removeSettingsRow("ads_allow_title")
    }
}
