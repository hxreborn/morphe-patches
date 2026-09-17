/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vpnsuper.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findFreeRegister
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val FUNCTION0_CLASS = "Lkotlin/jvm/functions/Function0;"
private const val BOOLEAN_CLASS = "Ljava/lang/Boolean;"
private const val USER_STATE_LABEL = ", userState="
private const val MULTI_PLATFORM_LABEL = ", isMpfEnabled="
private const val SHOW_BANNER_ADS_LABEL = ", showBannerAds="
private const val FREE_TRIAL_LABEL = ", freeTrial="
private const val PROMO_BANNER_COUNT = 4
private const val PREMIUM_USER_STATE = "PrimaryPremiumUserActiveSubscriptionNotSignedUp"

private fun writes(field: FieldReference): Instruction.() -> Boolean = {
    (opcode == Opcode.IPUT_BOOLEAN || opcode == Opcode.IPUT_OBJECT) &&
        (this as ReferenceInstruction).reference == field
}

private fun Method.booleanFieldsBetween(from: String, until: String): List<FieldReference> {
    val start = indexOfFirstInstructionOrThrow(string(from))
    val end = indexOfFirstInstructionOrThrow(start, string(until))

    return (start until end).mapNotNull { index ->
        getInstruction<Instruction>(index)
            .takeIf { it.opcode == Opcode.IGET_BOOLEAN }
            ?.let { (it as ReferenceInstruction).reference as FieldReference }
    }
}

private fun Method.fieldAfter(opcode: Opcode, startIndex: Int): FieldReference =
    getInstruction<ReferenceInstruction>(
        indexOfFirstInstructionOrThrow(startIndex, opcode),
    ).reference as FieldReference

private fun ClassDef.readsBooleanTrue() = methods.any { method ->
    method.indexOfFirstInstruction {
        opcode == Opcode.SGET_OBJECT &&
            (this as ReferenceInstruction).reference.let {
                it is FieldReference && it.definingClass == BOOLEAN_CLASS && it.name == "TRUE"
            }
    } >= 0
}

private fun ClassDef.hasInstanceFields() =
    fields.any { !AccessFlags.STATIC.isSet(it.accessFlags) }

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks premium servers and removes ads, upgrade banners, the launch paywall and " +
        "the Android TV sign-in screen.",
) {
    compatibleWith(AppCompatibilities.VPN_SUPER)

    execute {
        val candidates = VipStatusFlowFingerprint.matchAll()
            .filter { FUNCTION0_CLASS !in it.classDef.interfaces }
        val vipStatus = candidates.singleOrNull()
            ?: throw PatchException(
                "Expected one VIP status flow reading the settings data store, found ${candidates.size}",
            )

        val vipStatusInterface = vipStatus.classDef.interfaces.single()

        fun statelessImplementation(vararg excluded: String) = classDefByOrNull { classDef ->
            classDef.type !in excluded &&
                classDef.interfaces.singleOrNull() == vipStatusInterface &&
                !classDef.hasInstanceFields() &&
                classDef.readsBooleanTrue()
        }

        val statelessVipStatus = statelessImplementation(vipStatus.classDef.type)
            ?: throw PatchException(
                "No stateless $vipStatusInterface implementation reading $BOOLEAN_CLASS->TRUE",
            )
        statelessImplementation(vipStatus.classDef.type, statelessVipStatus.type)?.let {
            throw PatchException("${statelessVipStatus.type} and ${it.type} both qualify")
        }

        val declaredConstructors = statelessVipStatus.methods.filter { it.name == "<init>" }
        val constructorOwner = when {
            declaredConstructors.isEmpty() -> statelessVipStatus.superclass
            else -> declaredConstructors.singleOrNull { it.parameters.isEmpty() }?.definingClass
                ?: throw PatchException("${statelessVipStatus.type} has no no-argument constructor")
        }

        vipStatus.method.addInstructions(
            0,
            """
                new-instance v0, ${statelessVipStatus.type}
                invoke-direct { v0 }, $constructorOwner-><init>()V
                invoke-virtual { v0 }, ${statelessVipStatus.type}->invoke()Ljava/lang/Object;
                move-result-object v0
                return-object v0
            """,
        )

        TvSplashDestinationFingerprint.matchSingle().method.apply {
            val index = indexOfFirstInstructionOrThrow(Opcode.IGET_BOOLEAN)
            val register = getInstruction<TwoRegisterInstruction>(index).registerA

            replaceInstruction(index, "const/4 v$register, 0x1")
        }

        PrivacyPolicyAcceptedFingerprint.matchSingle().method.apply {
            val index = indexOfFirstInstructionOrThrow(Opcode.IGET_BOOLEAN)
            val register = getInstruction<TwoRegisterInstruction>(index).registerA

            replaceInstruction(index, "const/4 v$register, 0x1")
        }

        val connectReport = ConnectReportToStringFingerprint.matchSingle()
        val promoBanners = connectReport.method.booleanFieldsBetween(
            SHOW_BANNER_ADS_LABEL,
            FREE_TRIAL_LABEL,
        )
        if (promoBanners.size != PROMO_BANNER_COUNT) {
            throw PatchException("Expected $PROMO_BANNER_COUNT promo banner flags, found $promoBanners")
        }

        mutableClassDefBy(connectReport.classDef.type).methods
            .single { it.name == "<init>" }
            .apply {
                promoBanners.reversed().forEach { banner ->
                    val index = indexOfFirstInstructionOrThrow(filter = writes(banner))
                    val register = getInstruction<TwoRegisterInstruction>(index).registerA

                    addInstructions(index, "const/4 v$register, 0x0")
                }
            }

        val userInfo = UserInfoToStringFingerprint.matchSingle()
        val isPremium = userInfo.method.fieldAfter(Opcode.IGET_BOOLEAN, 0)
        val userState = userInfo.method.fieldAfter(
            Opcode.IGET_OBJECT,
            userInfo.method.indexOfFirstInstructionOrThrow(string(USER_STATE_LABEL)),
        )
        val multiPlatform = userInfo.method.fieldAfter(
            Opcode.IGET_BOOLEAN,
            userInfo.method.indexOfFirstInstructionOrThrow(string(MULTI_PLATFORM_LABEL)),
        )

        mutableClassDefBy(userInfo.classDef.type).methods
            .single { it.name == "<init>" && it.indexOfFirstInstruction(filter = writes(isPremium)) >= 0 }
            .apply {
                val premiumIndex = indexOfFirstInstructionOrThrow(filter = writes(isPremium))
                val premiumRegister = getInstruction<TwoRegisterInstruction>(premiumIndex).registerA
                addInstructions(premiumIndex, "const/16 v$premiumRegister, 0x1")

                val stateIndex = indexOfFirstInstructionOrThrow(filter = writes(userState))
                val stateRegister = getInstruction<TwoRegisterInstruction>(stateIndex).registerA
                addInstructions(stateIndex, "const-string v$stateRegister, \"$PREMIUM_USER_STATE\"")

                val mpfIndex = indexOfFirstInstructionOrThrow(filter = writes(multiPlatform))
                val instance = getInstruction<TwoRegisterInstruction>(mpfIndex).registerB
                val free = findFreeRegister(mpfIndex, instance)
                addInstructions(mpfIndex, "const/4 v$free, 0x0")
                replaceInstruction(
                    mpfIndex + 1,
                    "iput-boolean v$free, v$instance, " +
                        "${multiPlatform.definingClass}->${multiPlatform.name}:Z",
                )
            }
    }
}
