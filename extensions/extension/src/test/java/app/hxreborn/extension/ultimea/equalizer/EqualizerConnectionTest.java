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
package app.hxreborn.extension.ultimea.equalizer;

import java.time.Duration;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Looper;

import org.junit.After;
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
@Config(sdk = 33, shadows = EqualizerConnectionTest.GattShadow.class)
public class EqualizerConnectionTest {

    private static final UUID WRITE = UUID.fromString("27758d11-bf3a-4ac6-bee5-6259ccb7c9b7");

    private static final UUID NOTIFY = UUID.fromString("27758d22-bf3a-4ac6-bee5-6259ccb7c9b7");

    private final BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);

    private final BluetoothGattCharacteristic writer = new BluetoothGattCharacteristic(WRITE, 0, 0);

    private EqualizerCurve result;

    private int completions;

    @After
    public void disconnect() {
        EqualizerConnection.disconnected(this.gatt, 0, 0);
        idle();
    }

    @Test
    public void waitsForQueryWriteAfterReply() {
        connect();
        EqualizerConnection.read(this::completed);
        reply();
        Assert.assertEquals(0, this.completions);
        Assert.assertTrue(EqualizerConnection.written(this.gatt, this.writer, 0));
        idle();
        Assert.assertEquals(1, this.completions);
        Assert.assertEquals(new EqualizerCurve(8, new int[10]), this.result);
    }

    @Test
    public void waitsForReplyAfterQueryWrite() {
        connect();
        EqualizerConnection.read(this::completed);
        EqualizerConnection.written(this.gatt, this.writer, 0);
        idle();
        Assert.assertEquals(0, this.completions);
        reply();
        Assert.assertEquals(1, this.completions);
    }

    @Test
    public void retainsTimedOutWriteUntilCallback() {
        connect();
        EqualizerConnection.read(this::completed);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(3001));
        Assert.assertEquals(1, this.completions);
        Assert.assertNull(this.result);
        Assert.assertFalse(EqualizerConnection.writeCharacteristic(this.gatt, this.writer));
        Assert.assertTrue(EqualizerConnection.written(this.gatt, this.writer, 0));
        idle();
        Assert.assertTrue(EqualizerConnection.writeCharacteristic(this.gatt, this.writer));
        Assert.assertEquals(1, this.completions);
    }

    @Test
    public void rejectsOtherModels() {
        GattShadow shadow = Shadow.extract(this.gatt);
        shadow.name = "Other speaker";
        connect();
        EqualizerConnection.read(this::completed);
        Assert.assertEquals(1, this.completions);
        Assert.assertNull(this.result);
        Assert.assertEquals(1, shadow.writes);
    }

    @Test
    public void queriesAfterSetterWriteCompletes() {
        connect();
        GattShadow shadow = Shadow.extract(this.gatt);
        EqualizerConnection.apply(new EqualizerCurve(7, new int[10]), this::completed);
        Assert.assertEquals(2, shadow.writes);
        Assert.assertEquals(2, shadow.lastValue[3]);
        EqualizerConnection.written(this.gatt, this.writer, 0);
        idle();
        Assert.assertEquals(3, shadow.writes);
        Assert.assertEquals(1, shadow.lastValue[3]);
        Assert.assertEquals(8, shadow.lastValue[4]);
        Assert.assertEquals(0, this.completions);
        EqualizerConnection.written(this.gatt, this.writer, 0);
        idle();
        reply(new EqualizerCurve(7, new int[10]));
        Assert.assertEquals(1, this.completions);
    }

    @Test
    public void retriesPreviousCurveAfterQueryWrite() {
        // given
        connect();
        EqualizerCurve target = new EqualizerCurve(7, new int[10]).withGain(9, -14);
        EqualizerConnection.apply(target, this::completed);
        EqualizerConnection.written(this.gatt, this.writer, 0);
        idle();
        EqualizerConnection.written(this.gatt, this.writer, 0);
        idle();

        // when
        reply(target.withGain(9, -13));
        Assert.assertEquals(0, this.completions);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50));
        GattShadow shadow = Shadow.extract(this.gatt);
        Assert.assertEquals(1, shadow.lastValue[3]);
        Assert.assertEquals(8, shadow.lastValue[4]);
        EqualizerConnection.written(this.gatt, this.writer, 0);
        idle();
        reply(target);

        // then
        Assert.assertEquals(1, this.completions);
        Assert.assertEquals(target, this.result);
    }

    @Test
    public void retriesPreviousCurveReceivedBeforeWriteCallback() {
        // given
        connect();
        EqualizerCurve target = new EqualizerCurve(7, new int[10]).withGain(9, -14);
        EqualizerConnection.apply(target, this::completed);
        EqualizerConnection.written(this.gatt, this.writer, 0);
        idle();

        // when
        reply(target.withGain(9, -13));
        EqualizerConnection.written(this.gatt, this.writer, 0);
        idle();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50));
        reply(target);
        Assert.assertEquals(0, this.completions);
        EqualizerConnection.written(this.gatt, this.writer, 0);
        idle();

        // then
        Assert.assertEquals(1, this.completions);
        Assert.assertEquals(target, this.result);
    }

    private void connect() {
        this.writer.setValue(new byte[] { (byte) 0xaa, 0, 0, 1, 7, (byte) 0xb2 });
        Assert.assertTrue(EqualizerConnection.writeCharacteristic(this.gatt, this.writer));
        Assert.assertFalse(EqualizerConnection.onCharacteristicWrite(null, this.gatt, this.writer, 0));
    }

    private void reply() {
        reply(new EqualizerCurve(8, new int[10]));
    }

    private void reply(EqualizerCurve curve) {
        byte[] payload = curve.payload();
        byte[] frame = new byte[payload.length + 6];
        frame[0] = (byte) 0xbb;
        frame[1] = (byte) payload.length;
        frame[3] = 1;
        frame[4] = 8;
        System.arraycopy(payload, 0, frame, 5, payload.length);
        int checksum = 0xaa + 1 + 8;
        for (byte value : payload) {
            checksum += value & 255;
        }
        frame[frame.length - 1] = (byte) checksum;
        EqualizerConnection.received(this.gatt, new BluetoothGattCharacteristic(NOTIFY, 0, 0), frame, 0);
        idle();
    }

    private void completed(EqualizerCurve curve) {
        this.result = curve;
        this.completions++;
    }

    private static void idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    @Implements(BluetoothGatt.class)
    public static class GattShadow {

        private String name = "Poseidon D80 BLE";

        private int writes;

        private byte[] lastValue;

        @Implementation
        protected BluetoothDevice getDevice() {
            BluetoothDevice device = ShadowBluetoothDevice.newInstance("00:00:00:00:00:00");
            Shadows.shadowOf(device).setName(this.name);
            return device;
        }

        @Implementation
        protected boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
            this.writes++;
            this.lastValue = characteristic.getValue().clone();
            return true;
        }

    }

}
