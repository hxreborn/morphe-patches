/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.controls

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patches.shared.misc.smali.assembleDex
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class XupmixReadbackPatchTest {
    @Test
    fun createReplacementAndRegisterItInBothHostMaps() {
        for (range in listOf(false, true)) {
            val directory = Files.createTempDirectory("gatt-reconnect").toFile()
            try {
                val connect = if (range) "invoke-virtual/range {v0 .. v4}" else "invoke-virtual {v0, v1, v2, v3, v4}"
                val dex = assembleDex(listOf(
                    """
                        .class public Lfixture/Connection;
                        .super Ljava/lang/Object;
                        .field private a:Landroid/content/Context;
                        .field private b:Landroid/bluetooth/BluetoothGattCallback;
                        .field private c:Ljava/util/Map;
                        .field private d:Ljava/util/Map;
                        .field private e:Ljava/util/concurrent/Semaphore;
                        .method public connect()V
                            .registers 8
                            const/4 v0, 0x0
                            iget-object v1, p0, Lfixture/Connection;->a:Landroid/content/Context;
                            const/4 v2, 0x1
                            iget-object v3, p0, Lfixture/Connection;->b:Landroid/bluetooth/BluetoothGattCallback;
                            const/4 v4, 0x2
                            $connect, Landroid/bluetooth/BluetoothDevice;->connectGatt(Landroid/content/Context;ZLandroid/bluetooth/BluetoothGattCallback;I)Landroid/bluetooth/BluetoothGatt;
                            move-result-object v0
                            iget-object v5, p0, Lfixture/Connection;->c:Ljava/util/Map;
                            const-string v6, "address"
                            invoke-interface {v5, v6, v0}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                            iget-object v5, p0, Lfixture/Connection;->d:Ljava/util/Map;
                            invoke-interface {v5, v6, v0}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                            const-string v0, "device.connectGatt returned null"
                            return-void
                        .end method
                        .method public legacy(Landroid/bluetooth/BluetoothGatt;Landroid/bluetooth/BluetoothGattCharacteristic;)V
                            .registers 3
                            invoke-virtual {p1, p2}, Landroid/bluetooth/BluetoothGatt;->writeCharacteristic(Landroid/bluetooth/BluetoothGattCharacteristic;)Z
                            return-void
                        .end method
                        .method public modern(Landroid/bluetooth/BluetoothGatt;Landroid/bluetooth/BluetoothGattCharacteristic;[BI)V
                            .registers 5
                            invoke-virtual/range {p1 .. p4}, Landroid/bluetooth/BluetoothGatt;->writeCharacteristic(Landroid/bluetooth/BluetoothGattCharacteristic;[BI)I
                            return-void
                        .end method
                    """.trimIndent(),
                    """
                        .class public Lfixture/Callback;
                        .super Landroid/bluetooth/BluetoothGattCallback;
                        .field private q:Lfixture/Connection;
                        .method public onCharacteristicWrite(Landroid/bluetooth/BluetoothGatt;Landroid/bluetooth/BluetoothGattCharacteristic;I)V
                            .registers 5
                            const-string v0, "OnCharacteristicWritten"
                            return-void
                        .end method
                        .method public onCharacteristicReceived(Landroid/bluetooth/BluetoothGatt;Landroid/bluetooth/BluetoothGattCharacteristic;[BI)V
                            .registers 6
                            const-string v0, "OnCharacteristicReceived"
                            return-void
                        .end method
                        .method public onConnectionStateChange(Landroid/bluetooth/BluetoothGatt;II)V
                            .registers 5
                            const-string v0, "OnConnectionStateChanged"
                            return-void
                        .end method
                    """.trimIndent(),
                ))
                val apk = directory.resolve("fixture.apk")
                ZipOutputStream(apk.outputStream()).use { zip ->
                    zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
                    checkNotNull(javaClass.getResourceAsStream("/metadata/AndroidManifest.axml")).use { it.copyTo(zip) }
                    zip.closeEntry()
                    zip.putNextEntry(ZipEntry("classes.dex"))
                    zip.write(dex)
                    zip.closeEntry()
                }
                Patcher(PatcherConfig(apk, directory.resolve("work"))).use { patcher ->
                    patcher += setOf(xupmixReadbackPatch)
                    runBlocking { patcher().collect { assertNull(it.exception) } }
                    val classes = patcher.get().dexFiles.flatMap { output ->
                        output.stream.use { DexBackedDexFile.fromInputStream(Opcodes.getDefault(), it.buffered()).classes }
                    }
                    val host = classes.single { it.type == "Lfixture/Connection;" }
                    for ((name, parameters, returnType) in listOf(
                        Triple("legacy", listOf("Landroid/bluetooth/BluetoothGatt;", "Landroid/bluetooth/BluetoothGattCharacteristic;"), "Z"),
                        Triple("modern", listOf("Landroid/bluetooth/BluetoothGatt;", "Landroid/bluetooth/BluetoothGattCharacteristic;", "[B", "I"), "I"),
                    )) {
                        val write = host.methods.single { it.name == name }.implementation!!.instructions.first()
                        val reference = (write as ReferenceInstruction).reference as MethodReference
                        assertEquals("Lapp/hxreborn/extension/ultimea/controls/XupmixReadback;", reference.definingClass)
                        assertEquals("writeCharacteristic", reference.name)
                        assertEquals(parameters, reference.parameterTypes.map(CharSequence::toString))
                        assertEquals(returnType, reference.returnType)
                        if (name == "legacy") {
                            assertEquals(Opcode.INVOKE_STATIC, write.opcode)
                            val invocation = write as FiveRegisterInstruction
                            assertEquals(2, invocation.registerCount)
                            assertEquals(1, invocation.registerC)
                            assertEquals(2, invocation.registerD)
                        } else {
                            assertEquals(Opcode.INVOKE_STATIC_RANGE, write.opcode)
                            val invocation = write as RegisterRangeInstruction
                            assertEquals(1, invocation.startRegister)
                            assertEquals(4, invocation.registerCount)
                        }
                    }
                    val method = host.methods.single { it.name == "reconnectReadbackGatt" }
                    val instructions = method.implementation!!.instructions.toList()
                    fun call(index: Int) = (instructions[index] as? ReferenceInstruction)?.reference as? MethodReference
                    val create = instructions.indices.single { call(it)?.name == "connectGatt" }
                    val replacement = (instructions[create + 1] as OneRegisterInstruction).registerA
                    val addressCall = instructions.indices.single { call(it)?.name == "getAddress" }
                    val address = (instructions[addressCall + 1] as OneRegisterInstruction).registerA
                    val invocation = instructions[create] as RegisterRangeInstruction
                    val autoRegister = invocation.startRegister + 2
                    val autoValue = instructions.take(create).last {
                        it is NarrowLiteralInstruction && it is OneRegisterInstruction && it.registerA == autoRegister
                    } as NarrowLiteralInstruction
                    assertEquals(1, autoValue.narrowLiteral)
                    assertEquals(5, invocation.registerCount)
                    val transportRegister = invocation.startRegister + 4
                    val transportValue = instructions.take(create).last {
                        it is NarrowLiteralInstruction && it is OneRegisterInstruction && it.registerA == transportRegister
                    } as NarrowLiteralInstruction
                    assertEquals(2, transportValue.narrowLiteral)
                    val writes = instructions.indices.filter { call(it)?.definingClass == "Ljava/util/Map;" && call(it)?.name == "put" }
                    assertEquals(2, writes.size)
                    val fields = writes.map { index ->
                        val put = instructions[index] as FiveRegisterInstruction
                        assertEquals(replacement, put.registerE)
                        assertEquals(address, put.registerD)
                        instructions.take(index).last { instruction ->
                            instruction.opcode == Opcode.IGET_OBJECT && (instruction as TwoRegisterInstruction).registerA == put.registerC
                        }.let { ((it as ReferenceInstruction).reference as FieldReference).name }
                    }
                    assertEquals(setOf("c", "d"), fields.toSet())
                    val lookup = instructions.indices.single { call(it)?.definingClass == "Ljava/util/Map;" && call(it)?.name == "get" }
                    val get = instructions[lookup] as FiveRegisterInstruction
                    assertEquals(Opcode.INVOKE_INTERFACE, get.opcode)
                    assertEquals(2, get.registerCount)
                    assertEquals(address, get.registerD)
                    val receiverLoad = instructions[lookup - 1]
                    assertEquals(Opcode.IGET_OBJECT, receiverLoad.opcode)
                    assertEquals(get.registerC, (receiverLoad as TwoRegisterInstruction).registerA)
                    assertEquals(method.implementation!!.registerCount - 2, receiverLoad.registerB)
                    val lookupField = (receiverLoad as ReferenceInstruction).reference as FieldReference
                    assertEquals(host.type, lookupField.definingClass)
                    assertEquals("d", lookupField.name)
                    assertEquals("Ljava/util/Map;", lookupField.type)
                    val lookupResult = (instructions[lookup + 1] as OneRegisterInstruction).registerA
                    val guard = instructions[lookup + 2] as TwoRegisterInstruction
                    assertEquals(Opcode.IF_NE, guard.opcode)
                    assertEquals(lookupResult, guard.registerA)
                    assertEquals(method.implementation!!.registerCount - 1, guard.registerB)
                    val addresses = instructions.runningFold(0) { position, instruction -> position + instruction.codeUnits }.dropLast(1)
                    val nullGuard = instructions[create + 2] as OneRegisterInstruction
                    assertEquals(Opcode.IF_NEZ, nullGuard.opcode)
                    assertEquals(replacement, nullGuard.registerA)
                    val registeredAddress = addresses[create + 2] + (nullGuard as OffsetInstruction).codeOffset
                    val registeredIndex = addresses.indexOf(registeredAddress)
                    val failurePath = instructions.subList(create + 3, registeredIndex)
                    assertEquals(4, failurePath.size)
                    val (allocate, message, initialize, failure) = failurePath
                    assertEquals(Opcode.NEW_INSTANCE, allocate.opcode)
                    assertEquals("Ljava/lang/IllegalStateException;", ((allocate as ReferenceInstruction).reference as TypeReference).type)
                    val exception = (allocate as OneRegisterInstruction).registerA
                    assertEquals(Opcode.CONST_STRING, message.opcode)
                    assertEquals(Opcode.INVOKE_DIRECT, initialize.opcode)
                    val constructor = (initialize as ReferenceInstruction).reference as MethodReference
                    assertEquals("Ljava/lang/IllegalStateException;", constructor.definingClass)
                    assertEquals("<init>", constructor.name)
                    assertEquals(listOf("Ljava/lang/String;"), constructor.parameterTypes.map(CharSequence::toString))
                    assertEquals("V", constructor.returnType)
                    assertEquals(exception, (initialize as FiveRegisterInstruction).registerC)
                    assertEquals((message as OneRegisterInstruction).registerA, initialize.registerD)
                    assertEquals(Opcode.THROW, failure.opcode)
                    assertEquals(exception, (failure as OneRegisterInstruction).registerA)
                    assertTrue(writes.all { it > registeredIndex })
                    val mismatchTarget = addresses[lookup + 2] + (guard as OffsetInstruction).codeOffset
                    assertTrue(lookup + 2 < create)
                    assertTrue(addresses.indexOf(mismatchTarget) > writes.last())

                    assertEquals(1, instructions.indices.count { call(it)?.name == "acquireUninterruptibly" })
                    assertEquals(2, instructions.indices.count { call(it)?.name == "release" })
                    val tryBlock = method.implementation!!.tryBlocks.single()
                    val createAddress = instructions.take(create).sumOf { it.codeUnits }
                    assertTrue(createAddress in tryBlock.startCodeAddress until tryBlock.startCodeAddress + tryBlock.codeUnitCount)
                    for (write in writes) {
                        assertTrue(addresses[write] in tryBlock.startCodeAddress until tryBlock.startCodeAddress + tryBlock.codeUnitCount)
                    }
                    val handler = tryBlock.exceptionHandlers.single { it.exceptionType == null }
                    val handlerIndex = addresses.indexOf(handler.handlerCodeAddress)
                    assertEquals(Opcode.MOVE_EXCEPTION, instructions[handlerIndex].opcode)
                    assertEquals("release", call(handlerIndex + 1)?.name)
                    assertEquals(Opcode.THROW, instructions[handlerIndex + 2].opcode)
                    val normalRelease = instructions.indices.single { call(it)?.name == "release" && it != handlerIndex + 1 }
                    assertEquals(Opcode.RETURN_VOID, instructions[normalRelease + 1].opcode)
                    assertEquals(addresses[normalRelease - 1], mismatchTarget)
                    assertEquals(writes.last() + 2, normalRelease)
                    assertTrue(addresses[normalRelease] >= tryBlock.startCodeAddress + tryBlock.codeUnitCount)
                    val acquire = instructions.indices.single { call(it)?.name == "acquireUninterruptibly" }
                    val mutex = (instructions[acquire] as FiveRegisterInstruction).registerC
                    assertTrue(addresses[acquire] < tryBlock.startCodeAddress)
                    assertEquals(mutex, (instructions[normalRelease] as FiveRegisterInstruction).registerC)
                    assertEquals(mutex, (instructions[handlerIndex + 1] as FiveRegisterInstruction).registerC)
                    val callback = classes.single { it.type == "Lfixture/Callback;" }
                    val extension = "Lapp/hxreborn/extension/ultimea/controls/XupmixReadback;"
                    val callbackType = "Lapp/hxreborn/extension/ultimea/controls/ReadbackCallbacks;"
                    val gattType = "Landroid/bluetooth/BluetoothGatt;"
                    val characteristicType = "Landroid/bluetooth/BluetoothGattCharacteristic;"
                    fun assertStaticRange(instruction: Instruction,
                                          name: String, parameters: List<String>, returnType: String, start: Int) {
                        assertEquals(Opcode.INVOKE_STATIC_RANGE, instruction.opcode)
                        val registers = instruction as RegisterRangeInstruction
                        assertEquals(start, registers.startRegister)
                        assertEquals(parameters.size, registers.registerCount)
                        val reference = (instruction as ReferenceInstruction).reference as MethodReference
                        assertEquals(extension, reference.definingClass)
                        assertEquals(name, reference.name)
                        assertEquals(parameters, reference.parameterTypes.map(CharSequence::toString))
                        assertEquals(returnType, reference.returnType)
                    }
                    for ((name, marker) in listOf("onCharacteristicWrite" to "OnCharacteristicWritten",
                        "onCharacteristicReceived" to "OnCharacteristicReceived")) {
                        val patched = callback.methods.single { it.name == name }
                        val body = patched.implementation!!.instructions.toList()
                        val offsets = body.runningFold(0) { position, instruction -> position + instruction.codeUnits }.dropLast(1)
                        fun target(index: Int) = offsets.indexOf(offsets[index] + (body[index] as OffsetInstruction).codeOffset)
                        val firstParameter = patched.implementation!!.registerCount - patched.parameters.size
                        val hostIndex: Int
                        if (name == "onCharacteristicWrite") {
                            assertStaticRange(body[0], name, listOf(callbackType, gattType, characteristicType, "I"), "Z", firstParameter - 1)
                            assertEquals(Opcode.MOVE_RESULT, body[1].opcode)
                            assertEquals(Opcode.IF_EQZ, body[2].opcode)
                            assertEquals((body[1] as OneRegisterInstruction).registerA, (body[2] as OneRegisterInstruction).registerA)
                            assertEquals(Opcode.RETURN_VOID, body[3].opcode)
                            hostIndex = target(2)
                            assertEquals(4, hostIndex)
                        } else {
                            assertEquals(Opcode.IF_NEZ, body[0].opcode)
                            assertEquals(firstParameter + 3, (body[0] as OneRegisterInstruction).registerA)
                            assertEquals(Opcode.IF_EQZ, body[1].opcode)
                            assertEquals(firstParameter + 2, (body[1] as OneRegisterInstruction).registerA)
                            assertStaticRange(body[2], name, listOf(callbackType, gattType, characteristicType, "[B", "I"), "[B", firstParameter - 1)
                            assertEquals(Opcode.MOVE_RESULT_OBJECT, body[3].opcode)
                            assertEquals(firstParameter + 2, (body[3] as OneRegisterInstruction).registerA)
                            assertEquals(Opcode.IF_NEZ, body[4].opcode)
                            assertEquals(firstParameter + 2, (body[4] as OneRegisterInstruction).registerA)
                            assertEquals(Opcode.RETURN_VOID, body[5].opcode)
                            hostIndex = target(4)
                            assertEquals(6, hostIndex)
                            assertEquals(hostIndex, target(0))
                            assertEquals(hostIndex, target(1))
                        }
                        assertEquals(Opcode.NOP, body[hostIndex].opcode)
                        assertEquals(marker, ((body[hostIndex + 1] as ReferenceInstruction).reference as StringReference).string)
                    }
                    val bridge = callback.methods.single { it.name == "onReadbackReconnect" }
                    val bridgeInstructions = bridge.implementation!!.instructions.toList()
                    assertEquals(3, bridgeInstructions.size)
                    val (ownerLoad, delegate, bridgeReturn) = bridgeInstructions
                    assertEquals(Opcode.IGET_OBJECT, ownerLoad.opcode)
                    val ownerRegisters = ownerLoad as TwoRegisterInstruction
                    assertEquals(bridge.implementation!!.registerCount - 2, ownerRegisters.registerB)
                    val ownerField = (ownerLoad as ReferenceInstruction).reference as FieldReference
                    assertEquals(callback.type, ownerField.definingClass)
                    assertEquals("q", ownerField.name)
                    assertEquals(host.type, ownerField.type)
                    assertEquals(Opcode.INVOKE_VIRTUAL, delegate.opcode)
                    val delegateRegisters = delegate as FiveRegisterInstruction
                    assertEquals(2, delegateRegisters.registerCount)
                    assertEquals(ownerRegisters.registerA, delegateRegisters.registerC)
                    assertEquals(bridge.implementation!!.registerCount - 1, delegateRegisters.registerD)
                    val delegatedMethod = (delegate as ReferenceInstruction).reference as MethodReference
                    assertEquals(host.type, delegatedMethod.definingClass)
                    assertEquals("reconnectReadbackGatt", delegatedMethod.name)
                    assertEquals(listOf("Landroid/bluetooth/BluetoothGatt;"), delegatedMethod.parameterTypes.map(CharSequence::toString))
                    assertEquals("V", delegatedMethod.returnType)
                    assertEquals(Opcode.RETURN_VOID, bridgeReturn.opcode)

                    val wrapper = callback.methods.single { it.name == "onConnectionStateChange" }
                    val wrapperInstructions = wrapper.implementation!!.instructions.toList()
                    val wrapperAddresses = wrapperInstructions.runningFold(0) { position, instruction -> position + instruction.codeUnits }.dropLast(1)
                    val wrapperCalls = wrapperInstructions.map { ((it as? ReferenceInstruction)?.reference as? MethodReference)?.name }
                    val retired = wrapperCalls.indexOf("isRetiredConnection")
                    assertEquals(0, retired)
                    val retiredCall = wrapperInstructions[retired] as FiveRegisterInstruction
                    assertEquals(Opcode.INVOKE_STATIC, retiredCall.opcode)
                    assertEquals(1, retiredCall.registerCount)
                    assertEquals(wrapper.implementation!!.registerCount - 3, retiredCall.registerC)
                    val retiredResult = wrapperInstructions[retired + 1] as OneRegisterInstruction
                    assertEquals(Opcode.MOVE_RESULT, retiredResult.opcode)
                    val retiredBranch = wrapperInstructions[retired + 2] as OneRegisterInstruction
                    assertEquals(Opcode.IF_NEZ, retiredBranch.opcode)
                    assertEquals(retiredResult.registerA, retiredBranch.registerA)
                    val retiredTarget = wrapperAddresses[retired + 2] + (retiredBranch as OffsetInstruction).codeOffset
                    val retiredReturn = wrapperAddresses.indexOf(retiredTarget)
                    assertEquals(Opcode.RETURN_VOID, wrapperInstructions[retiredReturn].opcode)
                    val register = wrapperCalls.indexOf("registerConnection")
                    val changed = wrapperCalls.indexOf("onConnectionStateChanged")
                    assertEquals(retired + 3, register)
                    assertEquals(register + 1, changed)
                    assertStaticRange(wrapperInstructions[register], "registerConnection", listOf(callbackType, gattType, "I", "I"), "V",
                        wrapper.implementation!!.registerCount - 4)
                    assertStaticRange(wrapperInstructions[changed], "onConnectionStateChanged", listOf(gattType, "I", "I"), "V",
                        wrapper.implementation!!.registerCount - 3)
                    val original = wrapperCalls.indexOf("onReadbackConnectionStateChanged")
                    val replace = wrapperCalls.indexOf("replaceRetiredConnection")
                    assertEquals(changed + 1, original)
                    assertEquals(Opcode.INVOKE_VIRTUAL_RANGE, wrapperInstructions[original].opcode)
                    val originalRegisters = wrapperInstructions[original] as RegisterRangeInstruction
                    assertEquals(wrapper.implementation!!.registerCount - 4, originalRegisters.startRegister)
                    assertEquals(4, originalRegisters.registerCount)
                    assertTrue(replace > original && retiredReturn > replace)
                }
            } finally {
                directory.deleteRecursively()
            }
        }
    }
}
