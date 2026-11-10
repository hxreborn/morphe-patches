/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.controls

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference

private const val CALLBACKS_DESCRIPTOR = "Lapp/hxreborn/extension/ultimea/controls/ReadbackCallbacks;"

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/ultimea/controls/XupmixReadback;"

internal val xupmixReadbackPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")

    execute {
        listOf(WriteCharacteristicFingerprint, WriteCharacteristicWithValueFingerprint).forEach { fingerprint ->
            val match = fingerprint.matchSingle()
            val (write) = match.instructionMatches
            val call = match.method.getInstruction<Instruction>(write.index)
            val reference = call.getReference<MethodReference>()!!
            val wrapper = ImmutableMethodReference(
                EXTENSION_CLASS_DESCRIPTOR,
                "writeCharacteristic",
                listOf("Landroid/bluetooth/BluetoothGatt;") + reference.parameterTypes,
                reference.returnType,
            )
            val replacement = when (call) {
                is FiveRegisterInstruction -> BuilderInstruction35c(
                    Opcode.INVOKE_STATIC,
                    call.registerCount,
                    call.registerC,
                    call.registerD,
                    call.registerE,
                    call.registerF,
                    call.registerG,
                    wrapper,
                )
                is RegisterRangeInstruction -> BuilderInstruction3rc(
                    Opcode.INVOKE_STATIC_RANGE,
                    call.startRegister,
                    call.registerCount,
                    wrapper,
                )
                else -> throw PatchException("Expected a Bluetooth write invocation")
            }
            match.method.replaceInstruction(write.index, replacement)
        }

        val written = CharacteristicWrittenFingerprint.matchSingle().method
        val received = CharacteristicReceivedFingerprint.matchSingle().method
        val connection = ConnectionChangedFingerprint.matchSingle().method
        if (written.definingClass != received.definingClass || written.definingClass != connection.definingClass) {
            throw PatchException("Expected one Bluetooth callback class")
        }
        val writtenParameterRegisters = written.parameters.size + 1
        if (written.implementation!!.registerCount <= writtenParameterRegisters) {
            throw PatchException("Bluetooth write callback has no local register")
        }
        mutableClassDefBy(written.definingClass).apply {
            interfaces.add(CALLBACKS_DESCRIPTOR)
            addMethod(
                "onReadbackWrite",
                written.parameterTypes.map(CharSequence::toString),
                "V",
                AccessFlags.PUBLIC.value,
                writtenParameterRegisters,
                """
                    invoke-virtual/range { p0 .. p${written.parameters.size} }, ${descriptor(written)}
                    return-void
                """,
            )
            addMethod(
                "onReadbackReceived",
                received.parameterTypes.map(CharSequence::toString),
                "V",
                AccessFlags.PUBLIC.value,
                received.parameters.size + 1,
                """
                    invoke-virtual/range { p0 .. p${received.parameters.size} }, ${descriptor(received)}
                    return-void
                """,
            )
        }
        written.addInstructions(
            0,
            """
                invoke-static/range { p0 .. p3 }, $EXTENSION_CLASS_DESCRIPTOR->onCharacteristicWrite(${CALLBACKS_DESCRIPTOR}Landroid/bluetooth/BluetoothGatt;Landroid/bluetooth/BluetoothGattCharacteristic;I)Z
                move-result v0
                if-eqz v0, :host_write
                return-void
                :host_write
                nop
            """,
        )
        received.addInstructions(
            0,
            """
                if-nez p4, :host_receive
                if-eqz p3, :host_receive
                invoke-static/range { p0 .. p4 }, $EXTENSION_CLASS_DESCRIPTOR->onCharacteristicReceived(${CALLBACKS_DESCRIPTOR}Landroid/bluetooth/BluetoothGatt;Landroid/bluetooth/BluetoothGattCharacteristic;[BI)[B
                move-result-object p3
                if-nez p3, :host_receive
                return-void
                :host_receive
                nop
            """,
        )
        val factory = GattConnectionFactoryFingerprint.matchSingle()
        val (contextRead, callbackRead, transportConstant, connect) = factory.instructionMatches
        val registration = GattRegistrationFingerprint.matchSingle()
        if (registration.method != factory.method) throw PatchException("Expected one Bluetooth connection factory")
        val (_, connectingRead, _, autoRead) = registration.instructionMatches
        fun field(index: Int) = factory.method.getInstruction<Instruction>(index).getReference<FieldReference>()
            ?: throw PatchException("Expected Bluetooth connection field access")
        val contextField = field(contextRead.index)
        val callbackField = field(callbackRead.index)
        val connectingField = field(connectingRead.index)
        val autoField = field(autoRead.index)
        val transport = factory.method.getInstruction<NarrowLiteralInstruction>(transportConstant.index).narrowLiteral
        val transportRegister = factory.method.getInstruction<OneRegisterInstruction>(transportConstant.index).registerA
        val connectCall = factory.method.getInstruction<Instruction>(connect.index)
        val connectTransportRegister = when (connectCall) {
            is FiveRegisterInstruction -> connectCall.registerG
            is RegisterRangeInstruction -> connectCall.startRegister + connectCall.registerCount - 1
            else -> throw PatchException("Expected a Bluetooth connection invocation")
        }
        if (transportRegister != connectTransportRegister || connectingField == autoField ||
            listOf(contextField, callbackField, connectingField, autoField).any { it.definingClass != factory.method.definingClass }) {
            throw PatchException("Unexpected Bluetooth connection factory operands")
        }
        val plugin = mutableClassDefBy(factory.method.definingClass)
        val mutex = plugin.fields.singleOrNull { it.type == "Ljava/util/concurrent/Semaphore;" }
            ?: throw PatchException("Expected one Bluetooth connection semaphore")
        val reconnect = plugin.addMethod(
            "reconnectReadbackGatt", listOf("Landroid/bluetooth/BluetoothGatt;"), "V", AccessFlags.PUBLIC.value, 11,
            """
                iget-object v0, p0, ${descriptor(mutex)}
                invoke-virtual { v0 }, Ljava/util/concurrent/Semaphore;->acquireUninterruptibly()V
                :try_start
                invoke-virtual { p1 }, Landroid/bluetooth/BluetoothGatt;->getDevice()Landroid/bluetooth/BluetoothDevice;
                move-result-object v2
                invoke-virtual { v2 }, Landroid/bluetooth/BluetoothDevice;->getAddress()Ljava/lang/String;
                move-result-object v1
                iget-object v7, p0, ${descriptor(autoField)}
                invoke-interface { v7, v1 }, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v8
                if-ne v8, p1, :release_mutex
                iget-object v3, p0, ${descriptor(contextField)}
                const/4 v4, 0x1
                iget-object v5, p0, ${descriptor(callbackField)}
                const v6, $transport
                invoke-virtual/range { v2 .. v6 }, Landroid/bluetooth/BluetoothDevice;->connectGatt(Landroid/content/Context;ZLandroid/bluetooth/BluetoothGattCallback;I)Landroid/bluetooth/BluetoothGatt;
                move-result-object v2
                if-nez v2, :register_gatt
                new-instance v2, Ljava/lang/IllegalStateException;
                const-string v3, "Bluetooth connection replacement returned null"
                invoke-direct { v2, v3 }, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V
                throw v2
                :register_gatt
                iget-object v3, p0, ${descriptor(connectingField)}
                invoke-interface { v3, v1, v2 }, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                invoke-interface { v7, v1, v2 }, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                :release_mutex
                nop
                :try_end
                .catchall { :try_start .. :try_end } :release_after_failure
                invoke-virtual { v0 }, Ljava/util/concurrent/Semaphore;->release()V
                return-void
                :release_after_failure
                move-exception v1
                invoke-virtual { v0 }, Ljava/util/concurrent/Semaphore;->release()V
                throw v1
            """,
        )
        mutableClassDefBy(connection.definingClass).apply {
            val owner = fields.singleOrNull { it.type == plugin.type }
                ?: throw PatchException("Expected one Bluetooth callback owner")
            addMethod("onReadbackReconnect", listOf("Landroid/bluetooth/BluetoothGatt;"), "V", AccessFlags.PUBLIC.value, 3,
                """
                    iget-object v0, p0, ${descriptor(owner)}
                    invoke-virtual { v0, p1 }, ${descriptor(reconnect)}
                    return-void
                """,
            )
            val callbackName = connection.name
            methods.remove(connection)
            connection.setName("onReadbackConnectionStateChanged")
            methods.add(connection)
            addMethod(callbackName, connection.parameterTypes.map(CharSequence::toString), "V", connection.accessFlags, 5,
                """
                    invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->isRetiredConnection(Landroid/bluetooth/BluetoothGatt;)Z
                    move-result v0
                    if-nez v0, :callback_finished
                    invoke-static/range { p0 .. p3 }, $EXTENSION_CLASS_DESCRIPTOR->registerConnection(${CALLBACKS_DESCRIPTOR}Landroid/bluetooth/BluetoothGatt;II)V
                    invoke-static/range { p1 .. p3 }, $EXTENSION_CLASS_DESCRIPTOR->onConnectionStateChanged(Landroid/bluetooth/BluetoothGatt;II)V
                    invoke-virtual/range { p0 .. p3 }, ${descriptor(connection)}
                    invoke-static { p0, p1 }, $EXTENSION_CLASS_DESCRIPTOR->replaceRetiredConnection(${CALLBACKS_DESCRIPTOR}Landroid/bluetooth/BluetoothGatt;)V
                    :callback_finished
                    return-void
                """,
            )
        }
    }
}
