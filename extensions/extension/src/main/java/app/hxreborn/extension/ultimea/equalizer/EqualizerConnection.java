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

import java.util.Arrays;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import app.hxreborn.extension.ultimea.controls.ReadbackCallbacks;
import app.hxreborn.extension.ultimea.controls.XupmixReadback;

@SuppressWarnings("unused")
@SuppressLint("MissingPermission")
public final class EqualizerConnection {

    private static final UUID WRITE = UUID.fromString("27758d11-bf3a-4ac6-bee5-6259ccb7c9b7");

    private static final UUID NOTIFY = UUID.fromString("27758d22-bf3a-4ac6-bee5-6259ccb7c9b7");

    private static final int TIMEOUT_MS = 3000;

    private static final int RETRY_MS = 50;

    private static final int FRAME_OVERHEAD = 6;

    private static final int FRAME_HEADER_BYTES = 5;

    private static final int EQ_PAYLOAD_BYTES = 41;

    private static final int REQUEST_MARKER = 0xaa;

    private static final int RESPONSE_MARKER = 0xbb;

    private static final int QUERY_GROUP = 1;

    private static final int SET_GROUP = 2;

    private static final int EQ_QUERY = 8;

    private static final int EQ_SETTER = 4;

    private static final int WRITE_BUSY = 201;

    private static final int ANDROID_TIRAMISU = 33;

    private static final String D80_NAME = "Poseidon D80 BLE";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static volatile BluetoothGatt current;

    private static BluetoothGattCharacteristic writer;

    private static volatile boolean hostWriting;

    private static volatile Transaction pending;

    private EqualizerConnection() {
    }

    public static synchronized boolean writeCharacteristic(BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic) {
        if (!track(gatt, characteristic)) {
            return false;
        }
        boolean accepted = XupmixReadback.writeCharacteristic(gatt, characteristic);
        hostWriting = accepted;
        return accepted;
    }

    @TargetApi(ANDROID_TIRAMISU)
    public static synchronized int writeCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
            byte[] value, int writeType) {
        if (!track(gatt, characteristic)) {
            return WRITE_BUSY;
        }
        int result = XupmixReadback.writeCharacteristic(gatt, characteristic, value, writeType);
        hostWriting = result == 0;
        return result;
    }

    private static boolean track(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (!WRITE.equals(characteristic.getUuid())) {
            return true;
        }
        if (current != gatt) {
            if (pending != null) {
                return false;
            }
            current = gatt;
            writer = characteristic;
            hostWriting = false;
        }
        return pending == null;
    }

    public static boolean onCharacteristicWrite(ReadbackCallbacks callback, BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic, int status) {
        return written(gatt, characteristic, status)
                || XupmixReadback.onCharacteristicWrite(callback, gatt, characteristic, status);
    }

    public static byte[] onCharacteristicReceived(ReadbackCallbacks callback, BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic, byte[] value, int status) {
        received(gatt, characteristic, value, status);
        return XupmixReadback.onCharacteristicReceived(callback, gatt, characteristic, value, status);
    }

    public static void onConnectionStateChanged(BluetoothGatt gatt, int status, int state) {
        disconnected(gatt, status, state);
        XupmixReadback.onConnectionStateChanged(gatt, status, state);
    }

    public static boolean written(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (gatt != current || !WRITE.equals(characteristic.getUuid())) {
            return false;
        }
        Transaction transaction = pending;
        if (transaction != null && transaction.writing) {
            MAIN.post(() -> transaction.written(status));
            return true;
        }
        hostWriting = false;
        return false;
    }

    public static void received(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value,
            int status) {
        if (gatt != current || status != 0 || !NOTIFY.equals(characteristic.getUuid()) || value == null) {
            return;
        }
        byte[] frame = value.clone();
        MAIN.post(() -> {
            Transaction transaction = pending;
            if (transaction != null) {
                transaction.received(frame);
            }
        });
    }

    public static void disconnected(BluetoothGatt gatt, int status, int state) {
        if (gatt == current && (status != 0 || state == 0)) {
            MAIN.post(() -> {
                if (pending != null) {
                    pending.finish(null);
                }
                pending = null;
                current = null;
                writer = null;
                hostWriting = false;
            });
        }
    }

    static void read(EqualizerCallback callback) {
        start(null, callback);
    }

    static void apply(EqualizerCurve curve, EqualizerCallback callback) {
        start(curve, callback);
    }

    private static synchronized void start(EqualizerCurve curve, EqualizerCallback callback) {
        if (pending != null || current == null || writer == null || !D80_NAME.equals(current.getDevice().getName())) {
            callback.completed(null);
            return;
        }
        Transaction transaction = new Transaction(curve, callback);
        pending = transaction;
        transaction.send();
    }

    private static byte[] frame(int group, int operation, byte[] payload) {
        byte[] frame = new byte[FRAME_OVERHEAD + payload.length];
        frame[0] = (byte) REQUEST_MARKER;
        frame[1] = (byte) payload.length;
        frame[2] = (byte) (payload.length >> 8);
        frame[3] = (byte) group;
        frame[4] = (byte) operation;
        System.arraycopy(payload, 0, frame, FRAME_HEADER_BYTES, payload.length);
        int checksum = REQUEST_MARKER + group + operation;
        for (byte value : payload) {
            checksum += value & 255;
        }
        frame[frame.length - 1] = (byte) checksum;
        return frame;
    }

    private static final class Transaction {

        private final EqualizerCurve target;

        private final EqualizerCallback callback;

        private final long deadline = SystemClock.uptimeMillis() + TIMEOUT_MS;

        private boolean setting;

        private volatile boolean writing;

        private boolean completed;

        private EqualizerCurve reply;

        private final Runnable timeout = () -> finish(null);

        private final Runnable retry = this::send;

        Transaction(EqualizerCurve target, EqualizerCallback callback) {
            this.target = target;
            this.callback = callback;
            this.setting = target != null;
            MAIN.postDelayed(this.timeout, TIMEOUT_MS);
        }

        @SuppressWarnings("deprecation")
        void send() {
            if (pending != this || current == null || writer == null) {
                return;
            }
            if (SystemClock.uptimeMillis() >= this.deadline) {
                finish(null);
                return;
            }
            if (hostWriting) {
                MAIN.postDelayed(this.retry, RETRY_MS);
                return;
            }
            byte[] value = this.setting ? frame(SET_GROUP, EQ_SETTER, this.target.payload())
                    : frame(QUERY_GROUP, EQ_QUERY, new byte[0]);
            writer.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            writer.setValue(value);
            this.writing = true;
            if (!current.writeCharacteristic(writer)) {
                this.writing = false;
                MAIN.postDelayed(this.retry, RETRY_MS);
            }
        }

        void written(int status) {
            if (pending != this) {
                return;
            }
            this.writing = false;
            if (this.completed) {
                pending = null;
                return;
            }
            if (status != 0) {
                finish(null);
                return;
            }
            if (this.setting) {
                this.setting = false;
                send();
            } else if (this.reply != null) {
                confirmReply();
            }
        }

        void received(byte[] value) {
            if (this.completed || this.setting || value.length != FRAME_OVERHEAD + EQ_PAYLOAD_BYTES
                    || (value[0] & 255) != RESPONSE_MARKER || (value[1] & 255) != EQ_PAYLOAD_BYTES || value[2] != 0
                    || value[3] != QUERY_GROUP || value[4] != EQ_QUERY) {
                return;
            }
            int checksum = REQUEST_MARKER;
            for (int offset = 3; offset < value.length - 1; offset++) {
                checksum += value[offset] & 255;
            }
            if ((byte) checksum != value[value.length - 1]) {
                return;
            }
            try {
                this.reply = EqualizerCurve
                    .fromPayload(Arrays.copyOfRange(value, FRAME_HEADER_BYTES, value.length - 1));
            } catch (IllegalArgumentException ex) {
                finish(null);
                return;
            }
            if (!this.writing) {
                confirmReply();
            }
        }

        private void confirmReply() {
            if (this.target != null && !this.target.equals(this.reply)) {
                this.reply = null;
                MAIN.removeCallbacks(this.retry);
                MAIN.postDelayed(this.retry, RETRY_MS);
                return;
            }
            finish(this.reply);
        }

        void finish(EqualizerCurve result) {
            if (pending != this) {
                return;
            }
            if (this.completed) {
                return;
            }
            this.completed = true;
            if (!this.writing) {
                pending = null;
            }
            MAIN.removeCallbacks(this.timeout);
            MAIN.removeCallbacks(this.retry);
            this.callback.completed(result);
        }

    }

}
