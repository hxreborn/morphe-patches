/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.cloneapp.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.asSequence
import app.morphe.util.findElementByAttributeValueOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val VIP_EXPIRY_MILLIS = 4102358400000L

private fun MutableMethod.overwriteFieldAfter(index: Int, literal: String) {
    val write = getInstruction<TwoRegisterInstruction>(index)
    val field = getInstruction<ReferenceInstruction>(index).getReference<FieldReference>()!!
    val (constant, store) = if (field.type == "J") "const-wide" to "iput-wide" else "const/16" to "iput-boolean"

    addInstructions(
        index + 1,
        """
            $constant v${write.registerA}, $literal
            $store v${write.registerA}, v${write.registerB}, $field
        """,
    )
}

private fun Document.elementWithId(id: String) =
    getElementsByTagName("*").findElementByAttributeValueOrThrow("android:id", "@id/$id")

private val removeVipOfferEntriesPatch = resourcePatch {
    execute {
        document("res/layout/dialog_accessibility.xml").use { document ->
            val freeVipTool = document.elementWithId("ll_free_vip")
            (freeVipTool.parentNode.parentNode as Element).setAttribute("android:visibility", "gone")
        }

        document("res/layout/activity_main.xml").use { document ->
            val activityCenterButton = document.elementWithId("iv_btn_active")
            (activityCenterButton.parentNode as Element).setAttribute("android:visibility", "gone")

            val freeVipTip = document.elementWithId("fl_action_tip")
            freeVipTip.childNodes.asSequence().filterIsInstance<Element>().toList().forEach {
                freeVipTip.removeChild(it)
            }
        }

        document("res/layout/activity_user.xml").use { document ->
            document.elementWithId("tv_btn_buy").setAttribute("android:visibility", "gone")
        }
    }
}

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks VIP: multiple copies of each app, batch cloning, bubble mode, custom themes " +
        "and no ads. Removes the 50% off offer and the Free VIP, Renew VIP and Get F-Code entries.",
) {
    compatibleWith(AppCompatibilities.CLONE_APP)

    dependsOn(removeVipOfferEntriesPatch)

    execute {
        val expiry = "0x${VIP_EXPIRY_MILLIS.toString(16)}L"

        UserStateParseFingerprint.matchSingle().apply {
            val (vipWrite, _, expiryWrite) = instructionMatches.map { it.index }
            method.overwriteFieldAfter(expiryWrite, expiry)
            method.overwriteFieldAfter(vipWrite, "0x1")
        }

        val engineVipSetter = UserStateRestoreFingerprint.matchSingle().run {
            val vipWrite = instructionMatches[3].index
            val expiryWrite = instructionMatches.last().index
            val setter = instructionMatches[4].getInstruction<ReferenceInstruction>()
                .getReference<MethodReference>()!!

            method.overwriteFieldAfter(expiryWrite, expiry)
            method.overwriteFieldAfter(vipWrite, "0x1")
            setter
        }

        engineVipStateSetterFingerprint(engineVipSetter).matchSingle().apply {
            val vipStore = instructionMatches.last()
            val vipField = vipStore.getInstruction<ReferenceInstruction>().getReference<FieldReference>()!!
            val parameterRegister = method.implementation!!.registerCount - 1
            if (vipStore.getInstruction<OneRegisterInstruction>().registerA != parameterRegister) {
                throw PatchException("$vipField is not set from the VIP parameter of ${method.name}")
            }

            method.addInstruction(0, "const/4 p1, 0x1")

            staticInitializerFingerprint(vipField.definingClass).matchSingle().method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    sput-boolean v0, $vipField
                """,
            )
        }

        FCodeOfferFlagFingerprint.matchSingle().apply {
            method.overwriteFieldAfter(instructionMatches.last().index, "0x0")
        }

        DiscountOfferPopupFingerprint.matchSingle().apply {
            val offerCheck = instructionMatches.last().index
            val register = method.getInstruction<OneRegisterInstruction>(offerCheck).registerA

            method.addInstruction(offerCheck + 1, "const/16 v$register, 0x0")
        }
    }
}
