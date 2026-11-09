/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.graphmessenger.misc.restrictions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.graphmessenger.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.findInstructionIndicesReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val SERIALIZER_PACKAGE = "Lorg/telegram/tgnet/"

context(_: BytecodePatchContext)
private fun Fingerprint.replaceReadsWithFalse(read: InstructionFilter, write: InstructionFilter) {
    val readers = matchAll().filter { match ->
        !match.classDef.type.startsWith(SERIALIZER_PACKAGE) &&
            match.method.findInstructionIndicesReversed(write).isEmpty()
    }
    if (readers.isEmpty()) throw PatchException("No method outside the serializers reads the restriction flag")

    readers.forEach { match ->
        match.method.findInstructionIndicesReversed(read).forEach { index ->
            val register = match.method.getInstruction<TwoRegisterInstruction>(index).registerA
            if (!match.method.isStoredIntoField(index + 1, register)) {
                match.method.replaceInstruction(index, "const/4 v$register, 0x0")
            }
        }
    }
}

private fun MutableMethod.isStoredIntoField(index: Int, register: Int): Boolean {
    val next = getInstruction(index)
    return next.opcode == Opcode.IPUT_BOOLEAN && (next as TwoRegisterInstruction).registerA == register
}

@Suppress("unused")
val removeRestrictionsPatch = bytecodePatch(
    name = "Remove restrictions",
    description = "Allows copying, forwarding, saving and screenshots in chats and channels that restrict them.",
) {
    compatibleWith(AppCompatibilities.GRAPH_MESSENGER)
    availability(requireArm64)

    dependsOn(spoofSignaturePatch)

    execute {
        MessageNoForwardsReadFingerprint.replaceReadsWithFalse(messageNoForwardsRead, messageNoForwardsWrite)
        ChatNoForwardsReadFingerprint.replaceReadsWithFalse(chatNoForwardsRead, chatNoForwardsWrite)
        PeerNoForwardsReadFingerprint.replaceReadsWithFalse(peerNoForwardsRead, peerNoForwardsWrite)
    }
}
