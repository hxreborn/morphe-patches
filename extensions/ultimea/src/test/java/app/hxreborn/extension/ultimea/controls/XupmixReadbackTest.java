/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.ultimea.controls;

import java.time.Duration;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.os.Looper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowBluetoothDevice;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = XupmixReadbackTest.GattShadow.class)
public class XupmixReadbackTest {

    private static final UUID WRITE_UUID = UUID.fromString("27758d11-bf3a-4ac6-bee5-6259ccb7c9b7");

    private static final UUID NOTIFY_UUID = UUID.fromString("27758d22-bf3a-4ac6-bee5-6259ccb7c9b7");

    private static final byte[] ENABLE = { (byte) 0xaa, 1, 0, 2, 0x16, 1, (byte) 0xc3 };

    private static final byte[] DISABLE = { (byte) 0xaa, 1, 0, 2, 0x16, 0, (byte) 0xc2 };

    private static final byte[] READ_ENABLED = { (byte) 0xbb, 1, 0, 1, 0x18, 1, (byte) 0xc4 };

    private static final byte[] READ_DISABLED = { (byte) 0xbb, 1, 0, 1, 0x18, 0, (byte) 0xc3 };

    private static final byte[] CONFIRMED_ENABLE = { (byte) 0xbb, 1, 0, 2, 0x16, 1, (byte) 0xc3 };

    private static final byte[] CONFIRMED_DISABLE = { (byte) 0xbb, 1, 0, 2, 0x16, 0, (byte) 0xc2 };

    private static final byte[] QUERY = { (byte) 0xaa, 0, 0, 1, 0x18, (byte) 0xc3 };

    private static final byte[] VOLUME_QUERY = { (byte) 0xaa, 0, 0, 1, 7, (byte) 0xb2 };

    private static final int SETTER_AND_QUERY_WRITES = 2;

    private static final int SETTER_VOLUME_AND_QUERY_WRITES = 3;

    private static final int READ_TIMEOUT_MS = 1800;

    private static final int READ_RETRY_MS = 50;

    private static final int DISCONNECT_TIMEOUT_MS = 2000;

    private static final int CONNECTION_RECOVERY_MS = 5000;

    @Test
    public void rejectsExpiredQueryReplyBeforeReplacementConnection() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = startQuery(gatt, writer);
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, 0));
        idle();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(READ_TIMEOUT_MS));
        GattShadow shadow = Shadow.extract(gatt);
        Assert.assertEquals(1, shadow.disconnects);
        Assert.assertEquals(BluetoothGatt.GATT_FAILURE, callback.writeStatus);
        Assert.assertEquals(BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY, XupmixReadback.writeCharacteristic(gatt,
                writer, ENABLE, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        Assert.assertNull(XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID),
                READ_ENABLED, BluetoothGatt.GATT_SUCCESS));
        idle();
        Assert.assertNull(callback.received);
        Assert.assertEquals(1, callback.writeEvents);
    }

    @Test
    public void replacesConnectionWhenDisconnectCallbackNeverArrives() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        HostCallbacks callback = new HostCallbacks();
        XupmixReadback.registerConnection(callback, gatt, 0, BluetoothProfile.STATE_CONNECTED);
        XupmixReadback.disconnect(gatt);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(DISCONNECT_TIMEOUT_MS));
        Assert.assertTrue(XupmixReadback.isRetiredConnection(gatt));
        Assert.assertEquals(1, callback.reconnects);
        Assert.assertFalse(XupmixReadback.writeWhenIdle(gatt, characteristic(WRITE_UUID)));
        callback.onConnectionStateChange(gatt, 0, BluetoothProfile.STATE_DISCONNECTED);
        Assert.assertEquals(1, callback.reconnects);
    }

    @Test
    public void keepsOneDelayedRetryAfterExplicitReplacementFailures() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        HostCallbacks callback = new HostCallbacks();
        XupmixReadback.disconnect(gatt);
        XupmixReadback.onConnectionStateChanged(gatt, 0, BluetoothProfile.STATE_DISCONNECTED);
        GattShadow shadow = Shadow.extract(gatt);
        shadow.closeFailure = new SecurityException("permission rejected");
        for (int attempt = 0; attempt < 3; attempt++) {
            XupmixReadback.replaceRetiredConnection(callback, gatt);
        }
        Assert.assertEquals(3, shadow.closes);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        Assert.assertEquals(4, shadow.closes);
        shadow.closeFailure = null;
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(CONNECTION_RECOVERY_MS));
        Assert.assertEquals(1, callback.reconnects);
        Assert.assertEquals(5, shadow.closes);
    }

    @Test
    public void recoversAfterFastReplacementAttemptsFail() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        HostCallbacks callback = new HostCallbacks();
        XupmixReadback.disconnect(gatt);
        XupmixReadback.onConnectionStateChanged(gatt, 0, BluetoothProfile.STATE_DISCONNECTED);
        GattShadow shadow = Shadow.extract(gatt);
        shadow.closeFailure = new SecurityException("permission rejected");
        XupmixReadback.replaceRetiredConnection(callback, gatt);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));
        Assert.assertEquals(3, shadow.closes);
        shadow.closeFailure = null;
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(CONNECTION_RECOVERY_MS));
        Assert.assertEquals(1, callback.reconnects);
        Assert.assertEquals(4, shadow.closes);
    }

    @Test
    public void rejectsLateSetterSuccessAfterDeadline() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = new HostCallbacks();
        Assert.assertEquals(0, XupmixReadback.writeCharacteristic(gatt, writer, ENABLE,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(READ_TIMEOUT_MS));
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
        idle();
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertEquals(BluetoothGatt.GATT_FAILURE, callback.writeStatus);
        GattShadow shadow = Shadow.extract(gatt);
        Assert.assertEquals(1, shadow.disconnects);
    }

    @Test
    public void reservesWriterBeforeReadbackQueryStarts() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = new HostCallbacks();
        Assert.assertEquals(0, XupmixReadback.writeCharacteristic(gatt, writer, ENABLE,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
        Assert.assertEquals(BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY, XupmixReadback.writeCharacteristic(gatt,
                writer, VOLUME_QUERY, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        Assert.assertFalse(XupmixReadback.writeWhenIdle(gatt, writer));
        idle();
        GattShadow shadow = Shadow.extract(gatt);
        Assert.assertEquals(SETTER_AND_QUERY_WRITES, shadow.writes);
    }

    @Test
    public void retainsLateCallbackOwnershipWhenDisconnectPermissionFails() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = startQuery(gatt, writer);
        GattShadow shadow = Shadow.extract(gatt);
        shadow.disconnectFailure = new SecurityException("Bluetooth permission rejected");
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(READ_TIMEOUT_MS));
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertEquals(BluetoothGatt.GATT_FAILURE, callback.writeStatus);
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
        idle();
        Assert.assertEquals(BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY, XupmixReadback.writeCharacteristic(gatt,
                writer, VOLUME_QUERY, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        Assert.assertEquals(1, callback.writeEvents);
    }

    @Test
    public void retriesRejectedDisconnectWhenPermissionReturns() {
        for (boolean queryStarted : new boolean[] { false, true }) {
            BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
            BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
            writer.setValue(VOLUME_QUERY);
            HostCallbacks callback = new HostCallbacks();
            XupmixReadback.registerConnection(callback, gatt, BluetoothGatt.GATT_SUCCESS,
                    BluetoothProfile.STATE_CONNECTED);
            Assert.assertEquals(BluetoothStatusCodes.SUCCESS, XupmixReadback.writeCharacteristic(gatt, writer, ENABLE,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
            if (queryStarted) {
                Assert.assertTrue(
                        XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
                idle();
            }
            GattShadow shadow = Shadow.extract(gatt);
            shadow.disconnectFailure = new SecurityException("Bluetooth permission rejected");
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(READ_TIMEOUT_MS));
            Assert.assertEquals(1, shadow.disconnects);
            Assert.assertEquals(1, callback.writeEvents);
            shadow.disconnectFailure = null;
            Assert.assertEquals(BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY, XupmixReadback
                .writeCharacteristic(gatt, writer, VOLUME_QUERY, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
            Assert.assertEquals(2, shadow.disconnects);
            Assert.assertFalse(XupmixReadback.writeWhenIdle(gatt, writer));
            Assert.assertEquals(2, shadow.disconnects);
            XupmixReadback.onConnectionStateChanged(gatt, BluetoothGatt.GATT_SUCCESS,
                    BluetoothProfile.STATE_DISCONNECTED);
            idle();
            Assert.assertFalse(XupmixReadback.writeWhenIdle(gatt, writer));
            Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
            Assert.assertFalse(XupmixReadback.writeWhenIdle(gatt, writer));
            Assert.assertTrue(XupmixReadback.isRetiredConnection(gatt));
            Assert.assertEquals(1, callback.writeEvents);
        }
    }

    @Test
    public void reservesWriterUntilConfirmedFrameReachesHost() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        writer.setValue(VOLUME_QUERY);
        boolean[] wroteDuringCompletion = { false };
        HostCallbacks callback = new HostCallbacks() {
            @Override
            public void onReadbackReceived(BluetoothGatt connection, BluetoothGattCharacteristic characteristic,
                    byte[] value, int status) {
                Assert.assertTrue(XupmixReadback.hasPendingWrite(connection));
                Assert.assertFalse(XupmixReadback.writeWhenIdle(connection, writer));
                super.onReadbackReceived(connection, characteristic, value, status);
            }

            @Override
            public void onReadbackWrite(BluetoothGatt connection, BluetoothGattCharacteristic characteristic,
                    int status) {
                super.onReadbackWrite(connection, characteristic, status);
                Assert.assertFalse(XupmixReadback.hasPendingWrite(connection));
                wroteDuringCompletion[0] = XupmixReadback.writeWhenIdle(connection, writer);
            }
        };
        Assert.assertEquals(BluetoothStatusCodes.SUCCESS, XupmixReadback.writeCharacteristic(gatt, writer, ENABLE,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
        idle();
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
        idle();
        Assert.assertNull(XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID),
                READ_ENABLED, BluetoothGatt.GATT_SUCCESS));
        idle();
        Assert.assertArrayEquals(CONFIRMED_ENABLE, callback.received);
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertTrue(wroteDuringCompletion[0]);
        GattShadow shadow = Shadow.extract(gatt);
        Assert.assertEquals(SETTER_VOLUME_AND_QUERY_WRITES, shadow.writes);
    }

    @Test
    public void holdsHostWriteUntilQueryWriteCompletes() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = new HostCallbacks();
        GattShadow shadow = Shadow.extract(gatt);
        XupmixReadback.writeCharacteristic(gatt, writer, ENABLE, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        boolean consumed = XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS);
        idle();

        Assert.assertTrue(consumed);
        Assert.assertEquals(0, callback.writeEvents);
        Assert.assertEquals(SETTER_AND_QUERY_WRITES, shadow.writes);
        Assert.assertArrayEquals(QUERY, shadow.lastValue);
    }

    @Test
    public void completesHostWriteAfterQueryCallbackAndReadback() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = startQuery(gatt, writer);

        boolean consumed = XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS);
        idle();

        Assert.assertTrue(consumed);
        Assert.assertEquals(0, callback.writeEvents);
        XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID), READ_ENABLED,
                BluetoothGatt.GATT_SUCCESS);
        idle();
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertEquals(BluetoothGatt.GATT_SUCCESS, callback.writeStatus);
    }

    @Test
    public void confirmsReadbackForSameConnection() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = startQuery(gatt, writer);
        XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS);
        idle();

        byte[] intercepted = XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID),
                READ_ENABLED, BluetoothGatt.GATT_SUCCESS);
        idle();

        Assert.assertNull(intercepted);
        Assert.assertArrayEquals(CONFIRMED_ENABLE, callback.received);
    }

    @Test
    public void preservesPendingReadWhenUnrelatedWriteFails() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = new HostCallbacks();
        GattShadow shadow = Shadow.extract(gatt);
        XupmixReadback.writeCharacteristic(gatt, writer, ENABLE, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        shadow.result = BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY;
        XupmixReadback.writeCharacteristic(gatt, writer, VOLUME_QUERY, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        shadow.result = BluetoothStatusCodes.SUCCESS;

        XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS);
        idle();

        Assert.assertEquals(SETTER_AND_QUERY_WRITES, shadow.writes);
        Assert.assertArrayEquals(QUERY, shadow.lastValue);
    }

    @Test
    public void ignoresReadbackFromOtherConnection() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGatt other = Shadow.newInstanceOf(BluetoothGatt.class);
        HostCallbacks callback = startQuery(gatt, characteristic(WRITE_UUID));

        byte[] received = XupmixReadback.onCharacteristicReceived(callback, other, characteristic(NOTIFY_UUID),
                READ_ENABLED, BluetoothGatt.GATT_SUCCESS);

        Assert.assertSame(READ_ENABLED, received);
    }

    @Test
    public void rejectsReadbackBeforeQueryStarts() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = new HostCallbacks();
        XupmixReadback.writeCharacteristic(gatt, writer, ENABLE, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        byte[] beforeCallback = XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID),
                READ_ENABLED, BluetoothGatt.GATT_SUCCESS);
        XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS);
        byte[] beforeQuery = XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID),
                READ_ENABLED, BluetoothGatt.GATT_SUCCESS);

        Assert.assertSame(READ_ENABLED, beforeCallback);
        Assert.assertSame(READ_ENABLED, beforeQuery);
        Assert.assertNull(callback.received);
    }

    @Test
    public void preservesPendingReadAfterFailedReadCallback() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        HostCallbacks callback = startQuery(gatt, characteristic(WRITE_UUID));

        byte[] failed = XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID),
                READ_ENABLED, BluetoothGatt.GATT_FAILURE);
        XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID), READ_ENABLED,
                BluetoothGatt.GATT_SUCCESS);
        idle();

        Assert.assertSame(READ_ENABLED, failed);
        Assert.assertArrayEquals(CONFIRMED_ENABLE, callback.received);
    }

    @Test
    public void completesHostWriteWhenQueryIsRejected() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = new HostCallbacks();
        GattShadow shadow = Shadow.extract(gatt);
        XupmixReadback.writeCharacteristic(gatt, writer, ENABLE, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS);
        shadow.result = BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY;

        idle();

        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertEquals(BluetoothGatt.GATT_FAILURE, callback.writeStatus);
        Assert.assertFalse(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
    }

    @Test
    public void consumesLateQueryCallbackOnceAfterConfirmationExpires() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = startQuery(gatt, writer);
        XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID), READ_ENABLED,
                BluetoothGatt.GATT_SUCCESS);
        idle();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(READ_TIMEOUT_MS));

        boolean late = XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS);
        idle();

        Assert.assertTrue(late);
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertEquals(BluetoothGatt.GATT_SUCCESS, callback.writeStatus);
        Assert.assertFalse(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
    }

    @Test
    @Config(sdk = { 23, 33 })
    public void queriesCharacteristicsThatSupportOnlyWritesWithoutResponse() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = new BluetoothGattCharacteristic(WRITE_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, 0);
        writer.setValue(ENABLE);
        writer.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        HostCallbacks callback = new HostCallbacks();
        Assert.assertTrue(XupmixReadback.writeCharacteristic(gatt, writer));
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
        idle();
        GattShadow shadow = Shadow.extract(gatt);
        Assert.assertEquals(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE, shadow.lastWriteType);
        Assert.assertEquals(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE, writer.getWriteType());
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
        XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID), READ_ENABLED,
                BluetoothGatt.GATT_SUCCESS);
        idle();
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertEquals(BluetoothGatt.GATT_SUCCESS, callback.writeStatus);
    }

    @Test
    public void consumesLateQueryCallbackAfterDisconnect() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = startQuery(gatt, writer);

        XupmixReadback.onConnectionStateChanged(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTED);
        idle();

        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertEquals(BluetoothGatt.GATT_FAILURE, callback.writeStatus);
        if (!XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS)) {
            callback.onReadbackWrite(gatt, writer, BluetoothGatt.GATT_SUCCESS);
        }
        idle();
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertTrue(XupmixReadback.isRetiredConnection(gatt));
        Assert.assertFalse(XupmixReadback.writeWhenIdle(gatt, writer));
    }

    @Test
    public void consumesLateSetterCallbackAfterDisconnect() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = new HostCallbacks();
        XupmixReadback.registerConnection(callback, gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED);
        Assert.assertEquals(BluetoothStatusCodes.SUCCESS, XupmixReadback.writeCharacteristic(gatt, writer, ENABLE,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        XupmixReadback.onConnectionStateChanged(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTED);
        idle();
        Assert.assertEquals(1, callback.writeEvents);
        if (!XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_FAILURE)) {
            callback.onReadbackWrite(gatt, writer, BluetoothGatt.GATT_FAILURE);
        }
        idle();
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertTrue(XupmixReadback.isRetiredConnection(gatt));
        Assert.assertFalse(XupmixReadback.writeWhenIdle(gatt, writer));
    }

    @Test
    public void blocksWritesAfterLateQueryCompletesDuringTimeoutCallback() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        writer.setValue(VOLUME_QUERY);
        HostCallbacks callback = new HostCallbacks() {
            @Override
            public void onReadbackWrite(BluetoothGatt connection, BluetoothGattCharacteristic characteristic,
                    int status) {
                super.onReadbackWrite(connection, characteristic, status);
                boolean[] consumed = { false };
                Thread lateCallback = new Thread(() -> consumed[0] = XupmixReadback.onCharacteristicWrite(this,
                        connection, characteristic, BluetoothGatt.GATT_SUCCESS));
                lateCallback.start();
                try {
                    lateCallback.join(2000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(ex);
                }
                Assert.assertFalse(lateCallback.isAlive());
                Assert.assertTrue(consumed[0]);
                Assert.assertFalse(XupmixReadback.writeWhenIdle(connection, writer));
                Assert.assertTrue(XupmixReadback.hasPendingWrite(connection));
            }
        };
        Assert.assertEquals(BluetoothStatusCodes.SUCCESS, XupmixReadback.writeCharacteristic(gatt, writer, ENABLE,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
        idle();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(READ_TIMEOUT_MS));
        GattShadow shadow = Shadow.extract(gatt);
        Assert.assertEquals(1, shadow.disconnects);
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertEquals(SETTER_AND_QUERY_WRITES, shadow.writes);
        Assert.assertNull(XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID),
                READ_ENABLED, BluetoothGatt.GATT_SUCCESS));
        XupmixReadback.onConnectionStateChanged(gatt, 0, BluetoothProfile.STATE_DISCONNECTED);
        Assert.assertTrue(XupmixReadback.isRetiredConnection(gatt));
        Assert.assertFalse(XupmixReadback.writeWhenIdle(gatt, writer));
    }

    @Test
    public void holdsPendingMonitorWhileDeliveringConfirmation() throws Exception {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        java.lang.reflect.Field field = XupmixReadback.class.getDeclaredField("PENDING");
        field.setAccessible(true);
        Object monitor = field.get(null);
        HostCallbacks callback = new HostCallbacks() {
            @Override
            public void onReadbackReceived(BluetoothGatt connection, BluetoothGattCharacteristic characteristic,
                    byte[] value, int status) {
                Assert.assertTrue(Thread.holdsLock(monitor));
                super.onReadbackReceived(connection, characteristic, value, status);
            }
        };
        Assert.assertEquals(BluetoothStatusCodes.SUCCESS, XupmixReadback.writeCharacteristic(gatt, writer, ENABLE,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, 0));
        idle();
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, 0));
        XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID), READ_ENABLED, 0);
        idle();
        Assert.assertNotNull(callback.received);
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertEquals(BluetoothGatt.GATT_SUCCESS, callback.writeStatus);
    }

    @Test
    public void reservesDisconnectAfterExternalOwnerReleasesWriteCallback() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        writer.setValue(VOLUME_QUERY);
        HostCallbacks callback = new HostCallbacks();
        Assert.assertTrue(XupmixReadback.writeWhenIdle(gatt, writer));
        Assert.assertFalse(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, 0));
        XupmixReadback.disconnect(gatt);
        XupmixReadback.disconnect(gatt);
        GattShadow shadow = Shadow.extract(gatt);
        Assert.assertEquals(1, shadow.disconnects);
        Assert.assertTrue(XupmixReadback.hasPendingWrite(gatt));
        Assert.assertFalse(XupmixReadback.writeWhenIdle(gatt, writer));
        XupmixReadback.onConnectionStateChanged(gatt, 0, BluetoothProfile.STATE_DISCONNECTED);
        Assert.assertTrue(XupmixReadback.isRetiredConnection(gatt));
        Assert.assertFalse(XupmixReadback.writeWhenIdle(gatt, writer));
    }

    @Test
    public void cancelsQueuedQueryAfterExternalOwnerRequestsDisconnect() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = new HostCallbacks();
        Assert.assertEquals(BluetoothStatusCodes.SUCCESS, XupmixReadback.writeCharacteristic(gatt, writer, ENABLE,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, 0));
        XupmixReadback.disconnect(gatt);
        idle();
        GattShadow shadow = Shadow.extract(gatt);
        Assert.assertEquals(1, shadow.writes);
        Assert.assertEquals(1, shadow.disconnects);
        XupmixReadback.onConnectionStateChanged(gatt, 0, BluetoothProfile.STATE_DISCONNECTED);
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertEquals(BluetoothGatt.GATT_FAILURE, callback.writeStatus);
    }

    @Test
    public void preservesAcceptedRequestWhenOverlappingXupmixWriteIsRejected() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = startQuery(gatt, writer);
        GattShadow shadow = Shadow.extract(gatt);

        int result = XupmixReadback.writeCharacteristic(gatt, writer, DISABLE,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS);
        XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID), READ_ENABLED,
                BluetoothGatt.GATT_SUCCESS);
        idle();

        Assert.assertEquals(BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY, result);
        Assert.assertEquals(SETTER_AND_QUERY_WRITES, shadow.writes);
        Assert.assertArrayEquals(CONFIRMED_ENABLE, callback.received);
        Assert.assertEquals(1, callback.writeEvents);
    }

    @Test
    public void preservesUnrelatedWritePermissionFailure() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        GattShadow shadow = Shadow.extract(gatt);
        SecurityException failure = new SecurityException("permission rejected");
        shadow.failure = failure;

        try {
            XupmixReadback.writeCharacteristic(gatt, characteristic(WRITE_UUID), VOLUME_QUERY,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            Assert.fail("Expected the original permission failure");
        } catch (SecurityException ex) {
            Assert.assertSame(failure, ex);
        }
    }

    @Test
    @Config(sdk = 23)
    public void queriesStateThroughLegacyWrite() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = new HostCallbacks();
        GattShadow shadow = Shadow.extract(gatt);
        writer.setValue(ENABLE);
        writer.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        XupmixReadback.writeCharacteristic(gatt, writer);

        XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS);
        idle();

        Assert.assertEquals(SETTER_AND_QUERY_WRITES, shadow.writes);
        Assert.assertArrayEquals(QUERY, shadow.lastValue);
        Assert.assertArrayEquals(ENABLE, writer.getValue());
        Assert.assertEquals(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE, writer.getWriteType());
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
        XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID), READ_ENABLED,
                BluetoothGatt.GATT_SUCCESS);
        idle();
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertEquals(BluetoothGatt.GATT_SUCCESS, callback.writeStatus);
        Assert.assertArrayEquals(CONFIRMED_ENABLE, callback.received);
    }

    @Test
    public void retriesStaleStateAfterQueryWriteCompletes() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = startQuery(gatt, writer);
        GattShadow shadow = Shadow.extract(gatt);
        XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID), READ_DISABLED,
                BluetoothGatt.GATT_SUCCESS);
        idle();
        Assert.assertEquals(SETTER_AND_QUERY_WRITES, shadow.writes);
        XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS);
        idle();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(READ_RETRY_MS));
        Assert.assertEquals(SETTER_VOLUME_AND_QUERY_WRITES, shadow.writes);
        Assert.assertEquals(0, callback.writeEvents);
        XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS);
        XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID), READ_ENABLED,
                BluetoothGatt.GATT_SUCCESS);
        idle();
        Assert.assertArrayEquals(CONFIRMED_ENABLE, callback.received);
        Assert.assertEquals(1, callback.writeEvents);
    }

    @Test
    public void stopsRetryingAfterReadDeadline() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = startQuery(gatt, writer);
        GattShadow shadow = Shadow.extract(gatt);
        XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS);
        XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID), READ_DISABLED,
                BluetoothGatt.GATT_SUCCESS);
        idle();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(READ_TIMEOUT_MS));
        int writesAtDeadline = shadow.writes;
        XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS);
        XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID), READ_DISABLED,
                BluetoothGatt.GATT_SUCCESS);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(READ_TIMEOUT_MS));
        Assert.assertEquals(writesAtDeadline, shadow.writes);
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertEquals(BluetoothGatt.GATT_FAILURE, callback.writeStatus);
    }

    @Test
    public void preservesWritesForOtherModels() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        GattShadow shadow = Shadow.extract(gatt);
        shadow.deviceName = "Poseidon D60 BLE";
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        XupmixReadback.writeCharacteristic(gatt, writer, ENABLE, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        HostCallbacks callback = new HostCallbacks();
        Assert.assertFalse(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
        idle();
        Assert.assertEquals(1, shadow.writes);
    }

    @Test
    public void rejectsNewWritesUntilExpiredConnectionIsReplaced() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = startQuery(gatt, writer);
        GattShadow shadow = Shadow.extract(gatt);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(READ_TIMEOUT_MS));
        int result = XupmixReadback.writeCharacteristic(gatt, writer, VOLUME_QUERY,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        Assert.assertEquals(BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY, result);
        Assert.assertEquals(SETTER_AND_QUERY_WRITES, shadow.writes);
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
        idle();
        int accepted = XupmixReadback.writeCharacteristic(gatt, writer, VOLUME_QUERY,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        Assert.assertEquals(BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY, accepted);
        Assert.assertEquals(SETTER_AND_QUERY_WRITES, shadow.writes);
        Assert.assertFalse(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
    }

    @Test
    public void cancelsQueuedRetryWhenReadbackConfirms() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = startQuery(gatt, writer);
        GattShadow shadow = Shadow.extract(gatt);
        XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS);
        idle();
        XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID), READ_DISABLED,
                BluetoothGatt.GATT_SUCCESS);
        idle();
        XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID), READ_ENABLED,
                BluetoothGatt.GATT_SUCCESS);
        idle();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(READ_RETRY_MS));
        Assert.assertEquals(SETTER_AND_QUERY_WRITES, shadow.writes);
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertArrayEquals(CONFIRMED_ENABLE, callback.received);
    }

    @Test
    public void retriesStateAfterCompletedQueryWrite() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = startQuery(gatt, writer);
        GattShadow shadow = Shadow.extract(gatt);
        XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS);
        idle();
        XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID), READ_DISABLED,
                BluetoothGatt.GATT_SUCCESS);
        idle();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(READ_RETRY_MS));
        Assert.assertEquals(SETTER_VOLUME_AND_QUERY_WRITES, shadow.writes);
        Assert.assertEquals(0, callback.writeEvents);
        XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS);
        XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID), READ_ENABLED,
                BluetoothGatt.GATT_SUCCESS);
        idle();
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertArrayEquals(CONFIRMED_ENABLE, callback.received);
    }

    @Test
    public void completesDisableReadback() {
        assertDisableReadback(false);
    }

    @Test
    @Config(sdk = 23)
    public void completesDisableReadbackThroughLegacyWrite() {
        assertDisableReadback(true);
    }

    private void assertDisableReadback(boolean legacy) {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = new HostCallbacks();
        if (legacy) {
            writer.setValue(DISABLE);
            Assert.assertTrue(XupmixReadback.writeCharacteristic(gatt, writer));
        } else {
            Assert.assertEquals(BluetoothStatusCodes.SUCCESS, XupmixReadback.writeCharacteristic(gatt, writer, DISABLE,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        }
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
        idle();
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
        XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID), READ_DISABLED,
                BluetoothGatt.GATT_SUCCESS);
        idle();
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertEquals(BluetoothGatt.GATT_SUCCESS, callback.writeStatus);
        Assert.assertArrayEquals(CONFIRMED_DISABLE, callback.received);
    }

    @Test
    public void failsRegisteredHostWriteWhenSetterCallbackIsMissing() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = new HostCallbacks();
        XupmixReadback.registerConnection(callback, gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED);
        Assert.assertEquals(BluetoothStatusCodes.SUCCESS, XupmixReadback.writeCharacteristic(gatt, writer, ENABLE,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(READ_TIMEOUT_MS));
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertEquals(BluetoothGatt.GATT_FAILURE, callback.writeStatus);
        GattShadow shadow = Shadow.extract(gatt);
        Assert.assertEquals(1, shadow.disconnects);
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
        idle();
        Assert.assertEquals(1, callback.writeEvents);
        XupmixReadback.registerConnection(callback, gatt, 0, BluetoothProfile.STATE_DISCONNECTED);
        XupmixReadback.onConnectionStateChanged(gatt, 0, BluetoothProfile.STATE_DISCONNECTED);
        idle();
        Assert.assertEquals(1, callback.writeEvents);
    }

    @Test
    public void completeTimedOutSetterOnlyOnceAfterLateFailure() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = new HostCallbacks();
        XupmixReadback.registerConnection(callback, gatt, 0, BluetoothProfile.STATE_CONNECTED);
        Assert.assertEquals(0, XupmixReadback.writeCharacteristic(gatt, writer, ENABLE,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(READ_TIMEOUT_MS));
        Assert.assertEquals(1, callback.writeEvents);
        if (!XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_FAILURE)) {
            callback.onReadbackWrite(gatt, writer, BluetoothGatt.GATT_FAILURE);
        }
        idle();
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertEquals(BluetoothGatt.GATT_FAILURE, callback.writeStatus);
        XupmixReadback.onConnectionStateChanged(gatt, 0, BluetoothProfile.STATE_DISCONNECTED);
    }

    @Test
    public void preservesEarlierCallbackWhenSetterWriteIsRejected() throws Exception {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = new HostCallbacks();
        XupmixReadback.registerConnection(callback, gatt, 0, BluetoothProfile.STATE_CONNECTED);
        GattShadow shadow = Shadow.extract(gatt);
        shadow.result = BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY;
        boolean[] consumed = { true };
        java.util.concurrent.CountDownLatch completed = new java.util.concurrent.CountDownLatch(1);
        Thread callbackThread = new Thread(() -> {
            consumed[0] = XupmixReadback.onCharacteristicWrite(callback, gatt, writer, 0);
            completed.countDown();
        });
        shadow.beforeReturn = () -> {
            callbackThread.start();
            try {
                completed.await(100, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AssertionError(ex);
            }
        };
        Assert.assertEquals(BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY, XupmixReadback.writeCharacteristic(gatt,
                writer, ENABLE, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        Assert.assertTrue(completed.await(2, java.util.concurrent.TimeUnit.SECONDS));
        callbackThread.join();
        Assert.assertFalse(consumed[0]);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(READ_TIMEOUT_MS));
        Assert.assertEquals(0, callback.writeEvents);
        Assert.assertEquals(0, shadow.disconnects);
        XupmixReadback.registerConnection(callback, gatt, 0, BluetoothProfile.STATE_DISCONNECTED);
    }

    @Test
    @Config(sdk = 23)
    public void releaseLegacyOwnershipAfterRejectedOrExceptionalSetter() {
        for (RuntimeException failure : new RuntimeException[] { null, new SecurityException("permission"),
                new IllegalArgumentException("argument"), new IllegalStateException("state") }) {
            BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
            BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
            writer.setValue(ENABLE);
            writer.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            GattShadow shadow = Shadow.extract(gatt);
            shadow.result = BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY;
            shadow.failure = failure;
            if (failure == null) {
                Assert.assertFalse(XupmixReadback.writeCharacteristic(gatt, writer));
            } else {
                Assert.assertSame(failure, Assert.assertThrows(failure.getClass(),
                        () -> XupmixReadback.writeCharacteristic(gatt, writer)));
            }
            Assert.assertArrayEquals(ENABLE, writer.getValue());
            Assert.assertEquals(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE, writer.getWriteType());
            shadow.failure = null;
            shadow.result = BluetoothStatusCodes.SUCCESS;
            writer.setValue(VOLUME_QUERY);
            Assert.assertTrue(XupmixReadback.writeCharacteristic(gatt, writer));
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(READ_TIMEOUT_MS));
            Assert.assertEquals(0, shadow.disconnects);
            Assert.assertFalse(XupmixReadback.onCharacteristicWrite(new HostCallbacks(), gatt, writer, 0));
        }
    }

    @Test
    public void completeModernHostWriteAfterQueryFailure() {
        assertQueryFailures(false);
    }

    @Test
    @Config(sdk = 23)
    public void completeLegacyHostWriteAndRestoreCharacteristicAfterQueryFailure() {
        assertQueryFailures(true);
    }

    private void assertQueryFailures(boolean legacy) {
        for (RuntimeException failure : new RuntimeException[] { null, new SecurityException("permission"),
                new IllegalArgumentException("argument"), new IllegalStateException("state"),
                new RuntimeException("service") }) {
            BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
            BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
            writer.setValue(ENABLE);
            writer.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            if (legacy) {
                Assert.assertTrue(XupmixReadback.writeCharacteristic(gatt, writer));
            } else {
                Assert.assertEquals(BluetoothStatusCodes.SUCCESS, XupmixReadback.writeCharacteristic(gatt, writer,
                        ENABLE, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE));
            }
            GattShadow shadow = Shadow.extract(gatt);
            shadow.result = BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY;
            shadow.failure = failure;
            HostCallbacks callback = new HostCallbacks();
            Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, 0));
            idle();
            Assert.assertEquals(1, callback.writeEvents);
            Assert.assertEquals(BluetoothGatt.GATT_FAILURE, callback.writeStatus);
            Assert.assertArrayEquals(ENABLE, writer.getValue());
            Assert.assertEquals(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE, writer.getWriteType());
            shadow.failure = null;
            shadow.result = BluetoothStatusCodes.SUCCESS;
            writer.setValue(VOLUME_QUERY);
            Assert.assertTrue(XupmixReadback.writeCharacteristic(gatt, writer));
            Assert.assertFalse(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, 0));
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(READ_TIMEOUT_MS));
            Assert.assertEquals(1, callback.writeEvents);
            Assert.assertEquals(0, shadow.disconnects);
        }
    }

    @Test
    public void failsOnceAfterAcceptedQueryReportsFailure() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = startQuery(gatt, writer);
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_FAILURE));
        idle();
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertEquals(BluetoothGatt.GATT_FAILURE, callback.writeStatus);
        Assert.assertFalse(XupmixReadback.hasPendingWrite(gatt));
        XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID), READ_ENABLED, 0);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(READ_TIMEOUT_MS));
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertNull(callback.received);
    }

    @Test
    public void serializesQueryFailureAfterReadbackDelivery() throws Exception {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        Thread[] failure = { null };
        boolean[] consumed = { false };
        HostCallbacks callback = new HostCallbacks() {
            @Override
            public void onReadbackReceived(BluetoothGatt connection, BluetoothGattCharacteristic characteristic,
                    byte[] value, int status) {
                super.onReadbackReceived(connection, characteristic, value, status);
                failure[0] = new Thread(() -> consumed[0] = XupmixReadback.onCharacteristicWrite(this, connection,
                        writer, BluetoothGatt.GATT_FAILURE));
                failure[0].start();
                long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(2);
                while (failure[0].getState() != Thread.State.BLOCKED && failure[0].isAlive()
                        && System.nanoTime() < deadline) {
                    Thread.yield();
                }
                Assert.assertEquals(Thread.State.BLOCKED, failure[0].getState());
            }
        };
        Assert.assertEquals(BluetoothStatusCodes.SUCCESS, XupmixReadback.writeCharacteristic(gatt, writer, ENABLE,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, 0));
        idle();
        Assert.assertNull(
                XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID), READ_ENABLED, 0));
        idle();
        failure[0].join(2000);
        Assert.assertFalse(failure[0].isAlive());
        Assert.assertTrue(consumed[0]);
        idle();
        Assert.assertArrayEquals(CONFIRMED_ENABLE, callback.received);
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertEquals(BluetoothGatt.GATT_FAILURE, callback.writeStatus);
        Assert.assertFalse(XupmixReadback.hasPendingWrite(gatt));
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(READ_TIMEOUT_MS));
        Assert.assertEquals(1, callback.writeEvents);
    }

    @Test
    public void replaceDisconnectedGattBeforeAcceptingWritesAndConsumeLateCallbacks() {
        for (int callbacksDelivered : new int[] { 0, 1, 2 }) {
            BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
            BluetoothGatt replacement = Shadow.newInstanceOf(BluetoothGatt.class);
            BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
            HostCallbacks callback = new HostCallbacks();
            XupmixReadback.registerConnection(callback, gatt, 0, BluetoothProfile.STATE_CONNECTED);
            Assert.assertEquals(BluetoothStatusCodes.SUCCESS, XupmixReadback.writeCharacteristic(gatt, writer, ENABLE,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
            if (callbacksDelivered > 0) {
                Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, 0));
                idle();
            }
            if (callbacksDelivered > 1) {
                Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, 0));
            }
            XupmixReadback.onConnectionStateChanged(gatt, 0, BluetoothProfile.STATE_DISCONNECTED);
            Assert.assertTrue(XupmixReadback.isRetiredConnection(gatt));
            XupmixReadback.onConnectionStateChanged(gatt, 0, BluetoothProfile.STATE_CONNECTED);
            Assert.assertEquals(BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY, XupmixReadback
                .writeCharacteristic(gatt, writer, ENABLE, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
            XupmixReadback.replaceRetiredConnection(callback, gatt);
            XupmixReadback.replaceRetiredConnection(callback, gatt);
            Assert.assertEquals(1, callback.reconnects);
            GattShadow shadow = Shadow.extract(gatt);
            Assert.assertEquals(1, shadow.closes);
            XupmixReadback.registerConnection(callback, replacement, 0, BluetoothProfile.STATE_CONNECTED);
            Assert.assertEquals(BluetoothStatusCodes.SUCCESS, XupmixReadback.writeCharacteristic(replacement, writer,
                    ENABLE, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
            idle();
            Assert.assertEquals(1, callback.writeEvents);
            Assert.assertEquals(BluetoothGatt.GATT_FAILURE, callback.writeStatus);
            Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_FAILURE));
            Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
            Assert.assertNull(XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID),
                    READ_DISABLED, 0));
            Assert.assertTrue(XupmixReadback.hasPendingWrite(replacement));
            Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, replacement, writer, 0));
            idle();
            Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, replacement, writer, 0));
            Assert.assertNull(XupmixReadback.onCharacteristicReceived(callback, replacement,
                    characteristic(NOTIFY_UUID), READ_ENABLED, 0));
            idle();
            Assert.assertEquals(2, callback.writeEvents);
            Assert.assertEquals(BluetoothGatt.GATT_SUCCESS, callback.writeStatus);
            Assert.assertFalse(XupmixReadback.hasPendingWrite(replacement));
        }
    }

    @Test
    public void completesOldWriteBeforeDisconnectReturnsAndReplacementStarts() {
        BluetoothGatt old = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGatt fresh = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = new HostCallbacks();
        XupmixReadback.registerConnection(callback, old, 0, BluetoothProfile.STATE_CONNECTED);
        Assert.assertEquals(BluetoothStatusCodes.SUCCESS, XupmixReadback.writeCharacteristic(old, writer, ENABLE,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        XupmixReadback.onConnectionStateChanged(old, 0, BluetoothProfile.STATE_DISCONNECTED);
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertEquals(BluetoothGatt.GATT_FAILURE, callback.writeStatus);
        XupmixReadback.replaceRetiredConnection(callback, old);
        XupmixReadback.registerConnection(callback, fresh, 0, BluetoothProfile.STATE_CONNECTED);
        Assert.assertEquals(BluetoothStatusCodes.SUCCESS, XupmixReadback.writeCharacteristic(fresh, writer, ENABLE,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        idle();
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertTrue(XupmixReadback.hasPendingWrite(fresh));
    }

    @Test
    public void replaceDisconnectedGattWithUntrackedWriteWithoutAffectingOtherConnections() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGatt replacement = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGatt independent = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        HostCallbacks callback = new HostCallbacks();
        writer.setValue(VOLUME_QUERY);
        Assert.assertTrue(XupmixReadback.writeWhenIdle(gatt, writer));
        Assert.assertTrue(XupmixReadback.writeWhenIdle(independent, writer));
        XupmixReadback.onConnectionStateChanged(gatt, 0, BluetoothProfile.STATE_DISCONNECTED);
        XupmixReadback.replaceRetiredConnection(callback, gatt);
        Assert.assertEquals(1, callback.reconnects);
        Assert.assertTrue(XupmixReadback.writeWhenIdle(replacement, writer));
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_FAILURE));
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
        Assert.assertFalse(
                XupmixReadback.onCharacteristicWrite(callback, replacement, writer, BluetoothGatt.GATT_SUCCESS));
        Assert.assertFalse(
                XupmixReadback.onCharacteristicWrite(callback, independent, writer, BluetoothGatt.GATT_SUCCESS));
        Assert.assertFalse(XupmixReadback.hasPendingWrite(independent));
    }

    @Test
    public void preserveGattWithoutOutstandingWriteOnDisconnect() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        HostCallbacks callback = new HostCallbacks();
        XupmixReadback.onConnectionStateChanged(gatt, 0, BluetoothProfile.STATE_DISCONNECTED);
        XupmixReadback.replaceRetiredConnection(callback, gatt);
        Assert.assertFalse(XupmixReadback.isRetiredConnection(gatt));
        Assert.assertEquals(0, callback.reconnects);
        GattShadow shadow = Shadow.extract(gatt);
        Assert.assertEquals(0, shadow.closes);
    }

    @Test
    public void retryConnectionReplacementAfterTransientCloseFailure() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        writer.setValue(VOLUME_QUERY);
        Assert.assertTrue(XupmixReadback.writeWhenIdle(gatt, writer));
        XupmixReadback.onConnectionStateChanged(gatt, 0, BluetoothProfile.STATE_DISCONNECTED);
        GattShadow shadow = Shadow.extract(gatt);
        shadow.closeFailure = new SecurityException("permission");
        HostCallbacks callback = new HostCallbacks();
        XupmixReadback.replaceRetiredConnection(callback, gatt);
        Assert.assertEquals(0, callback.reconnects);
        shadow.closeFailure = null;
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        Assert.assertEquals(1, callback.reconnects);
        Assert.assertEquals(2, shadow.closes);
        XupmixReadback.replaceRetiredConnection(callback, gatt);
        Assert.assertEquals(1, callback.reconnects);
    }

    @Test
    public void delayAutomaticRetriesAndAllowEarlierExplicitReplacement() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        writer.setValue(VOLUME_QUERY);
        Assert.assertTrue(XupmixReadback.writeWhenIdle(gatt, writer));
        XupmixReadback.onConnectionStateChanged(gatt, 0, BluetoothProfile.STATE_DISCONNECTED);
        GattShadow shadow = Shadow.extract(gatt);
        shadow.closeFailure = new SecurityException("permission");
        HostCallbacks callback = new HostCallbacks();
        XupmixReadback.replaceRetiredConnection(callback, gatt);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4));
        Assert.assertEquals(3, shadow.closes);
        Assert.assertEquals(0, callback.reconnects);
        shadow.closeFailure = null;
        XupmixReadback.replaceRetiredConnection(callback, gatt);
        Assert.assertEquals(1, callback.reconnects);
    }

    @Test
    public void retryConnectionReplacementAfterTransientFactoryFailure() {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        writer.setValue(VOLUME_QUERY);
        Assert.assertTrue(XupmixReadback.writeWhenIdle(gatt, writer));
        XupmixReadback.onConnectionStateChanged(gatt, 0, BluetoothProfile.STATE_DISCONNECTED);
        int[] attempts = { 0 };
        HostCallbacks callback = new HostCallbacks() {
            @Override
            public void onReadbackReconnect(BluetoothGatt connection) {
                if (++attempts[0] == 1) {
                    throw new IllegalStateException("service");
                }
                super.onReadbackReconnect(connection);
            }
        };
        XupmixReadback.replaceRetiredConnection(callback, gatt);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        Assert.assertEquals(2, attempts[0]);
        XupmixReadback.replaceRetiredConnection(callback, gatt);
        Assert.assertEquals(2, attempts[0]);
    }

    @Test
    public void releaseModernOwnershipAfterPlatformException() {
        for (RuntimeException failure : new RuntimeException[] { new SecurityException("permission"),
                new IllegalArgumentException("argument"), new IllegalStateException("state") }) {
            BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
            BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
            GattShadow shadow = Shadow.extract(gatt);
            shadow.failure = failure;
            Assert.assertSame(failure, Assert.assertThrows(failure.getClass(), () -> XupmixReadback
                .writeCharacteristic(gatt, writer, ENABLE, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)));
            shadow.failure = null;
            Assert.assertEquals(BluetoothStatusCodes.SUCCESS, XupmixReadback.writeCharacteristic(gatt, writer,
                    VOLUME_QUERY, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(READ_TIMEOUT_MS));
            Assert.assertEquals(0, shadow.disconnects);
            Assert.assertFalse(XupmixReadback.onCharacteristicWrite(new HostCallbacks(), gatt, writer, 0));
        }
    }

    @Test
    public void preserveEarlierUntrackedCallbackBeforeAcceptedSetter() {
        assertEarlierUntrackedCallback(false, false);
    }

    @Test
    @Config(sdk = 23)
    public void preserveEarlierLegacyCallbackBeforeAcceptedSetter() {
        assertEarlierUntrackedCallback(true, false);
    }

    @Test
    @Config(sdk = 23)
    public void preserveEarlierDirectCallbackBeforeAcceptedSetter() {
        assertEarlierUntrackedCallback(true, true);
    }

    private void assertEarlierUntrackedCallback(boolean legacy, boolean direct) {
        BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        BluetoothGattCharacteristic writer = characteristic(WRITE_UUID);
        writer.setValue(VOLUME_QUERY);
        if (direct) {
            Assert.assertTrue(XupmixReadback.writeWhenIdle(gatt, writer));
        } else if (legacy) {
            Assert.assertTrue(XupmixReadback.writeCharacteristic(gatt, writer));
        } else {
            Assert.assertEquals(0, XupmixReadback.writeCharacteristic(gatt, writer, VOLUME_QUERY,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        }
        writer.setValue(ENABLE);
        if (legacy) {
            Assert.assertTrue(XupmixReadback.writeCharacteristic(gatt, writer));
        } else {
            Assert.assertEquals(0, XupmixReadback.writeCharacteristic(gatt, writer, ENABLE,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        }
        HostCallbacks callback = new HostCallbacks();
        Assert.assertFalse(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
        idle();
        GattShadow shadow = Shadow.extract(gatt);
        Assert.assertEquals(2, shadow.writes);
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
        idle();
        Assert.assertEquals(3, shadow.writes);
        Assert.assertArrayEquals(QUERY, shadow.lastValue);
        Assert.assertTrue(XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS));
        XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic(NOTIFY_UUID), READ_ENABLED, 0);
        idle();
        Assert.assertEquals(1, callback.writeEvents);
        Assert.assertEquals(BluetoothGatt.GATT_SUCCESS, callback.writeStatus);
    }

    private static HostCallbacks startQuery(BluetoothGatt gatt, BluetoothGattCharacteristic writer) {
        HostCallbacks callback = new HostCallbacks();
        XupmixReadback.writeCharacteristic(gatt, writer, ENABLE, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        XupmixReadback.onCharacteristicWrite(callback, gatt, writer, BluetoothGatt.GATT_SUCCESS);
        idle();
        return callback;
    }

    private static void idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    private static BluetoothGattCharacteristic characteristic(UUID uuid) {
        return new BluetoothGattCharacteristic(uuid, 0, 0);
    }

    private static class HostCallbacks extends BluetoothGattCallback implements ReadbackCallbacks {

        private int reconnects;

        @Override
        public void onReadbackReconnect(BluetoothGatt gatt) {
            this.reconnects++;
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int state) {
            if (!XupmixReadback.isRetiredConnection(gatt)) {
                XupmixReadback.registerConnection(this, gatt, status, state);
                XupmixReadback.onConnectionStateChanged(gatt, status, state);
                XupmixReadback.replaceRetiredConnection(this, gatt);
            }
        }

        private int writeEvents;

        private int writeStatus;

        private byte[] received;

        @Override
        public void onReadbackWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Assert.assertFalse(XupmixReadback.onCharacteristicWrite(this, gatt, characteristic, status));
            this.writeEvents++;
            this.writeStatus = status;
        }

        @Override
        public void onReadbackReceived(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value,
                int status) {
            this.received = XupmixReadback.onCharacteristicReceived(this, gatt, characteristic, value, status);
        }

    }

    @Implements(BluetoothGatt.class)
    public static class GattShadow {

        private int writes;

        private int disconnects;

        private int closes;

        private RuntimeException closeFailure;

        @Implementation
        protected void close() {
            this.closes++;
            if (this.closeFailure != null) {
                throw this.closeFailure;
            }
        }

        private SecurityException disconnectFailure;

        @Implementation
        protected void disconnect() {
            this.disconnects++;
            if (this.disconnectFailure != null) {
                throw this.disconnectFailure;
            }
        }

        private String deviceName = "Poseidon D80 BLE";

        @Implementation
        protected BluetoothDevice getDevice() {
            BluetoothDevice device = ShadowBluetoothDevice.newInstance("00:00:00:00:00:00");
            Shadows.shadowOf(device).setName(this.deviceName);
            return device;
        }

        private byte[] lastValue;

        private int lastWriteType;

        private int result = BluetoothStatusCodes.SUCCESS;

        private RuntimeException failure;

        private Runnable beforeReturn;

        @Implementation(minSdk = 33)
        protected int writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value, int writeType) {
            if (this.failure != null) {
                throw this.failure;
            }
            this.writes++;
            this.lastValue = value.clone();
            this.lastWriteType = writeType;
            if (this.beforeReturn != null) {
                this.beforeReturn.run();
            }
            return this.result;
        }

        @Implementation
        protected boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
            if (this.failure != null) {
                throw this.failure;
            }
            this.writes++;
            this.lastValue = characteristic.getValue().clone();
            this.lastWriteType = characteristic.getWriteType();
            return this.result == BluetoothStatusCodes.SUCCESS;
        }

    }

}
