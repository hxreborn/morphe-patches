/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.terabox.ads.HasPrivilegeFingerprint
import app.morphe.patches.terabox.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.terabox.misc.quality.unlockHdPlaybackPatch
import app.morphe.util.findElementByAttributeValueOrThrow
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.formatter.DexFormatter
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
                document.getElementsByTagName("*")
                    .findElementByAttributeValueOrThrow("android:id", "@id/video_rb_resolution_fluent_layout")
                    .setAttribute("android:layout_height", "0dp")
            }
        }
    }
}

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Plus",
    description = "Unlocks HD up to original quality, playback speeds up to 3x and video uploads. " +
        "HD buffers faster over parallel connections.",
) {
    compatibleWith(AppCompatibilities.TERABOX)
    dependsOn(spoofSignaturePatch, unlockHdPlaybackPatch, hideDataSaverOptionPatch)

    extendWith("extensions/extension.mpe")

    execute {
        VipInfoIsVipFingerprint.matchSingle().method.returnEarly(true)
        VipInfoLevelFingerprint.matchSingle().method.returnEarly(PREMIUM_PLUS_LEVEL)
        VipInfoIdentityFingerprint.matchSingle().method.returnEarly(PREMIUM_PLUS_LEVEL)
        NamedPrivilegeCheckFingerprint.matchSingle().method.returnEarly(true)
        HasPrivilegeFingerprint.matchSingle().method.returnEarly(true)
        VideoUploadPremiumSwitchFingerprint.matchSingle().method.returnEarly(false)
        PlaybackSpeedFreeLimitFingerprint.matchSingle().method.returnEarly(MAX_PLAYBACK_SPEED)

        val currentVipInfo = CurrentVipInfoFingerprint.matchSingle()
        val vipManager = currentVipInfo.classDef
        val vipManagerInstance = vipManager.fields.single {
            AccessFlags.STATIC.isSet(it.accessFlags) && it.type == vipManager.type
        }
        val loadServerVipInfo = """
            sget-object v0, ${DexFormatter.INSTANCE.getFieldDescriptor(vipManagerInstance)}
            invoke-virtual { v0 }, ${DexFormatter.INSTANCE.getMethodDescriptor(currentVipInfo.method)}
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
