/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.controls

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import com.android.tools.smali.dexlib2.Opcode

internal object WriteCharacteristicFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            definingClass = "Landroid/bluetooth/BluetoothGatt;",
            name = "writeCharacteristic",
            parameters = listOf("Landroid/bluetooth/BluetoothGattCharacteristic;"),
            returnType = "Z",
        ),
    ),
    custom = { _, classDef -> !classDef.type.startsWith("Lapp/hxreborn/extension/") },
)

internal object WriteCharacteristicWithValueFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            definingClass = "Landroid/bluetooth/BluetoothGatt;",
            name = "writeCharacteristic",
            parameters = listOf("Landroid/bluetooth/BluetoothGattCharacteristic;", "[B", "I"),
            returnType = "I",
        ),
    ),
    custom = { _, classDef -> !classDef.type.startsWith("Lapp/hxreborn/extension/") },
)

internal object CharacteristicReceivedFingerprint : Fingerprint(
    strings = listOf("OnCharacteristicReceived"),
    parameters = listOf("Landroid/bluetooth/BluetoothGatt;", "Landroid/bluetooth/BluetoothGattCharacteristic;", "[B", "I"),
    returnType = "V",
)

internal object CharacteristicWrittenFingerprint : Fingerprint(
    strings = listOf("OnCharacteristicWritten"),
    parameters = listOf("Landroid/bluetooth/BluetoothGatt;", "Landroid/bluetooth/BluetoothGattCharacteristic;", "I"),
    returnType = "V",
)

internal object ConnectionChangedFingerprint : Fingerprint(
    strings = listOf("OnConnectionStateChanged"),
    parameters = listOf("Landroid/bluetooth/BluetoothGatt;", "I", "I"),
    returnType = "V",
)

private const val GATT_TRANSPORT_LE = 2

internal object GattConnectionFactoryFingerprint : Fingerprint(
    strings = listOf("device.connectGatt returned null"),
    filters = listOf(
        fieldAccess(type = "Landroid/content/Context;", opcode = Opcode.IGET_OBJECT),
        fieldAccess(type = "Landroid/bluetooth/BluetoothGattCallback;", opcode = Opcode.IGET_OBJECT),
        literal(GATT_TRANSPORT_LE),
        methodCall(
            definingClass = "Landroid/bluetooth/BluetoothDevice;",
            name = "connectGatt",
            parameters = listOf("Landroid/content/Context;", "Z", "Landroid/bluetooth/BluetoothGattCallback;", "I"),
            returnType = "Landroid/bluetooth/BluetoothGatt;",
        ),
    ),
)

internal object GattRegistrationFingerprint : Fingerprint(
    classFingerprint = GattConnectionFactoryFingerprint,
    filters = listOf(
        methodCall(definingClass = "Landroid/bluetooth/BluetoothDevice;", name = "connectGatt"),
        fieldAccess(type = "Ljava/util/Map;", opcode = Opcode.IGET_OBJECT),
        methodCall(definingClass = "Ljava/util/Map;", name = "put"),
        fieldAccess(type = "Ljava/util/Map;", opcode = Opcode.IGET_OBJECT),
        methodCall(definingClass = "Ljava/util/Map;", name = "put"),
    ),
)
