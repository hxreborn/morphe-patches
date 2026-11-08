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
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import app.hxreborn.extension.ultimea.controls.ReadbackCallbacks;
import app.hxreborn.extension.ultimea.controls.XupmixReadback;

@SuppressWarnings("unused")
@SuppressLint("MissingPermission")
public final class EqualizerConnection {

    private static final String LOG_TAG = "hxUltimea";

    private static final UUID WRITE = UUID.fromString("27758d11-bf3a-4ac6-bee5-6259ccb7c9b7");

    private static final UUID NOTIFY = UUID.fromString("27758d22-bf3a-4ac6-bee5-6259ccb7c9b7");

    private static final int TIMEOUT_MS = 3000;

    private static final int RETRY_MS = 50;

    private static final int FRAME_OVERHEAD = 6;

    private static final int FRAME_HEADER_BYTES = 5;

    private static final int EQ_PAYLOAD_BYTES = 41;

    private static final int MAX_RESPONSE_BYTES = EQ_PAYLOAD_BYTES + FRAME_OVERHEAD;

    private static final int REQUEST_MARKER = 0xaa;

    private static final int RESPONSE_MARKER = 0xbb;

    private static final int QUERY_GROUP = 1;

    private static final int SET_GROUP = 2;

    private static final int EQ_QUERY = 8;

    private static final int EQ_SETTER = 4;

    private static final int XUPMIX_QUERY = 24;

    private static final int XUPMIX_SETTER = 22;

    private static final int WRITE_BUSY = 201;

    private static final int ANDROID_TIRAMISU = 33;

    private static final String D80_NAME = "Poseidon D80 BLE";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static volatile BluetoothGatt current;

    private static BluetoothGattCharacteristic writer;

    private static volatile boolean hostWriting;

    private static volatile Transaction pending;

    private static int hostCurveQueries;

    private static int hostXupmixQueries;

    private static NotificationFrame incoming;

    private static long connectionVersion;

    private EqualizerConnection() {
    }

    public static synchronized boolean writeCharacteristic(BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic) {
        if (!canWrite(characteristic)) {
            return false;
        }
        byte[] value = characteristic.getValue();
        byte[] request = (value != null) ? value.clone() : null;
        boolean accepted = XupmixReadback.writeCharacteristic(gatt, characteristic);
        if (accepted) {
            trackAcceptedWrite(gatt, characteristic, request);
        }
        return accepted;
    }

    @TargetApi(ANDROID_TIRAMISU)
    public static synchronized int writeCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
            byte[] value, int writeType) {
        if (!canWrite(characteristic)) {
            return WRITE_BUSY;
        }
        int result = XupmixReadback.writeCharacteristic(gatt, characteristic, value, writeType);
        if (result == 0) {
            trackAcceptedWrite(gatt, characteristic, value);
        }
        return result;
    }

    private static boolean canWrite(BluetoothGattCharacteristic characteristic) {
        return !WRITE.equals(characteristic.getUuid()) || pending == null;
    }

    private static void trackAcceptedWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
            byte[] value) {
        if (!WRITE.equals(characteristic.getUuid())) {
            return;
        }
        if (current != gatt) {
            clearConnection();
            current = gatt;
        }
        writer = characteristic;
        hostWriting = true;
        if (value != null && value.length == FRAME_OVERHEAD && (value[0] & 255) == REQUEST_MARKER && value[1] == 0
                && value[2] == 0 && value[3] == QUERY_GROUP
                && value[FRAME_HEADER_BYTES] == (byte) (REQUEST_MARKER + QUERY_GROUP + (value[4] & 255))) {
            if (value[4] == EQ_QUERY) {
                hostCurveQueries++;
            } else if (value[4] == XUPMIX_QUERY) {
                hostXupmixQueries++;
            }
        }
    }

    public static boolean onCharacteristicWrite(ReadbackCallbacks callback, BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic, int status) {
        return XupmixReadback.onCharacteristicWrite(callback, gatt, characteristic, status)
                || written(gatt, characteristic, status);
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

    public static synchronized boolean written(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
            int status) {
        if (gatt != current || !WRITE.equals(characteristic.getUuid())) {
            return false;
        }
        if (XupmixReadback.isForwardingCallback()) {
            hostWriting = false;
            return false;
        }
        Transaction transaction = pending;
        if (transaction != null && transaction.writing) {
            MAIN.post(() -> transaction.written(status));
            return true;
        }
        if (status != 0 && (hostCurveQueries != 0 || hostXupmixQueries != 0)) {
            XupmixReadback.disconnect(gatt);
            disconnected(gatt, status, 0);
        }
        hostWriting = false;
        return false;
    }

    public static void received(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value,
            int status) {
        if (XupmixReadback.isForwardingCallback()) {
            return;
        }
        synchronized (EqualizerConnection.class) {
            if (gatt != current || status != 0 || !NOTIFY.equals(characteristic.getUuid()) || value == null) {
                return;
            }
            if (value.length == 0) {
                return;
            }
            if (incoming == null || NotificationFrame.hasQueryHeader(value)) {
                incoming = new NotificationFrame(pending, hostCurveQueries != 0 || hostXupmixQueries != 0);
            }
            NotificationFrame notification = incoming;
            byte[] frame = notification.append(value);
            if (notification.finished) {
                incoming = null;
            }
            if (frame == null) {
                return;
            }
            MAIN.post(() -> {
                synchronized (EqualizerConnection.class) {
                    if (gatt != current || notification.connectionVersion != connectionVersion) {
                        return;
                    }
                    int operation = updateSummary(frame);
                    if (notification.hostQueryPendingAtArrival) {
                        if (operation == EQ_QUERY && hostCurveQueries > 0) {
                            hostCurveQueries--;
                        } else if (operation == XUPMIX_QUERY && hostXupmixQueries > 0) {
                            hostXupmixQueries--;
                        }
                    } else if (notification.queryStartedAtArrival && pending == notification.transaction) {
                        notification.transaction.received(frame);
                    }
                }
            });
        }
    }

    private static int updateSummary(byte[] frame) {
        if (frame.length < FRAME_OVERHEAD || (frame[0] & 255) != RESPONSE_MARKER || frame[2] != 0
                || frame[3] != QUERY_GROUP || (frame[1] & 255) + FRAME_OVERHEAD != frame.length) {
            return -1;
        }
        int operation = frame[4] & 255;
        int length = frame[1] & 255;
        if (operation != EQ_QUERY && operation != XUPMIX_QUERY
                || operation == EQ_QUERY && length != EQ_PAYLOAD_BYTES && length != 1 || operation == XUPMIX_QUERY
                        && (length != 1 || frame[FRAME_HEADER_BYTES] < 0 || frame[FRAME_HEADER_BYTES] > 1)) {
            return -1;
        }
        int checksum = REQUEST_MARKER;
        for (int offset = 3; offset < frame.length - 1; offset++) {
            checksum += frame[offset] & 255;
        }
        if ((byte) checksum != frame[frame.length - 1]) {
            return -1;
        }
        if (operation == EQ_QUERY) {
            EqualizerCurve state = curve(Arrays.copyOfRange(frame, FRAME_HEADER_BYTES, frame.length - 1));
            if (state != null) {
                EqualizerSummary.curve(state);
            } else {
                return -1;
            }
        } else {
            EqualizerSummary.xupmix(frame[FRAME_HEADER_BYTES]);
        }
        return operation;
    }

    public static synchronized void disconnected(BluetoothGatt gatt, int status, int state) {
        if (gatt == current && (status != 0 || state == 0)) {
            Transaction transaction = pending;
            clearConnection();
            if (transaction != null) {
                MAIN.post(() -> transaction.finish(null));
            }
        }
    }

    private static void clearConnection() {
        connectionVersion++;
        current = null;
        writer = null;
        hostWriting = false;
        hostCurveQueries = 0;
        hostXupmixQueries = 0;
        incoming = null;
        EqualizerSummary.clear();
    }

    static void read(EqualizerCallback callback) {
        start(EQ_SETTER, EQ_QUERY, null, (payload) -> {
            EqualizerCurve state = curve(payload);
            if (state != null) {
                EqualizerSummary.curve(state);
            }
            callback.completed(state);
        });
    }

    static void apply(EqualizerCurve curve, EqualizerCallback callback) {
        byte[] payload = (curve.mode() < EqualizerCurve.CUSTOM_MODE) ? new byte[] { (byte) curve.mode() }
                : curve.payload();
        start(EQ_SETTER, EQ_QUERY, payload, (reply) -> callback.completed(curve(reply)));
    }

    static Object connection() {
        return current;
    }

    static synchronized void readState(SoundCallback callback) {
        BluetoothGatt connection = current;
        read((curve) -> {
            synchronized (EqualizerConnection.class) {
                if (curve == null || connection != current) {
                    callback.completed(null);
                    return;
                }
                start(XUPMIX_SETTER, XUPMIX_QUERY, null, (reply) -> {
                    synchronized (EqualizerConnection.class) {
                        if (reply != null && connection == current) {
                            EqualizerSummary.xupmix(reply[0]);
                            callback.completed(new SoundState(curve, reply[0]));
                        } else {
                            callback.completed(null);
                        }
                    }
                });
            }
        });
    }

    static synchronized void applyState(SoundState before, SoundState state, SoundCallback callback) {
        BluetoothGatt connection = current;
        EqualizerCallback afterCurve = (curve) -> {
            synchronized (EqualizerConnection.class) {
                if (curve == null || connection != current) {
                    callback.completed(null);
                } else if (state.xupmix >= 0 && state.xupmix != before.xupmix) {
                    start(XUPMIX_SETTER, XUPMIX_QUERY, new byte[] { (byte) state.xupmix }, (reply) -> {
                        synchronized (EqualizerConnection.class) {
                            if (reply == null || connection != current) {
                                callback.completed(null);
                            } else {
                                readState(callback);
                            }
                        }
                    });
                } else {
                    readState(callback);
                }
            }
        };
        if (state.curve.equals(before.curve)) {
            afterCurve.completed(before.curve);
        } else {
            apply(state.curve, afterCurve);
        }
    }

    private static EqualizerCurve curve(byte[] payload) {
        if (payload == null) {
            return null;
        }
        try {
            if (payload.length == 1) {
                EqualizerBases.Entry base = EqualizerBases.mode(payload[0] & 255);
                return (base != null) ? base.curve : null;
            }
            return EqualizerCurve.fromPayload(payload);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static synchronized void start(int setter, int query, byte[] target, PayloadCallback callback) {
        if (pending != null || current == null || writer == null) {
            callback.completed(null);
            return;
        }
        try {
            if (!D80_NAME.equals(current.getDevice().getName())) {
                callback.completed(null);
                return;
            }
        } catch (SecurityException ex) {
            clearConnection();
            Log.w(LOG_TAG, "Equalizer identity permission rejected", ex);
            callback.completed(null);
            return;
        }
        Transaction transaction = new Transaction(setter, query, target, callback);
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

    private static final class WriteCharacteristic extends BluetoothGattCharacteristic {

        private final BluetoothGattService service;

        private final int instanceId;

        WriteCharacteristic(BluetoothGattCharacteristic original, byte[] value) {
            super(original.getUuid(), original.getProperties(), original.getPermissions());
            this.service = original.getService();
            this.instanceId = original.getInstanceId();
            setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            setValue(value);
        }

        @Override
        public BluetoothGattService getService() {
            return this.service;
        }

        @Override
        public int getInstanceId() {
            return this.instanceId;
        }

    }

    private static final class NotificationFrame {

        private final Transaction transaction;

        private final boolean queryStartedAtArrival;

        private final boolean hostQueryPendingAtArrival;

        private final long connectionVersion = EqualizerConnection.connectionVersion;

        private final byte[] bytes = new byte[MAX_RESPONSE_BYTES];

        private int size;

        private boolean finished;

        NotificationFrame(Transaction transaction, boolean hostQueryPendingAtArrival) {
            this.transaction = transaction;
            this.queryStartedAtArrival = transaction != null && transaction.queryStarted;
            this.hostQueryPendingAtArrival = hostQueryPendingAtArrival;
        }

        static boolean hasQueryHeader(byte[] value) {
            return value.length >= FRAME_HEADER_BYTES && (value[0] & 255) == RESPONSE_MARKER && value[2] == 0
                    && value[3] == QUERY_GROUP && (value[4] == EQ_QUERY || value[4] == XUPMIX_QUERY);
        }

        byte[] append(byte[] value) {
            if (value.length > this.bytes.length - this.size) {
                this.finished = true;
                return null;
            }
            System.arraycopy(value, 0, this.bytes, this.size, value.length);
            this.size += value.length;
            if ((this.bytes[0] & 255) != RESPONSE_MARKER) {
                this.finished = true;
                return null;
            }
            if (this.size < FRAME_HEADER_BYTES) {
                return null;
            }
            int length = this.bytes[1] & 255;
            int operation = this.bytes[4] & 255;
            if (!hasQueryHeader(this.bytes) || operation == EQ_QUERY && length != EQ_PAYLOAD_BYTES && length != 1
                    || operation == XUPMIX_QUERY && length != 1 || this.size > length + FRAME_OVERHEAD) {
                this.finished = true;
                return null;
            }
            if (this.size < length + FRAME_OVERHEAD) {
                return null;
            }
            this.finished = true;
            return Arrays.copyOf(this.bytes, this.size);
        }

    }

    private static final class Transaction {

        private final byte[] target;

        private final int setter;

        private final int query;

        private final PayloadCallback callback;

        private final long deadline = SystemClock.uptimeMillis() + TIMEOUT_MS;

        private boolean setting;

        private volatile boolean writing;

        private boolean completed;

        private volatile boolean queryStarted;

        private byte[] reply;

        private final Runnable timeout = () -> finish(null);

        private final Runnable retry = this::send;

        Transaction(int setter, int query, byte[] target, PayloadCallback callback) {
            this.setter = setter;
            this.query = query;
            this.target = target;
            this.callback = callback;
            this.setting = target != null;
            MAIN.postDelayed(this.timeout, TIMEOUT_MS);
        }

        @SuppressWarnings("deprecation")
        void send() {
            synchronized (EqualizerConnection.class) {
                if (pending != this || current == null || writer == null) {
                    return;
                }
                if (SystemClock.uptimeMillis() >= this.deadline) {
                    finish(null);
                    return;
                }
                if (hostWriting || hostCurveQueries != 0 || hostXupmixQueries != 0
                        || XupmixReadback.hasPendingWrite(current)) {
                    MAIN.postDelayed(this.retry, RETRY_MS);
                    return;
                }
                byte[] value = this.setting ? frame(SET_GROUP, this.setter, this.target)
                        : frame(QUERY_GROUP, this.query, new byte[0]);
                BluetoothGattCharacteristic request = new WriteCharacteristic(writer, value);
                this.writing = true;
                boolean previouslyStarted = this.queryStarted;
                this.queryStarted |= !this.setting;
                try {
                    if (!XupmixReadback.writeWhenIdle(current, request)) {
                        this.queryStarted = previouslyStarted;
                        this.writing = false;
                        MAIN.postDelayed(this.retry, RETRY_MS);
                    }
                } catch (SecurityException ex) {
                    Log.w(LOG_TAG, "Equalizer write permission rejected", ex);
                    finish(null);
                }
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
            if (this.completed || this.setting || !this.queryStarted || value.length < FRAME_OVERHEAD
                    || (value[0] & 255) != RESPONSE_MARKER || value[2] != 0 || value[3] != QUERY_GROUP
                    || (value[4] & 255) != this.query || (value[1] & 255) + FRAME_OVERHEAD != value.length) {
                return;
            }
            int length = value[1] & 255;
            if (this.query == EQ_QUERY && length != EQ_PAYLOAD_BYTES && length != 1 || this.query == XUPMIX_QUERY
                    && (length != 1 || value[FRAME_HEADER_BYTES] < 0 || value[FRAME_HEADER_BYTES] > 1)) {
                return;
            }
            int checksum = REQUEST_MARKER;
            for (int offset = 3; offset < value.length - 1; offset++) {
                checksum += value[offset] & 255;
            }
            if ((byte) checksum != value[value.length - 1]) {
                return;
            }
            byte[] payload = Arrays.copyOfRange(value, FRAME_HEADER_BYTES, value.length - 1);
            if (this.query == EQ_QUERY && curve(payload) == null) {
                return;
            }
            this.reply = payload;
            if (!this.writing) {
                confirmReply();
            }
        }

        private void confirmReply() {
            boolean matches = this.target == null || Arrays.equals(this.target, this.reply);
            if (!matches && this.query == EQ_QUERY && this.target.length == 1
                    && this.reply.length == EQ_PAYLOAD_BYTES) {
                EqualizerCurve returned = curve(this.reply);
                matches = returned != null && returned.mode() == (this.target[0] & 255);
            }
            if (!matches) {
                this.reply = null;
                MAIN.removeCallbacks(this.retry);
                MAIN.postDelayed(this.retry, RETRY_MS);
                return;
            }
            finish(this.reply);
        }

        void finish(byte[] result) {
            synchronized (EqualizerConnection.class) {
                if (pending != this || this.completed) {
                    return;
                }
                this.completed = true;
                if ((this.writing || hostWriting || hostCurveQueries != 0 || hostXupmixQueries != 0
                        || result == null && this.queryStarted) && current != null) {
                    XupmixReadback.disconnect(current);
                    clearConnection();
                }
                pending = null;
                if (incoming != null && incoming.transaction == this) {
                    incoming = null;
                }
                MAIN.removeCallbacks(this.timeout);
                MAIN.removeCallbacks(this.retry);
                this.callback.completed(result);
            }
        }

    }

}
