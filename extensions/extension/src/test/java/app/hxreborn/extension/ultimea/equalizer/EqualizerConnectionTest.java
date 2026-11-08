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
import android.bluetooth.BluetoothProfile;
import android.os.Looper;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
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

    private BluetoothGatt gatt = Shadow.newInstanceOf(BluetoothGatt.class);

    private final BluetoothGattCharacteristic writer = new BluetoothGattCharacteristic(WRITE, 0, 0);

    private EqualizerCurve result;

    private int completions;

    @After
    public void disconnect() {
        EqualizerConnection.disconnected(this.gatt, 0, 0);
        idle();
    }

    @Test
    public void readsCurveAndXupmixAsOneState() {
        connect();
        SoundState[] result = { null };
        EqualizerConnection.readState((state) -> {
            result[0] = state;
            this.completions++;
        });
        finishWrite(0);
        reply();
        GattShadow shadow = Shadow.extract(this.gatt);
        Assert.assertEquals(24, shadow.lastValue[4]);
        Assert.assertEquals(0, this.completions);
        finishWrite(0);
        xupmixReply(1);
        Assert.assertEquals(new SoundState(new EqualizerCurve(8, new int[10]), 1), result[0]);
        Assert.assertEquals(1, this.completions);
    }

    @Test
    public void appliesSameCurveXupmixChangeAndReadsFinalState() {
        connect();
        SoundState before = new SoundState(new EqualizerCurve(8, new int[10]), 0);
        SoundState target = before.withXupmix(1);
        SoundState[] result = { null };
        EqualizerConnection.applyState(before, target, (state) -> {
            result[0] = state;
            this.completions++;
        });
        GattShadow shadow = Shadow.extract(this.gatt);
        Assert.assertArrayEquals(new byte[] { (byte) 0xaa, 1, 0, 2, 22, 1, (byte) 0xc3 }, shadow.lastValue);
        finishWrite(0);
        Assert.assertEquals(24, shadow.lastValue[4]);
        finishWrite(0);
        xupmixReply(1);
        Assert.assertEquals(8, shadow.lastValue[4]);
        Assert.assertEquals(0, this.completions);
        finishWrite(0);
        reply();
        Assert.assertEquals(24, shadow.lastValue[4]);
        finishWrite(0);
        Assert.assertEquals(0, this.completions);
        xupmixReply(1);
        Assert.assertEquals(target, result[0]);
        Assert.assertEquals(1, this.completions);
        Assert.assertEquals(5, shadow.writes);
    }

    @Test
    public void skipsUnchangedOrUnknownXupmixSetter() {
        for (int xupmix : new int[] { 0, -1 }) {
            connect();
            SoundState before = new SoundState(new EqualizerCurve(8, new int[10]), 0);
            SoundState[] result = { null };
            EqualizerConnection.applyState(before, before.withXupmix(xupmix), (state) -> result[0] = state);
            GattShadow shadow = Shadow.extract(this.gatt);
            Assert.assertEquals(8, shadow.lastValue[4]);
            finishWrite(0);
            reply();
            Assert.assertEquals(24, shadow.lastValue[4]);
            finishWrite(0);
            xupmixReply(0);
            Assert.assertEquals(before, result[0]);
            EqualizerConnection.disconnected(this.gatt, 0, 0);
            idle();
        }
    }

    @Test
    public void failsCompoundStateWhenEitherReadComponentFails() {
        for (boolean failXupmix : new boolean[] { false, true }) {
            this.gatt = Shadow.newInstanceOf(BluetoothGatt.class);
            connect();
            int[] completed = { 0 };
            EqualizerConnection.readState((state) -> {
                Assert.assertNull(state);
                completed[0]++;
            });
            if (failXupmix) {
                finishWrite(0);
                reply();
            }
            finishWrite(BluetoothGatt.GATT_FAILURE);
            Assert.assertEquals(1, completed[0]);
        }
    }

    @Test
    public void failsCompoundApplyAtEachComponent() {
        for (int failure = 0; failure < 3; failure++) {
            connect();
            SoundState before = new SoundState(new EqualizerCurve(8, new int[10]), 0);
            SoundState target = (failure == 0) ? before.withCurve(before.curve.withGain(0, 1)) : before.withXupmix(1);
            int[] completed = { 0 };
            EqualizerConnection.applyState(before, target, (state) -> {
                Assert.assertNull(state);
                completed[0]++;
            });
            if (failure == 2) {
                finishWrite(0);
                finishWrite(0);
                xupmixReply(1);
            }
            finishWrite(BluetoothGatt.GATT_FAILURE);
            Assert.assertEquals(1, completed[0]);
        }
    }

    @Test
    public void holdsConnectionMonitorWhileReleasingTimedOutTransaction() throws Exception {
        connect();
        EqualizerConnection.read(this::completed);
        java.lang.reflect.Field field = EqualizerConnection.class.getDeclaredField("pending");
        field.setAccessible(true);
        Object transaction = field.get(null);
        java.lang.reflect.Method finish = transaction.getClass().getDeclaredMethod("finish", byte[].class);
        finish.setAccessible(true);
        java.util.concurrent.CountDownLatch started = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        Thread timeout = new Thread(() -> {
            started.countDown();
            try {
                finish.invoke(transaction, (Object) null);
            } catch (ReflectiveOperationException ex) {
                failure.set(ex);
            }
        });
        try {
            synchronized (EqualizerConnection.class) {
                timeout.start();
                Assert.assertTrue(started.await(2, java.util.concurrent.TimeUnit.SECONDS));
                long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(2);
                while (timeout.getState() != Thread.State.BLOCKED && timeout.isAlive()
                        && System.nanoTime() < deadline) {
                    Thread.yield();
                }
                Assert.assertEquals(Thread.State.BLOCKED, timeout.getState());
                Assert.assertSame(transaction, field.get(null));
            }
        } finally {
            timeout.join(2000);
        }
        Assert.assertFalse(timeout.isAlive());
        Assert.assertNull(failure.get());
        Assert.assertEquals(1, this.completions);
        GattShadow old = Shadow.extract(this.gatt);
        Assert.assertEquals(1, old.disconnects);
        BluetoothGatt replacement = Shadow.newInstanceOf(BluetoothGatt.class);
        Assert.assertTrue(EqualizerConnection.writeCharacteristic(replacement, this.writer));
        GattShadow fresh = Shadow.extract(replacement);
        Assert.assertEquals(0, fresh.disconnects);
        EqualizerConnection.disconnected(replacement, 0, 0);
        idle();
    }

    @Test
    public void holdsConnectionMonitorWhileScheduledRetryChecksConnection() throws Exception {
        connect();
        Assert.assertTrue(EqualizerConnection.writeCharacteristic(this.gatt, this.writer));
        EqualizerConnection.read(this::completed);
        Assert.assertFalse(EqualizerConnection.onCharacteristicWrite(null, this.gatt, this.writer, 0));
        java.lang.reflect.Field pending = EqualizerConnection.class.getDeclaredField("pending");
        pending.setAccessible(true);
        Object transaction = pending.get(null);
        java.lang.reflect.Field retryField = transaction.getClass().getDeclaredField("retry");
        retryField.setAccessible(true);
        Runnable retry = (Runnable) retryField.get(transaction);
        java.util.concurrent.CountDownLatch started = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        Thread sending = new Thread(() -> {
            started.countDown();
            try {
                retry.run();
            } catch (RuntimeException ex) {
                failure.set(ex);
            }
        });
        GattShadow shadow = Shadow.extract(this.gatt);
        int writes = shadow.writes;
        try {
            synchronized (EqualizerConnection.class) {
                sending.start();
                Assert.assertTrue(started.await(2, java.util.concurrent.TimeUnit.SECONDS));
                long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(2);
                while (sending.getState() != Thread.State.BLOCKED && sending.isAlive()
                        && System.nanoTime() < deadline) {
                    Thread.yield();
                }
                Assert.assertEquals(Thread.State.BLOCKED, sending.getState());
                EqualizerConnection.disconnected(this.gatt, 0, 0);
            }
        } finally {
            sending.join(2000);
        }
        Assert.assertFalse(sending.isAlive());
        Assert.assertNull(failure.get());
        idle();
        Assert.assertEquals(writes, shadow.writes);
        Assert.assertEquals(1, this.completions);
        Assert.assertNull(this.result);
    }

    @Test
    public void holdsConnectionMonitorThroughSuccessfulReadContinuation() {
        connect();
        boolean[] continued = { false };
        EqualizerConnection.read((curve) -> {
            Assert.assertTrue(Thread.holdsLock(EqualizerConnection.class));
            Assert.assertNotNull(curve);
            continued[0] = true;
            EqualizerConnection.read(this::completed);
        });
        finishWrite(0);
        reply();
        Assert.assertTrue(continued[0]);
        Assert.assertEquals(0, this.completions);
        finishWrite(0);
        reply();
        Assert.assertEquals(1, this.completions);
    }

    @Test
    public void appliesCurveAndXupmixThroughPublicCallbacks() {
        connect();
        app.hxreborn.extension.ultimea.controls.ReadbackCallbacks callbacks = new app.hxreborn.extension.ultimea.controls.ReadbackCallbacks() {
            @Override
            public void onReadbackReconnect(BluetoothGatt gatt) {
                Assert.fail("Unexpected reconnect during compound apply");
            }

            @Override
            public void onReadbackWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Assert.fail("Internal write reached host callback");
            }

            @Override
            public void onReadbackReceived(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value,
                    int status) {
                EqualizerConnection.onCharacteristicReceived(this, gatt, characteristic, value, status);
            }
        };
        SoundState before = new SoundState(new EqualizerCurve(8, new int[10]), 0);
        SoundState target = before.withCurve(before.curve.withGain(4, 1)).withXupmix(1);
        SoundState[] result = { null };
        EqualizerConnection.applyState(before, target, (state) -> {
            result[0] = state;
            this.completions++;
        });
        BluetoothGattCharacteristic notify = new BluetoothGattCharacteristic(NOTIFY, 0, 0);
        GattShadow shadow = Shadow.extract(this.gatt);
        for (int operation : new int[] { 4, 8, 22, 24, 8, 24 }) {
            Assert.assertEquals(operation, shadow.lastValue[4]);
            Assert.assertEquals(0, this.completions);
            Assert.assertTrue(EqualizerConnection.onCharacteristicWrite(callbacks, this.gatt, this.writer, 0));
            idle();
            if (operation == 8 || operation == 24) {
                byte[] frame = (operation == 8) ? replyFrame(target.curve)
                        : new byte[] { (byte) 0xbb, 1, 0, 1, 24, 1, (byte) 0xc4 };
                Assert.assertArrayEquals(frame,
                        EqualizerConnection.onCharacteristicReceived(callbacks, this.gatt, notify, frame, 0));
                idle();
            }
        }
        Assert.assertEquals(target, result[0]);
        Assert.assertEquals(1, this.completions);
        Assert.assertEquals(7, shadow.writes);
    }

    @Test
    @Config(shadows = SummaryMonitorShadow.class)
    public void holdsConnectionMonitorDuringNotificationPublication() {
        connect();
        reply();
        Assert.assertTrue(SummaryMonitorShadow.published);
    }

    @Test
    public void refreshesCharacteristicAfterAcceptedSameGattWrite() {
        connect();
        BluetoothGattCharacteristic fresh = new BluetoothGattCharacteristic(WRITE, 8, 16);
        org.robolectric.util.ReflectionHelpers.setField(fresh, "mInstance", 77);
        android.bluetooth.BluetoothGattService service = new android.bluetooth.BluetoothGattService(UUID.randomUUID(),
                0);
        service.addCharacteristic(fresh);
        fresh.setValue(new byte[] { (byte) 0xaa, 0, 0, 1, 7, (byte) 0xb2 });
        Assert.assertTrue(EqualizerConnection.writeCharacteristic(this.gatt, fresh));
        Assert.assertFalse(EqualizerConnection.onCharacteristicWrite(null, this.gatt, fresh, 0));
        EqualizerConnection.read(this::completed);
        GattShadow shadow = Shadow.extract(this.gatt);
        Assert.assertEquals(77, shadow.lastCharacteristic.getInstanceId());
        Assert.assertSame(service, shadow.lastCharacteristic.getService());
        finishWrite(0);
        reply();
        Assert.assertEquals(1, this.completions);
    }

    @Test
    public void isolatesQueryBytesFromHostCharacteristicMutation() {
        connect();
        GattShadow shadow = Shadow.extract(this.gatt);
        byte[] host = { (byte) 0xaa, 0, 0, 1, 7, (byte) 0xb2 };
        shadow.beforeReadValue = () -> this.writer.setValue(host);
        EqualizerConnection.read(this::completed);
        Assert.assertNotSame(this.writer, shadow.lastCharacteristic);
        Assert.assertArrayEquals(new byte[] { (byte) 0xaa, 0, 0, 1, 8, (byte) 0xb3 }, shadow.lastValue);
        Assert.assertArrayEquals(host, this.writer.getValue());
        finishWrite(0);
        reply();
        Assert.assertEquals(1, this.completions);
    }

    @Test
    public void waitsForNativeQueryResponseBeforeStartingOwnedRead() {
        connect();
        this.writer.setValue(new byte[] { (byte) 0xaa, 0, 0, 1, 8, (byte) 0xb3 });
        Assert.assertTrue(EqualizerConnection.writeCharacteristic(this.gatt, this.writer));
        Assert.assertFalse(EqualizerConnection.onCharacteristicWrite(null, this.gatt, this.writer, 0));
        EqualizerConnection.read(this::completed);
        GattShadow shadow = Shadow.extract(this.gatt);
        Assert.assertEquals(2, shadow.writes);
        reply();
        Assert.assertEquals(0, this.completions);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50));
        Assert.assertEquals(3, shadow.writes);
        finishWrite(0);
        EqualizerCurve fresh = new EqualizerCurve(8, new int[10]).withGain(4, 1);
        reply(fresh);
        Assert.assertEquals(fresh, this.result);
        Assert.assertEquals(1, this.completions);
    }

    @Test
    public void reassemblesFullCurveAcrossEveryNotificationSplit() {
        EqualizerCurve curve = new EqualizerCurve(8, new int[10]).withGain(4, 1);
        byte[] frame = replyFrame(curve);
        for (int split = 1; split < frame.length; split++) {
            connect();
            this.completions = 0;
            EqualizerConnection.read(this::completed);
            finishWrite(0);
            EqualizerConnection.received(this.gatt, new BluetoothGattCharacteristic(NOTIFY, 0, 0),
                    java.util.Arrays.copyOfRange(frame, 0, split), 0);
            idle();
            Assert.assertEquals(0, this.completions);
            EqualizerConnection.received(this.gatt, new BluetoothGattCharacteristic(NOTIFY, 0, 0),
                    java.util.Arrays.copyOfRange(frame, split, frame.length), 0);
            idle();
            Assert.assertEquals(1, this.completions);
            Assert.assertEquals(curve, this.result);
            disconnect();
        }
    }

    @Test
    @Config(shadows = BaseDefinitionsShadow.class)
    public void readsOneByteBuiltInModeReply() {
        connect();
        EqualizerCurve expected = EqualizerBases.entries().get(0).curve;
        EqualizerConnection.read(this::completed);
        finishWrite(0);
        EqualizerConnection.received(this.gatt, new BluetoothGattCharacteristic(NOTIFY, 0, 0),
                new byte[] { (byte) 0xbb, 1, 0, 1, 8, 1, (byte) 0xb4 }, 0);
        idle();
        Assert.assertEquals(expected, this.result);
        Assert.assertEquals(1, this.completions);
    }

    @Test
    @Config(shadows = BaseDefinitionsShadow.class)
    public void confirmsBuiltInApplyFromOneByteModeReply() {
        connect();
        EqualizerCurve expected = EqualizerBases.entries().get(0).curve;
        EqualizerConnection.apply(expected, this::completed);
        finishWrite(0);
        finishWrite(0);
        EqualizerConnection.received(this.gatt, new BluetoothGattCharacteristic(NOTIFY, 0, 0),
                new byte[] { (byte) 0xbb, 1, 0, 1, 8, 1, (byte) 0xb4 }, 0);
        idle();
        Assert.assertEquals(expected, this.result);
        Assert.assertEquals(1, this.completions);
    }

    @Test
    public void rejectsQueuedNotificationAfterSameGattReconnect() {
        connect();
        queueReply(new EqualizerCurve(8, new int[10]));
        EqualizerConnection.disconnected(this.gatt, 0, 0);
        connect();
        idle();
        Assert.assertNull(EqualizerSummary.current(RuntimeEnvironment.getApplication()));
    }

    @Test
    public void rejectsFragmentStartedBeforeOwnedQuery() {
        connect();
        byte[] frame = replyFrame(new EqualizerCurve(8, new int[10]));
        BluetoothGattCharacteristic notify = new BluetoothGattCharacteristic(NOTIFY, 0, 0);
        EqualizerConnection.received(this.gatt, notify, java.util.Arrays.copyOfRange(frame, 0, 20), 0);
        EqualizerConnection.read(this::completed);
        finishWrite(0);
        EqualizerConnection.received(this.gatt, notify, java.util.Arrays.copyOfRange(frame, 20, frame.length), 0);
        idle();
        Assert.assertEquals(0, this.completions);
        reply();
        Assert.assertEquals(1, this.completions);
    }

    @Test
    public void recoversAfterMalformedAndInterruptedNotificationFragments() {
        connect();
        EqualizerConnection.read(this::completed);
        finishWrite(0);
        BluetoothGattCharacteristic notify = new BluetoothGattCharacteristic(NOTIFY, 0, 0);
        EqualizerConnection.received(this.gatt, notify, new byte[] { (byte) 0xbb, (byte) 255, 0, 1, 8 }, 0);
        EqualizerConnection.received(this.gatt, notify, new byte[48], 0);
        byte[] frame = replyFrame(new EqualizerCurve(8, new int[10]));
        EqualizerConnection.received(this.gatt, notify, java.util.Arrays.copyOfRange(frame, 0, 20), 0);
        idle();
        Assert.assertEquals(0, this.completions);
        reply();
        Assert.assertEquals(1, this.completions);
    }

    @Test
    public void disconnectsWhenNativeQueryHasNoResponse() {
        connect();
        this.writer.setValue(new byte[] { (byte) 0xaa, 0, 0, 1, 8, (byte) 0xb3 });
        Assert.assertTrue(EqualizerConnection.writeCharacteristic(this.gatt, this.writer));
        Assert.assertFalse(EqualizerConnection.onCharacteristicWrite(null, this.gatt, this.writer, 0));
        EqualizerConnection.read(this::completed);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(3001));
        Assert.assertEquals(1, this.completions);
        Assert.assertNull(this.result);
        Assert.assertNull(EqualizerConnection.connection());
        GattShadow shadow = Shadow.extract(this.gatt);
        Assert.assertEquals(2, shadow.writes);
        Assert.assertEquals(1, shadow.disconnects);
    }

    @Test
    public void ignoresForwardedReadbackBeforeTakingConnectionMonitor() throws Exception {
        connect();
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        Thread forwarding = new Thread(() -> {
            ThreadLocal<Boolean> marker = org.robolectric.util.ReflectionHelpers
                .getStaticField(app.hxreborn.extension.ultimea.controls.XupmixReadback.class, "FORWARDING_CALLBACK");
            try {
                marker.set(true);
                EqualizerConnection.received(this.gatt, new BluetoothGattCharacteristic(NOTIFY, 0, 0),
                        replyFrame(new EqualizerCurve(8, new int[10])), 0);
            } catch (RuntimeException ex) {
                failure.set(ex);
            } finally {
                marker.remove();
            }
        });
        try {
            synchronized (EqualizerConnection.class) {
                forwarding.start();
                forwarding.join(2000);
                Assert.assertFalse(forwarding.isAlive());
            }
        } finally {
            forwarding.join(2000);
        }
        Assert.assertNull(failure.get());
    }

    @Test
    public void waitsForEveryAcceptedNativeQueryReplyOnAndroid33() {
        connect();
        for (int query : new int[] { 8, 24 }) {
            byte[] request = { (byte) 0xaa, 0, 0, 1, (byte) query, (byte) (0xab + query) };
            Assert.assertEquals(0, EqualizerConnection.writeCharacteristic(this.gatt, this.writer, request,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
            Assert.assertFalse(EqualizerConnection.onCharacteristicWrite(null, this.gatt, this.writer, 0));
        }
        EqualizerConnection.read(this::completed);
        GattShadow shadow = Shadow.extract(this.gatt);
        Assert.assertEquals(3, shadow.writes);
        reply();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50));
        Assert.assertEquals(3, shadow.writes);
        xupmixReply(1);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50));
        Assert.assertEquals(4, shadow.writes);
        Assert.assertEquals(0, this.completions);
        finishWrite(0);
        reply();
        Assert.assertEquals(1, this.completions);
    }

    @Test
    public void failsPendingReadAfterNativeQueryWriteFailure() {
        connect();
        this.writer.setValue(new byte[] { (byte) 0xaa, 0, 0, 1, 8, (byte) 0xb3 });
        Assert.assertTrue(EqualizerConnection.writeCharacteristic(this.gatt, this.writer));
        EqualizerConnection.read(this::completed);
        Assert.assertFalse(
                EqualizerConnection.onCharacteristicWrite(null, this.gatt, this.writer, BluetoothGatt.GATT_FAILURE));
        idle();
        Assert.assertEquals(1, this.completions);
        Assert.assertNull(this.result);
        Assert.assertNull(EqualizerConnection.connection());
        GattShadow shadow = Shadow.extract(this.gatt);
        Assert.assertEquals(2, shadow.writes);
        Assert.assertEquals(1, shadow.disconnects);
    }

    private void finishWrite(int status) {
        Assert.assertTrue(EqualizerConnection.written(this.gatt, this.writer, status));
        idle();
    }

    private void xupmixReply(int enabled) {
        byte[] frame = { (byte) 0xbb, 1, 0, 1, 24, (byte) enabled, (byte) (0xc3 + enabled) };
        EqualizerConnection.received(this.gatt, new BluetoothGattCharacteristic(NOTIFY, 0, 0), frame, 0);
        idle();
    }

    @Test
    public void waitsForXupmixReadbackBeforeEqualizerQuery() {
        connect();
        this.writer.setValue(new byte[] { (byte) 0xaa, 1, 0, 2, 22, 1, (byte) 0xc3 });
        Assert.assertTrue(EqualizerConnection.writeCharacteristic(this.gatt, this.writer));
        int[] hostCompletions = { 0 };
        app.hxreborn.extension.ultimea.controls.ReadbackCallbacks callbacks = new app.hxreborn.extension.ultimea.controls.ReadbackCallbacks() {
            @Override
            public void onReadbackReconnect(BluetoothGatt gatt) {
                Assert.fail("Unexpected reconnect during shared writer handoff");
            }

            @Override
            public void onReadbackWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Assert.assertFalse(EqualizerConnection.onCharacteristicWrite(this, gatt, characteristic, status));
                hostCompletions[0]++;
            }

            @Override
            public void onReadbackReceived(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value,
                    int status) {
                EqualizerConnection.onCharacteristicReceived(this, gatt, characteristic, value, status);
            }
        };
        EqualizerConnection.read(this::completed);
        Assert.assertTrue(EqualizerConnection.onCharacteristicWrite(callbacks, this.gatt, this.writer, 0));
        idle();
        GattShadow shadow = Shadow.extract(this.gatt);
        Assert.assertEquals(3, shadow.writes);
        Assert.assertEquals(24, shadow.lastValue[4]);
        Assert.assertTrue(EqualizerConnection.onCharacteristicWrite(callbacks, this.gatt, this.writer, 0));
        idle();
        EqualizerConnection.onCharacteristicReceived(callbacks, this.gatt,
                new BluetoothGattCharacteristic(NOTIFY, 0, 0), new byte[] { (byte) 0xbb, 1, 0, 1, 24, 1, (byte) 0xc4 },
                0);
        idle();
        Assert.assertEquals(1, hostCompletions[0]);
        Assert.assertEquals(0, this.completions);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50));
        Assert.assertEquals(4, shadow.writes);
        Assert.assertEquals(8, shadow.lastValue[4]);
        Assert.assertTrue(EqualizerConnection.onCharacteristicWrite(callbacks, this.gatt, this.writer, 0));
        idle();
        reply();
        Assert.assertEquals(1, this.completions);
    }

    @Test
    public void observesNativeReadWithoutStartingAnotherQuery() {
        connect();
        reply();
        Assert.assertEquals("Style", EqualizerSummary.current(RuntimeEnvironment.getApplication()));
        Assert.assertEquals(0, this.completions);
        GattShadow shadow = Shadow.extract(this.gatt);
        Assert.assertEquals(1, shadow.writes);
    }

    @Test
    public void clearsObservedNativeModeOnDisconnect() {
        connect();
        reply();
        EqualizerConnection.disconnected(this.gatt, 0, 0);
        idle();
        Assert.assertNull(EqualizerSummary.current(RuntimeEnvironment.getApplication()));
    }

    @Test
    public void readsAfterUnrelatedCharacteristicWrite() {
        connect();
        BluetoothGattCharacteristic unrelated = new BluetoothGattCharacteristic(NOTIFY, 0, 0);
        unrelated.setValue(new byte[] { 0 });
        Assert.assertTrue(EqualizerConnection.writeCharacteristic(this.gatt, unrelated));
        Assert.assertFalse(EqualizerConnection.written(this.gatt, unrelated, 0));
        assertQueryCompletes();
    }

    @Test
    public void readsAfterUnrelatedCharacteristicWriteOnAndroid33() {
        connect();
        BluetoothGattCharacteristic unrelated = new BluetoothGattCharacteristic(NOTIFY, 0, 0);
        Assert.assertEquals(0, EqualizerConnection.writeCharacteristic(this.gatt, unrelated, new byte[] { 0 },
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        Assert.assertFalse(EqualizerConnection.written(this.gatt, unrelated, 0));
        assertQueryCompletes();
    }

    private void assertQueryCompletes() {
        EqualizerConnection.read(this::completed);
        GattShadow shadow = Shadow.extract(this.gatt);
        Assert.assertEquals(3, shadow.writes);
        Assert.assertEquals(1, shadow.lastValue[3]);
        Assert.assertEquals(8, shadow.lastValue[4]);
        EqualizerConnection.written(this.gatt, this.writer, 0);
        idle();
        reply();
        Assert.assertEquals(1, this.completions);
        Assert.assertEquals(new EqualizerCurve(8, new int[10]), this.result);
    }

    @Test
    public void ignoresReplyBeforeOwnedQueryIsWritten() {
        connect();
        Assert.assertTrue(EqualizerConnection.writeCharacteristic(this.gatt, this.writer));
        EqualizerConnection.read(this::completed);
        reply();
        Assert.assertEquals(0, this.completions);
        Assert.assertFalse(EqualizerConnection.written(this.gatt, this.writer, 0));
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50));
        Assert.assertTrue(EqualizerConnection.written(this.gatt, this.writer, 0));
        idle();
        reply();
        Assert.assertEquals(1, this.completions);
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
    public void ignoresNotificationQueuedBeforeRead() {
        connect();
        queueReply(new EqualizerCurve(8, new int[10]));
        EqualizerConnection.read(this::completed);
        idle();
        EqualizerConnection.written(this.gatt, this.writer, 0);
        idle();
        Assert.assertEquals(0, this.completions);
        reply();
        Assert.assertEquals(1, this.completions);
    }

    @Test
    public void ignoresNotificationQueuedForPreviousRead() {
        connect();
        EqualizerConnection.read((curve) -> {
            completed(curve);
            EqualizerConnection.read(this::completed);
        });
        EqualizerConnection.written(this.gatt, this.writer, 0);
        idle();
        queueReply(new EqualizerCurve(8, new int[10]));
        queueReply(new EqualizerCurve(8, new int[10]));
        idle();
        Assert.assertEquals(1, this.completions);
        EqualizerConnection.written(this.gatt, this.writer, 0);
        idle();
        Assert.assertEquals(1, this.completions);
        reply();
        Assert.assertEquals(2, this.completions);
    }

    @Test
    public void retainsSameGattReconnectBeforeMainQueueDrains() {
        for (boolean modern : new boolean[] { false, true }) {
            assertSameGattReconnect(modern);
            disconnect();
        }
    }

    @Test
    @Config(sdk = 26)
    public void retainsSameGattReconnectThroughLegacyWriteOnApi26() {
        assertSameGattReconnect(false);
    }

    private void assertSameGattReconnect(boolean modern) {
        connect();
        EqualizerConnection.onConnectionStateChanged(this.gatt, 0, BluetoothProfile.STATE_DISCONNECTED);
        EqualizerConnection.onConnectionStateChanged(this.gatt, 0, BluetoothProfile.STATE_CONNECTED);
        BluetoothGattCharacteristic freshWriter = new BluetoothGattCharacteristic(WRITE, 8, 16);
        org.robolectric.util.ReflectionHelpers.setField(freshWriter, "mInstance", 77);
        android.bluetooth.BluetoothGattService freshService = new android.bluetooth.BluetoothGattService(
                UUID.randomUUID(), 0);
        freshService.addCharacteristic(freshWriter);
        freshWriter.setValue(new byte[] { (byte) 0xaa, 0, 0, 1, 7, (byte) 0xb2 });
        if (modern) {
            Assert.assertEquals(0, EqualizerConnection.writeCharacteristic(this.gatt, freshWriter,
                    freshWriter.getValue(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        } else {
            Assert.assertTrue(EqualizerConnection.writeCharacteristic(this.gatt, freshWriter));
        }
        Assert.assertFalse(EqualizerConnection.onCharacteristicWrite(null, this.gatt, freshWriter, 0));
        EqualizerCurve curve = new EqualizerCurve(8, new int[10]);
        EqualizerSummary.curve(curve);
        idle();
        Assert.assertSame(this.gatt, EqualizerConnection.connection());
        Assert.assertNotNull(EqualizerSummary.current(RuntimeEnvironment.getApplication()));
        int completedBefore = this.completions;
        EqualizerConnection.read(this::completed);
        GattShadow shadow = Shadow.extract(this.gatt);
        Assert.assertEquals(77, shadow.lastCharacteristic.getInstanceId());
        Assert.assertSame(freshService, shadow.lastCharacteristic.getService());
        Assert.assertTrue(EqualizerConnection.onCharacteristicWrite(null, this.gatt, freshWriter, 0));
        idle();
        reply(curve);
        Assert.assertEquals(completedBefore + 1, this.completions);
        Assert.assertEquals(curve, this.result);
    }

    @Test
    public void completesDisconnectedTransactionOnMainBeforeAcceptingReplacement() {
        connect();
        EqualizerConnection.read(this::completed);
        EqualizerConnection.disconnected(this.gatt, 0, 0);
        BluetoothGatt replacement = Shadow.newInstanceOf(BluetoothGatt.class);
        Assert.assertNull(EqualizerConnection.connection());
        Assert.assertEquals(0, this.completions);
        Assert.assertFalse(EqualizerConnection.writeCharacteristic(replacement, this.writer));
        idle();
        Assert.assertEquals(1, this.completions);
        Assert.assertNull(this.result);
        Assert.assertTrue(EqualizerConnection.writeCharacteristic(replacement, this.writer));
        Assert.assertSame(replacement, EqualizerConnection.connection());
        EqualizerConnection.disconnected(replacement, 0, 0);
        idle();
    }

    @Test
    public void retainsReplacementConnectionAfterDisconnect() {
        connect();
        EqualizerConnection.disconnected(this.gatt, 0, 0);
        BluetoothGatt replacement = Shadow.newInstanceOf(BluetoothGatt.class);
        Assert.assertTrue(EqualizerConnection.writeCharacteristic(replacement, this.writer));
        Assert.assertFalse(EqualizerConnection.written(replacement, this.writer, 0));
        idle();
        EqualizerConnection.read(this::completed);
        Assert.assertEquals(0, this.completions);
        GattShadow shadow = Shadow.extract(replacement);
        Assert.assertEquals(2, shadow.writes);
        EqualizerConnection.disconnected(replacement, 0, 0);
        idle();
        Assert.assertEquals(1, this.completions);
        Assert.assertNull(this.result);
    }

    @Test
    public void disconnectsAfterMissingWriteCallback() {
        connect();
        EqualizerConnection.read(this::completed);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(3001));
        Assert.assertEquals(1, this.completions);
        Assert.assertNull(this.result);
        GattShadow shadow = Shadow.extract(this.gatt);
        Assert.assertEquals(1, shadow.disconnects);
        Assert.assertFalse(EqualizerConnection.written(this.gatt, this.writer, 0));
        Assert.assertFalse(EqualizerConnection.writeCharacteristic(this.gatt, this.writer));
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

    @Test
    public void rejectsReentrantReadAfterDisconnect() {
        connect();
        EqualizerConnection.read((curve) -> {
            completed(curve);
            EqualizerConnection.read(this::completed);
        });
        EqualizerConnection.written(this.gatt, this.writer, 0);
        idle();
        EqualizerConnection.disconnected(this.gatt, 0, 0);
        idle();
        Assert.assertEquals(2, this.completions);
        Assert.assertNull(this.result);
    }

    @Test
    public void confirmsBuiltInModeFromFullCurve() {
        connect();
        EqualizerCurve target = new EqualizerCurve(3, new int[10]);
        EqualizerConnection.apply(target, this::completed);
        EqualizerConnection.written(this.gatt, this.writer, 0);
        idle();
        EqualizerConnection.written(this.gatt, this.writer, 0);
        idle();
        reply(target);
        Assert.assertEquals(1, this.completions);
        Assert.assertEquals(target, this.result);
    }

    @Test
    public void preservesAcceptedHostWriteAfterRejectedWrite() {
        assertRejectedWriteKeepsHostOwnership(false);
    }

    @Test
    public void preservesAcceptedHostWriteAfterRejectedWriteOnAndroid33() {
        assertRejectedWriteKeepsHostOwnership(true);
    }

    private void assertRejectedWriteKeepsHostOwnership(boolean modern) {
        connect();
        Assert.assertTrue(EqualizerConnection.writeCharacteristic(this.gatt, this.writer));
        GattShadow shadow = Shadow.extract(this.gatt);
        shadow.writeResult = 201;
        if (modern) {
            Assert.assertEquals(201, EqualizerConnection.writeCharacteristic(this.gatt, this.writer,
                    this.writer.getValue(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        } else {
            Assert.assertFalse(EqualizerConnection.writeCharacteristic(this.gatt, this.writer));
        }
        BluetoothGatt rejected = Shadow.newInstanceOf(BluetoothGatt.class);
        GattShadow rejectedShadow = Shadow.extract(rejected);
        rejectedShadow.writeResult = 201;
        if (modern) {
            Assert.assertEquals(201, EqualizerConnection.writeCharacteristic(rejected, this.writer,
                    this.writer.getValue(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        } else {
            Assert.assertFalse(EqualizerConnection.writeCharacteristic(rejected, this.writer));
        }
        Assert.assertSame(this.gatt, EqualizerConnection.connection());
        shadow.writeResult = 0;
        EqualizerConnection.read(this::completed);
        Assert.assertEquals(3, shadow.writes);
        Assert.assertFalse(EqualizerConnection.written(this.gatt, this.writer, 0));
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50));
        Assert.assertEquals(4, shadow.writes);
        EqualizerConnection.written(this.gatt, this.writer, 0);
        idle();
        reply();
        Assert.assertEquals(1, this.completions);
    }

    @Test
    public void completesTimeoutWhenDisconnectPermissionFails() {
        connect();
        GattShadow shadow = Shadow.extract(this.gatt);
        shadow.disconnectFailure = new SecurityException("Permission revoked");
        EqualizerConnection.read(this::completed);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(3001));
        Assert.assertEquals(1, this.completions);
        Assert.assertNull(this.result);
        Assert.assertEquals(1, shadow.disconnects);
        shadow.disconnectFailure = null;
        Assert.assertFalse(EqualizerConnection.writeCharacteristic(this.gatt, this.writer));
        Assert.assertEquals(2, shadow.disconnects);
        Assert.assertNull(EqualizerConnection.connection());
    }

    @Test
    public void ignoresReplyArrivingBeforeDelayedQueryStarts() {
        connect();
        Assert.assertTrue(EqualizerConnection.writeCharacteristic(this.gatt, this.writer));
        EqualizerConnection.read(this::completed);
        Assert.assertFalse(EqualizerConnection.written(this.gatt, this.writer, 0));
        org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofMillis(50));
        queueReply(new EqualizerCurve(8, new int[10]));
        idle();
        Assert.assertTrue(EqualizerConnection.written(this.gatt, this.writer, 0));
        idle();
        Assert.assertEquals(0, this.completions);
        reply();
        Assert.assertEquals(1, this.completions);
    }

    @Test
    public void disconnectsAfterMissingHostWriteCallback() {
        connect();
        Assert.assertTrue(EqualizerConnection.writeCharacteristic(this.gatt, this.writer));
        EqualizerConnection.read(this::completed);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(3001));
        Assert.assertEquals(1, this.completions);
        Assert.assertNull(this.result);
        GattShadow shadow = Shadow.extract(this.gatt);
        Assert.assertEquals(1, shadow.disconnects);
        EqualizerConnection.read(this::completed);
        Assert.assertEquals(2, this.completions);
        this.gatt = Shadow.newInstanceOf(BluetoothGatt.class);
        connect();
        EqualizerConnection.read(this::completed);
        shadow = Shadow.extract(this.gatt);
        Assert.assertEquals(1, shadow.lastValue[3]);
        Assert.assertEquals(8, shadow.lastValue[4]);
        EqualizerConnection.written(this.gatt, this.writer, 0);
        idle();
        reply();
        Assert.assertEquals(3, this.completions);
    }

    @Test
    public void completesReadAfterIdentityPermissionFailure() {
        connect();
        GattShadow shadow = Shadow.extract(this.gatt);
        shadow.identityFailure = new SecurityException("Bluetooth permission rejected");
        EqualizerConnection.read(this::completed);
        Assert.assertEquals(1, this.completions);
        Assert.assertNull(this.result);
        EqualizerConnection.read(this::completed);
        Assert.assertEquals(2, this.completions);
    }

    @Test
    public void completesReadAfterScheduledWritePermissionFailure() {
        connect();
        Assert.assertTrue(EqualizerConnection.writeCharacteristic(this.gatt, this.writer));
        EqualizerConnection.read(this::completed);
        Assert.assertFalse(EqualizerConnection.written(this.gatt, this.writer, 0));
        GattShadow shadow = Shadow.extract(this.gatt);
        shadow.writeFailure = new SecurityException("Bluetooth permission rejected");
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50));
        Assert.assertEquals(1, this.completions);
        Assert.assertNull(this.result);
        Assert.assertEquals(1, shadow.disconnects);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(3001));
        Assert.assertEquals(1, this.completions);
    }

    @Test
    public void preservesCompletionDeliveredBeforeHostWriteReturns() throws Exception {
        assertEarlyHostCompletion(false);
    }

    @Test
    public void preservesCompletionDeliveredBeforeAndroid33WriteReturns() throws Exception {
        assertEarlyHostCompletion(true);
    }

    private void assertEarlyHostCompletion(boolean modern) throws Exception {
        connect();
        GattShadow shadow = Shadow.extract(this.gatt);
        java.util.concurrent.CountDownLatch completed = new java.util.concurrent.CountDownLatch(1);
        boolean[] consumed = { true };
        Thread callback = new Thread(() -> {
            consumed[0] = EqualizerConnection.written(this.gatt, this.writer, 0);
            completed.countDown();
        });
        shadow.beforeReturn = () -> {
            callback.start();
            try {
                completed.await(100, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AssertionError(ex);
            }
        };
        if (modern) {
            Assert.assertEquals(0, EqualizerConnection.writeCharacteristic(this.gatt, this.writer,
                    this.writer.getValue(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        } else {
            Assert.assertTrue(EqualizerConnection.writeCharacteristic(this.gatt, this.writer));
        }
        Assert.assertTrue(completed.await(2, java.util.concurrent.TimeUnit.SECONDS));
        callback.join();
        Assert.assertFalse(consumed[0]);
        shadow.beforeReturn = null;
        EqualizerConnection.read(this::completed);
        Assert.assertEquals(3, shadow.writes);
        EqualizerConnection.written(this.gatt, this.writer, 0);
        idle();
        reply();
        Assert.assertEquals(1, this.completions);
    }

    @Test
    public void emitsCompleteBuiltInSetterFrame() {
        connect();
        EqualizerConnection.apply(new EqualizerCurve(3, new int[10]), this::completed);
        GattShadow shadow = Shadow.extract(this.gatt);
        Assert.assertArrayEquals(new byte[] { (byte) 0xaa, 1, 0, 2, 4, 3, (byte) 0xb3 }, shadow.lastValue);
    }

    @Test
    public void emitsCompleteCustomSetterFrame() {
        connect();
        int[] gains = { -14, 1, 2, 3, 4, 5, 6, 7, 8, 14 };
        EqualizerConnection.apply(new EqualizerCurve(7, gains), this::completed);
        GattShadow shadow = Shadow.extract(this.gatt);
        byte[] expected = { (byte) 0xaa, 41, 0, 2, 4, 7, 31, 0, (byte) 0xf2, (byte) 0xff, 62, 0, 1, 0, 125, 0, 2, 0,
                (byte) 0xfa, 0, 3, 0, (byte) 0xf4, 1, 4, 0, (byte) 0xe8, 3, 5, 0, (byte) 0xd0, 7, 6, 0, (byte) 0xa0, 15,
                7, 0, 64, 31, 8, 0, (byte) 0x80, 62, 14, 0, 0 };
        int checksum = 0xaa + 2 + 4;
        for (int index = 5; index < expected.length - 1; index++) {
            checksum += expected[index] & 255;
        }
        expected[expected.length - 1] = (byte) checksum;
        Assert.assertArrayEquals(expected, shadow.lastValue);
    }

    @Test
    public void ignoresCorruptedChecksumBeforeAcceptingValidEqualizerReply() {
        connect();
        EqualizerCurve original = new EqualizerCurve(8, new int[10]);
        EqualizerSummary.curve(original);
        String summary = EqualizerSummary.current(RuntimeEnvironment.getApplication());
        EqualizerConnection.read(this::completed);
        finishWrite(0);
        EqualizerCurve changed = original.withGain(4, 1);
        byte[] invalid = replyFrame(changed);
        invalid[invalid.length - 1] ^= 1;
        EqualizerConnection.received(this.gatt, new BluetoothGattCharacteristic(NOTIFY, 0, 0), invalid, 0);
        idle();
        Assert.assertEquals(0, this.completions);
        Assert.assertEquals(summary, EqualizerSummary.current(RuntimeEnvironment.getApplication()));
        reply(changed);
        Assert.assertEquals(1, this.completions);
        Assert.assertEquals(changed, this.result);
    }

    @Test
    @Config(sdk = 26)
    public void readsAndAppliesStateThroughLegacyWrapperOnApi26() {
        readsCurveAndXupmixAsOneState();
        this.completions = 0;
        disconnect();
        GattShadow shadow = Shadow.extract(this.gatt);
        shadow.writes = 0;
        appliesSameCurveXupmixChangeAndReadsFinalState();
    }

    @Test
    @Config(shadows = SummaryShadow.class)
    public void rejectsStateReadWhenConnectionChangesBetweenComponents() {
        connect();
        BluetoothGatt replacement = Shadow.newInstanceOf(BluetoothGatt.class);
        SummaryShadow.replacement = replacement;
        SummaryShadow.writer = new BluetoothGattCharacteristic(WRITE, 0, 0);
        SummaryShadow.curveReads = 0;
        int[] completed = { 0 };
        try {
            EqualizerConnection.readState((state) -> {
                Assert.assertNull(state);
                completed[0]++;
            });
            finishWrite(0);
            reply();
            Assert.assertEquals(1, completed[0]);
            Assert.assertSame(replacement, EqualizerConnection.connection());
            GattShadow shadow = Shadow.extract(replacement);
            Assert.assertEquals(1, shadow.writes);
        } finally {
            EqualizerConnection.disconnected(replacement, 0, 0);
            idle();
        }
    }

    private void connect() {
        this.writer.setValue(new byte[] { (byte) 0xaa, 0, 0, 1, 7, (byte) 0xb2 });
        Assert.assertTrue(EqualizerConnection.writeCharacteristic(this.gatt, this.writer));
        Assert.assertFalse(EqualizerConnection.onCharacteristicWrite(null, this.gatt, this.writer, 0));
    }

    @Test
    public void ignoresInvalidCurvesBeforeAcceptingValidReply() {
        for (boolean writtenFirst : new boolean[] { false, true }) {
            for (int field : new int[] { 5, 6, 8 }) {
                connect();
                this.completions = 0;
                EqualizerCurve original = new EqualizerCurve(8, new int[10]);
                EqualizerSummary.curve(original);
                String summary = EqualizerSummary.current(RuntimeEnvironment.getApplication());
                EqualizerConnection.read(this::completed);
                if (writtenFirst) {
                    finishWrite(0);
                }
                byte[] invalid = replyFrame(original);
                invalid[field] = (byte) ((field == 8) ? 61 : 0);
                int checksum = 0xaa;
                for (int offset = 3; offset < invalid.length - 1; offset++) {
                    checksum += invalid[offset] & 255;
                }
                invalid[invalid.length - 1] = (byte) checksum;
                EqualizerConnection.received(this.gatt, new BluetoothGattCharacteristic(NOTIFY, 0, 0), invalid, 0);
                idle();
                if (!writtenFirst) {
                    finishWrite(0);
                }
                Assert.assertEquals(0, this.completions);
                Assert.assertEquals(summary, EqualizerSummary.current(RuntimeEnvironment.getApplication()));
                EqualizerCurve changed = original.withGain(4, 1);
                reply(changed);
                Assert.assertEquals(1, this.completions);
                Assert.assertEquals(changed, this.result);
                disconnect();
            }
        }
    }

    @Test
    public void disconnectsAfterMissingReplyAndRejectsLateOldCurve() {
        connect();
        EqualizerConnection.read(this::completed);
        finishWrite(0);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(3001));
        Assert.assertEquals(1, this.completions);
        Assert.assertNull(this.result);
        GattShadow shadow = Shadow.extract(this.gatt);
        Assert.assertEquals(1, shadow.disconnects);
        Assert.assertNull(EqualizerConnection.connection());
        EqualizerConnection.read(this::completed);
        Assert.assertEquals(2, this.completions);
        BluetoothGatt replacement = Shadow.newInstanceOf(BluetoothGatt.class);
        try {
            Assert.assertTrue(EqualizerConnection.writeCharacteristic(replacement, this.writer));
            Assert.assertFalse(EqualizerConnection.written(replacement, this.writer, 0));
            EqualizerConnection.read(this::completed);
            Assert.assertTrue(EqualizerConnection.written(replacement, this.writer, 0));
            idle();
            reply();
            Assert.assertEquals(2, this.completions);
            EqualizerCurve changed = new EqualizerCurve(8, new int[10]).withGain(4, 1);
            EqualizerConnection.received(replacement, new BluetoothGattCharacteristic(NOTIFY, 0, 0),
                    replyFrame(changed), 0);
            idle();
            Assert.assertEquals(3, this.completions);
            Assert.assertEquals(changed, this.result);
        } finally {
            EqualizerConnection.disconnected(replacement, 0, 0);
            idle();
        }
    }

    @Test
    public void rejectsOldHostWritesUntilTimedOutConnectionIsReplaced() {
        connect();
        EqualizerConnection.read(this::completed);
        finishWrite(0);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(3001));
        Assert.assertNull(EqualizerConnection.connection());
        Assert.assertFalse(EqualizerConnection.writeCharacteristic(this.gatt, this.writer));
        Assert.assertEquals(201, EqualizerConnection.writeCharacteristic(this.gatt, this.writer, this.writer.getValue(),
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT));
        Assert.assertNull(EqualizerConnection.connection());
        Assert.assertTrue(app.hxreborn.extension.ultimea.controls.XupmixReadback.hasPendingWrite(this.gatt));
    }

    @Test
    @Config(shadows = DisconnectSummaryShadow.class)
    public void holdsConnectionMonitorThroughDisconnectCleanup() {
        connect();
        DisconnectSummaryShadow.inspect = true;
        try {
            EqualizerConnection.disconnected(this.gatt, 0, 0);
            idle();
            Assert.assertTrue(DisconnectSummaryShadow.observed);
            Assert.assertNull(EqualizerConnection.connection());
        } finally {
            DisconnectSummaryShadow.inspect = false;
        }
    }

    private void reply() {
        reply(new EqualizerCurve(8, new int[10]));
    }

    private void reply(EqualizerCurve curve) {
        queueReply(curve);
        idle();
    }

    private void queueReply(EqualizerCurve curve) {
        EqualizerConnection.received(this.gatt, new BluetoothGattCharacteristic(NOTIFY, 0, 0), replyFrame(curve), 0);
    }

    private static byte[] replyFrame(EqualizerCurve curve) {
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
        return frame;
    }

    private void completed(EqualizerCurve curve) {
        this.result = curve;
        this.completions++;
    }

    private static void idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    @Implements(value = EqualizerSummary.class, isInAndroidSdk = false)
    public static class DisconnectSummaryShadow {

        private static boolean inspect;

        private static boolean observed;

        @Implementation
        protected static void clear() {
            if (inspect) {
                observed = true;
                Assert.assertTrue(Thread.holdsLock(EqualizerConnection.class));
            }
        }

    }

    @Implements(value = EqualizerSummary.class, isInAndroidSdk = false)
    public static class SummaryShadow {

        private static BluetoothGatt replacement;

        private static BluetoothGattCharacteristic writer;

        private static int curveReads;

        @Implementation
        protected static void curve(EqualizerCurve curve) {
            curveReads++;
            if (curveReads == 2) {
                writer.setValue(new byte[] { (byte) 0xaa, 0, 0, 1, 7, (byte) 0xb2 });
                Assert.assertTrue(EqualizerConnection.writeCharacteristic(replacement, writer));
                Assert.assertFalse(EqualizerConnection.onCharacteristicWrite(null, replacement, writer, 0));
            }
        }

    }

    @Implements(EqualizerSummary.class)
    public static class SummaryMonitorShadow {

        private static boolean published;

        @Implementation
        protected static void curve(EqualizerCurve curve) {
            Assert.assertTrue(Thread.holdsLock(EqualizerConnection.class));
            published = true;
        }

    }

    @Implements(EqualizerBases.class)
    public static class BaseDefinitionsShadow {

        @Implementation
        protected static String definitions() {
            return "{\"default_mode\":[{\"name\":\"Music\",\"db\":[0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9]}]}";
        }

    }

    @Implements(BluetoothGatt.class)
    public static class GattShadow {

        private String name = "Poseidon D80 BLE";

        private int writes;

        private int disconnects;

        private int writeResult;

        private SecurityException identityFailure;

        private SecurityException writeFailure;

        private Runnable beforeReturn;

        private Runnable beforeReadValue;

        private SecurityException disconnectFailure;

        @Implementation
        protected void disconnect() {
            this.disconnects++;
            if (this.disconnectFailure != null) {
                throw this.disconnectFailure;
            }
        }

        private byte[] lastValue;

        private BluetoothGattCharacteristic lastCharacteristic;

        @Implementation
        protected BluetoothDevice getDevice() {
            if (this.identityFailure != null) {
                throw this.identityFailure;
            }
            BluetoothDevice device = ShadowBluetoothDevice.newInstance("00:00:00:00:00:00");
            Shadows.shadowOf(device).setName(this.name);
            return device;
        }

        @Implementation
        protected boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
            if (this.writeFailure != null) {
                throw this.writeFailure;
            }
            this.writes++;
            this.lastCharacteristic = characteristic;
            if (this.beforeReadValue != null) {
                this.beforeReadValue.run();
            }
            this.lastValue = characteristic.getValue().clone();
            if (this.beforeReturn != null) {
                this.beforeReturn.run();
            }
            return this.writeResult == 0;
        }

        @Implementation
        protected int writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value, int writeType) {
            if (this.writeFailure != null) {
                throw this.writeFailure;
            }
            this.writes++;
            this.lastValue = value.clone();
            if (this.beforeReturn != null) {
                this.beforeReturn.run();
            }
            return this.writeResult;
        }

    }

}
