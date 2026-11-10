/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.equalizer

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.ultimea.misc.controls.xupmixPatch
import app.morphe.patches.ultimea.misc.settings.markPatchApplied
import app.morphe.patches.ultimea.misc.settings.nativeMethods
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.util.StringUtils.escapeString
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference

private const val CONNECTION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/ultimea/equalizer/EqualizerConnection;"
private const val PANEL_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/ultimea/equalizer/FineEqualizer;"

internal val equalizerConnectionPatch = bytecodePatch {
    dependsOn(xupmixPatch)
    execute {
        markPatchApplied("fineEqualizer", fineEqualizerPatch)
        val bases = nativeMethods.snapshot.stringsByPoolOffset.values.distinct().single {
            "\"default_mode\"" in it && "\"style\"" in it
        }
        mutableClassDefBy("Lapp/hxreborn/extension/ultimea/equalizer/EqualizerBases;").replaceStub("definitions", 1,
            """
                const-string/jumbo v0, "${escapeString(bases)}"
                return-object v0
            """.trimIndent())
        listOf(
            listOf("Landroid/bluetooth/BluetoothGatt;", "Landroid/bluetooth/BluetoothGattCharacteristic;"),
            listOf("Landroid/bluetooth/BluetoothGatt;", "Landroid/bluetooth/BluetoothGattCharacteristic;", "[B", "I"),
        ).forEach { parameters ->
            val match = ReadbackCallFingerprint("writeCharacteristic", parameters).matchSingle()
            val (write) = match.instructionMatches
            val call = match.method.getInstruction<ReferenceInstruction>(write.index)
            val reference = call.reference as MethodReference
            val target = ImmutableMethodReference(CONNECTION_CLASS_DESCRIPTOR, reference.name, reference.parameterTypes, reference.returnType)
            val replacement = when (call) {
                is RegisterRangeInstruction -> BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,
                    call.startRegister, call.registerCount, target)
                is FiveRegisterInstruction -> BuilderInstruction35c(Opcode.INVOKE_STATIC,
                    call.registerCount, call.registerC, call.registerD, call.registerE, call.registerF, call.registerG, target)
                else -> throw PatchException("Expected Bluetooth write invocation")
            }
            match.method.replaceInstruction(write.index, replacement)
        }
        listOf("onCharacteristicWrite", "onCharacteristicReceived", "onConnectionStateChanged").forEach { name ->
            val match = ReadbackCallFingerprint(name).matchSingle()
            val (callback) = match.instructionMatches
            val call = match.method.getInstruction<ReferenceInstruction>(callback.index)
            val reference = call.reference as MethodReference
            val target = ImmutableMethodReference(CONNECTION_CLASS_DESCRIPTOR, reference.name, reference.parameterTypes, reference.returnType)
            val replacement = when (call) {
                is RegisterRangeInstruction ->
                    BuilderInstruction3rc(
                        Opcode.INVOKE_STATIC_RANGE, call.startRegister, call.registerCount, target)
                is FiveRegisterInstruction -> BuilderInstruction35c(Opcode.INVOKE_STATIC, call.registerCount,
                    call.registerC, call.registerD, call.registerE, call.registerF, call.registerG, target)
                else -> throw PatchException("Expected Bluetooth callback invocation")
            }
            match.method.replaceInstruction(callback.index, replacement)
        }
        val activity = DeviceActivityFingerprint.matchSingle().method
        activity.addInstructions(activity.implementation!!.instructions.lastIndex,
            "invoke-static { p0 }, $PANEL_CLASS_DESCRIPTOR->attach(Landroid/app/Activity;)V")
        NavigationMessageFingerprint.matchSingle().method.addInstructions(0,
            "invoke-static { p1, p2 }, $PANEL_CLASS_DESCRIPTOR->navigation(Ljava/lang/String;Ljava/nio/ByteBuffer;)V")
    }
}
