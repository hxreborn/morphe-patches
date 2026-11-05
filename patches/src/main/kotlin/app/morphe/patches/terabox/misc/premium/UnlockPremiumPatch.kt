/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.terabox.ads.HasPrivilegeFingerprint
import app.morphe.patches.terabox.misc.quality.unlockHdPlaybackPatch
import app.morphe.patches.terabox.misc.settings.PREMIUM_ENABLED
import app.morphe.patches.terabox.misc.settings.markPatchApplied
import app.morphe.patches.terabox.misc.settings.overrideWhenEnabled
import app.morphe.patches.terabox.misc.settings.patchesSettingsPatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val PREMIUM_PLUS_LEVEL = 2
private const val MAX_PLAYBACK_SPEED = 3.0
private const val SERVER_MEMBERSHIP_CLASS = "Lapp/hxreborn/extension/terabox/TeraboxServerMembership;"

private val DATA_SAVER_LAYOUTS = listOf(
    "video_bottom_bar_resolution_b",
    "video_full_bar_resolution_b",
    "video_full_bar_resolution_c",
    "video_full_bar_resolution_drama",
)

private val hideDataSaverOptionPatch = resourcePatch {
    execute {
        DATA_SAVER_LAYOUTS.forEach { layout ->
            document("res/layout/$layout.xml").use { document ->
                val controller = document.createElement("app.hxreborn.extension.terabox.DataSaverOption")
                controller.setAttribute("android:layout_width", "0dp")
                controller.setAttribute("android:layout_height", "0dp")
                document.documentElement.appendChild(controller)
            }
        }
    }
}

private const val DESCRIPTION = "Unlocks HD, 3x speed and uploads, with HD up to original quality. " +
    "HD buffers faster over parallel connections."

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Plus",
    description = DESCRIPTION,
) {
    compatibleWith(AppCompatibilities.TERABOX)
    dependsOn(patchesSettingsPatch, unlockHdPlaybackPatch, hideDataSaverOptionPatch)

    extendWith("extensions/extension.mpe")

    execute {
        markPatchApplied("unlockPremium", DESCRIPTION)
        VipInfoIsVipFingerprint.matchSingle().method.overrideWhenEnabled(PREMIUM_ENABLED) { returnEarly(true) }
        VipInfoLevelFingerprint.matchSingle().method.overrideWhenEnabled(PREMIUM_ENABLED) { returnEarly(PREMIUM_PLUS_LEVEL) }
        VipInfoIdentityFingerprint.matchSingle().method.overrideWhenEnabled(PREMIUM_ENABLED) { returnEarly(PREMIUM_PLUS_LEVEL) }
        NamedPrivilegeCheckFingerprint.matchSingle().method.overrideWhenEnabled(PREMIUM_ENABLED) { returnEarly(true) }
        HasPrivilegeFingerprint.matchSingle().method.overrideWhenEnabled(PREMIUM_ENABLED) { returnEarly(true) }
        VideoUploadPremiumSwitchFingerprint.matchSingle().method.overrideWhenEnabled(PREMIUM_ENABLED) { returnEarly(false) }
        PlaybackSpeedFreeLimitFingerprint.matchSingle().method.overrideWhenEnabled(PREMIUM_ENABLED) { returnEarly(MAX_PLAYBACK_SPEED) }

        val currentVipInfo = CurrentVipInfoFingerprint.matchSingle()
        val vipManager = currentVipInfo.classDef
        val vipManagerInstance = vipManager.fields.single {
            AccessFlags.STATIC.isSet(it.accessFlags) && it.type == vipManager.type
        }
        val loadServerVipInfo = """
            sget-object v0, ${descriptor(vipManagerInstance)}
            invoke-virtual { v0 }, ${descriptor(currentVipInfo.method)}
            move-result-object v0
        """

        ReportedVipStatusFingerprint.matchSingle().method.addInstructions(
            0,
            """
                $loadServerVipInfo
                invoke-static { v0 }, $SERVER_MEMBERSHIP_CLASS->isVip(Ljava/lang/Object;)Z
                move-result v0
                return v0
            """,
        )
        val vipTypeGetter = VipTypeQueryParameterFingerprint.matchSingle().instructionMatches
            .first { it.instruction.opcode == Opcode.INVOKE_STATIC }
            .instruction.getReference<MethodReference>()!!
        reportedVipTypeFingerprint(vipTypeGetter).matchSingle().method.addInstructions(
            0,
            """
                $loadServerVipInfo
                invoke-static { v0 }, $SERVER_MEMBERSHIP_CLASS->vipType(Ljava/lang/Object;)Ljava/lang/String;
                move-result-object v0
                return-object v0
            """,
        )
    }
}
