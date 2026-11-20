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

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

@SuppressWarnings("unused")
@SuppressLint("MissingPermission")
public final class XupmixReadback {

    private static final String LOG_TAG = "hxUltimea";

    private static final UUID WRITE_CHARACTERISTIC = UUID.fromString("27758d11-bf3a-4ac6-bee5-6259ccb7c9b7");

    private static final UUID NOTIFY_CHARACTERISTIC = UUID.fromString("27758d22-bf3a-4ac6-bee5-6259ccb7c9b7");

    private static final int READ_TIMEOUT_MS = 1800;

    private static final int READ_RETRY_MS = 50;

    private static final int CONNECTION_RETRY_MS = 1000;

    private static final int FAST_CONNECTION_ATTEMPTS = 3;

    private static final int CONNECTION_RECOVERY_MS = 5000;

    private static final int DISCONNECT_TIMEOUT_MS = 2000;

    private static final int ANDROID_TIRAMISU = 33;

    private static final byte[] QUERY = { (byte) 0xaa, 0, 0, 1, 0x18, (byte) 0xc3 };

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static final ThreadLocal<Boolean> FORWARDING_CALLBACK = new ThreadLocal<>();

    private static final PendingRead BUSY_REQUEST = new PendingRead(-1, null);

    private static final PendingRead UNTRACKED_WRITE = new PendingRead(-1, null);

    private static final Map<BluetoothGatt, ArrayDeque<PendingRead>> WRITE_CALLBACK_OWNERS = new WeakHashMap<>();

    private static final Map<BluetoothGatt, RetiredConnection> RETIRED_GATTS = new WeakHashMap<>();

    private static final Map<BluetoothGatt, PendingRead> PENDING = new WeakHashMap<>();

    private static final Map<BluetoothGatt, PendingRead> QUERY_WRITES = new WeakHashMap<>();

    private static final Map<BluetoothGatt, DisconnectState> DISCONNECTING_GATTS = new WeakHashMap<>();

    private static final Map<BluetoothGatt, WeakReference<ReadbackCallbacks>> CALLBACKS = new WeakHashMap<>();

    private XupmixReadback() {
    }

    @TargetApi(ANDROID_TIRAMISU)
    public static int writeCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value,
            int writeType) {
        synchronized (PENDING) {
            retryDisconnect(gatt);
            if (hasPendingWrite(gatt)) {
                return BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY;
            }
            PendingRead pending = trackRequest(gatt, characteristic, value);
            if (pending == BUSY_REQUEST) {
                return BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY;
            }
            try {
                int result = gatt.writeCharacteristic(characteristic, value, writeType);
                if (pending != null) {
                    pending.accepted = result == BluetoothStatusCodes.SUCCESS;
                }
                if (result == BluetoothStatusCodes.SUCCESS) {
                    recordWriteOwner(gatt, characteristic, pending);
                }
                return result;
            } finally {
                if (pending != null && !pending.accepted) {
                    cancelPendingRead(gatt, pending);
                }
            }
        }
    }

    public static boolean writeCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        synchronized (PENDING) {
            retryDisconnect(gatt);
            if (hasPendingWrite(gatt)) {
                return false;
            }
            PendingRead pending = trackRequest(gatt, characteristic, characteristic.getValue());
            if (pending == BUSY_REQUEST) {
                return false;
            }
            try {
                boolean result = gatt.writeCharacteristic(characteristic);
                if (pending != null) {
                    pending.accepted = result;
                }
                if (result) {
                    recordWriteOwner(gatt, characteristic, pending);
                }
                return result;
            } finally {
                if (pending != null && !pending.accepted) {
                    cancelPendingRead(gatt, pending);
                }
            }
        }
    }

    public static boolean onCharacteristicWrite(ReadbackCallbacks callback, BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic, int status) {
        if (Boolean.TRUE.equals(FORWARDING_CALLBACK.get()) || !WRITE_CHARACTERISTIC.equals(characteristic.getUuid())) {
            return false;
        }
        PendingRead query;
        synchronized (PENDING) {
            ArrayDeque<PendingRead> owners = WRITE_CALLBACK_OWNERS.get(gatt);
            if (owners == null || owners.isEmpty()) {
                return RETIRED_GATTS.containsKey(gatt);
            }
            PendingRead completed = owners.removeFirst();
            if (owners.isEmpty()) {
                WRITE_CALLBACK_OWNERS.remove(gatt);
            }
            if (completed == UNTRACKED_WRITE) {
                return RETIRED_GATTS.containsKey(gatt);
            }
            if (completed.disconnected) {
                return true;
            }
            query = QUERY_WRITES.get(gatt);
            if (query != completed) {
                PendingRead pending = PENDING.get(gatt);
                if (pending != completed || pending.queryScheduled) {
                    return false;
                }
                pending.callback = new WeakReference<>(callback);
                if (pending.expired) {
                    PENDING.remove(gatt);
                    MAIN.post(() -> completeHostWrite(gatt, characteristic, pending, BluetoothGatt.GATT_FAILURE));
                    return true;
                }
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    pending.accepted = false;
                    PENDING.remove(gatt);
                    return false;
                }
                pending.queryScheduled = true;
                MAIN.post(() -> queryState(gatt, characteristic, pending));
                return true;
            }
            QUERY_WRITES.remove(gatt);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                query.queryFailed = true;
                if (PENDING.get(gatt) == query) {
                    PENDING.remove(gatt);
                }
            }
        }
        PendingRead completedQuery = query;
        MAIN.post(() -> {
            if (status != BluetoothGatt.GATT_SUCCESS || completedQuery.confirmed) {
                completeHostWrite(gatt, characteristic, completedQuery, status);
            } else {
                scheduleRetry(gatt, completedQuery);
            }
        });
        return true;
    }

    public static byte[] onCharacteristicReceived(ReadbackCallbacks callback, BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic, byte[] value, int status) {
        if (Boolean.TRUE.equals(FORWARDING_CALLBACK.get()) || status != BluetoothGatt.GATT_SUCCESS
                || !NOTIFY_CHARACTERISTIC.equals(characteristic.getUuid())) {
            return value;
        }
        synchronized (PENDING) {
            if (RETIRED_GATTS.containsKey(gatt) || DISCONNECTING_GATTS.containsKey(gatt)) {
                return null;
            }
        }
        if (XupmixFrames.isLegacyCapabilities(value)) {
            try {
                value = XupmixFrames.exposeD80Xupmix(gatt.getDevice().getName(), value);
            } catch (SecurityException ex) {
                Log.w(LOG_TAG, "D80 capability identity read failed", ex);
            }
        }
        PendingRead pending;
        synchronized (PENDING) {
            pending = PENDING.get(gatt);
            if (pending == null) {
                return value;
            }
            if (XupmixFrames.isSetterReply(value)) {
                return null;
            }
            if (!pending.queryStarted) {
                return value;
            }
        }
        byte[] confirmed = XupmixFrames.confirmedSetterReply(pending.requestedState, value);
        if (confirmed == null) {
            if (XupmixFrames.readState(value) < 0) {
                return value;
            }
            MAIN.post(() -> {
                synchronized (PENDING) {
                    if (PENDING.get(gatt) != pending || !pending.queryAccepted) {
                        return;
                    }
                    pending.retryRequested = true;
                }
                scheduleRetry(gatt, pending);
            });
            return null;
        }
        MAIN.post(() -> {
            synchronized (PENDING) {
                if (PENDING.get(gatt) != pending || !pending.queryAccepted || pending.confirmed) {
                    return;
                }
                pending.confirmed = true;
                FORWARDING_CALLBACK.set(true);
                try {
                    callback.onReadbackReceived(gatt, characteristic, confirmed, status);
                } finally {
                    FORWARDING_CALLBACK.remove();
                }
                if (QUERY_WRITES.get(gatt) == pending) {
                    return;
                }
            }
            completeHostWrite(gatt, pending.writer, pending, BluetoothGatt.GATT_SUCCESS);
        });
        return null;
    }

    public static void registerConnection(ReadbackCallbacks callback, BluetoothGatt gatt, int status, int state) {
        synchronized (PENDING) {
            if (!RETIRED_GATTS.containsKey(gatt) && !DISCONNECTING_GATTS.containsKey(gatt)
                    && status == BluetoothGatt.GATT_SUCCESS && state == BluetoothProfile.STATE_CONNECTED) {
                CALLBACKS.put(gatt, new WeakReference<>(callback));
            } else {
                CALLBACKS.remove(gatt);
            }
        }
    }

    public static void onConnectionStateChanged(BluetoothGatt gatt, int status, int connectionState) {
        if (status == BluetoothGatt.GATT_SUCCESS && connectionState == BluetoothProfile.STATE_CONNECTED) {
            return;
        }
        PendingRead pending;
        PendingRead query;
        synchronized (PENDING) {
            pending = PENDING.remove(gatt);
            query = QUERY_WRITES.remove(gatt);
            DisconnectState disconnecting = DISCONNECTING_GATTS.remove(gatt);
            ArrayDeque<PendingRead> owners = WRITE_CALLBACK_OWNERS.get(gatt);
            if ((owners != null && !owners.isEmpty() || pending != null || query != null || disconnecting != null)
                    && !RETIRED_GATTS.containsKey(gatt)) {
                RETIRED_GATTS.put(gatt, new RetiredConnection());
                if (owners != null) {
                    for (PendingRead owner : owners) {
                        if (owner != UNTRACKED_WRITE) {
                            owner.disconnected = true;
                        }
                    }
                }
            }
            if (pending != null) {
                pending.disconnected = true;
            }
            if (query != null) {
                query.disconnected = true;
            }
        }
        if (pending != null) {
            completeHostWrite(gatt, pending.writer, pending, BluetoothGatt.GATT_FAILURE);
        }
        if (query != null && query != pending) {
            completeHostWrite(gatt, query.writer, query, BluetoothGatt.GATT_FAILURE);
        }
    }

    public static boolean isRetiredConnection(BluetoothGatt gatt) {
        synchronized (PENDING) {
            return RETIRED_GATTS.containsKey(gatt);
        }
    }

    public static void replaceRetiredConnection(ReadbackCallbacks callback, BluetoothGatt gatt) {
        RetiredConnection retired;
        synchronized (PENDING) {
            retired = RETIRED_GATTS.get(gatt);
            if (retired == null || retired.replacing || retired.replaced) {
                return;
            }
            retired.replacing = true;
            retired.attempts++;
        }
        try {
            gatt.close();
            callback.onReadbackReconnect(gatt);
            synchronized (PENDING) {
                retired.replaced = true;
                retired.replacing = false;
            }
        } catch (RuntimeException ex) {
            synchronized (PENDING) {
                retired.replacing = false;
                if (!retired.retryScheduled) {
                    retired.retryScheduled = true;
                    WeakReference<ReadbackCallbacks> reference = new WeakReference<>(callback);
                    MAIN.postDelayed(() -> {
                        synchronized (PENDING) {
                            retired.retryScheduled = false;
                        }
                        ReadbackCallbacks target = reference.get();
                        if (target != null) {
                            replaceRetiredConnection(target, gatt);
                        }
                    }, (retired.attempts < FAST_CONNECTION_ATTEMPTS) ? CONNECTION_RETRY_MS : CONNECTION_RECOVERY_MS);
                }
            }
            Log.w(LOG_TAG, "Bluetooth connection replacement failed", ex);
        }
    }

    public static boolean isForwardingCallback() {
        return Boolean.TRUE.equals(FORWARDING_CALLBACK.get());
    }

    public static boolean writeWhenIdle(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        synchronized (PENDING) {
            retryDisconnect(gatt);
            if (hasPendingWrite(gatt) || !gatt.writeCharacteristic(characteristic)) {
                return false;
            }
            recordWriteOwner(gatt, characteristic, null);
            return true;
        }
    }

    public static boolean hasPendingWrite(BluetoothGatt gatt) {
        synchronized (PENDING) {
            if (RETIRED_GATTS.containsKey(gatt) || DISCONNECTING_GATTS.containsKey(gatt)) {
                return true;
            }
            return PENDING.containsKey(gatt) || QUERY_WRITES.containsKey(gatt);
        }
    }

    private static PendingRead trackRequest(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
            byte[] value) {
        if (!WRITE_CHARACTERISTIC.equals(characteristic.getUuid())) {
            return null;
        }
        int requestedState = XupmixFrames.requestedState(value);
        if (requestedState < 0) {
            return null;
        }
        try {
            if (!XupmixFrames.isD80(gatt.getDevice().getName())) {
                return null;
            }
        } catch (SecurityException ex) {
            Log.w(LOG_TAG, "D80 readback identity read failed", ex);
            return null;
        }
        PendingRead pending = new PendingRead(requestedState, characteristic);
        synchronized (PENDING) {
            if (PENDING.containsKey(gatt) || QUERY_WRITES.containsKey(gatt)) {
                return BUSY_REQUEST;
            }
            pending.callback = CALLBACKS.get(gatt);
            PENDING.put(gatt, pending);
        }
        MAIN.postDelayed(() -> {
            boolean disconnectRequired;
            synchronized (PENDING) {
                if (!pending.accepted) {
                    return;
                }
                pending.expired = true;
                disconnectRequired = PENDING.get(gatt) == pending
                        && (!pending.queryScheduled || pending.queryAccepted && !pending.confirmed)
                        || QUERY_WRITES.get(gatt) == pending;
                if (disconnectRequired) {
                    if (!DISCONNECTING_GATTS.containsKey(gatt)) {
                        DISCONNECTING_GATTS.put(gatt, DisconnectState.PENDING);
                    }
                }
                if (pending.queryScheduled && PENDING.get(gatt) == pending) {
                    PENDING.remove(gatt);
                }
            }
            completeHostWrite(gatt, characteristic, pending,
                    pending.confirmed ? BluetoothGatt.GATT_SUCCESS : BluetoothGatt.GATT_FAILURE);
            if (disconnectRequired) {
                disconnect(gatt);
            }
        }, READ_TIMEOUT_MS);
        return pending;
    }

    private static void retryDisconnect(BluetoothGatt gatt) {
        DisconnectState state = DISCONNECTING_GATTS.get(gatt);
        if (state == DisconnectState.REJECTED) {
            disconnect(gatt);
        }
    }

    public static void disconnect(BluetoothGatt gatt) {
        synchronized (PENDING) {
            if (RETIRED_GATTS.containsKey(gatt)) {
                return;
            }
            DisconnectState state = DISCONNECTING_GATTS.get(gatt);
            if (state == DisconnectState.REQUESTED) {
                return;
            }
            DISCONNECTING_GATTS.put(gatt, DisconnectState.REQUESTED);
            try {
                gatt.disconnect();
                MAIN.postDelayed(() -> finishMissingDisconnect(gatt), DISCONNECT_TIMEOUT_MS);
            } catch (SecurityException ex) {
                DISCONNECTING_GATTS.put(gatt, DisconnectState.REJECTED);
                Log.w(LOG_TAG, "Bluetooth timeout disconnect rejected", ex);
            }
        }
    }

    private static void finishMissingDisconnect(BluetoothGatt gatt) {
        ReadbackCallbacks callback;
        synchronized (PENDING) {
            if (DISCONNECTING_GATTS.get(gatt) != DisconnectState.REQUESTED) {
                return;
            }
            WeakReference<ReadbackCallbacks> reference = CALLBACKS.get(gatt);
            callback = (reference != null) ? reference.get() : null;
        }
        if (callback instanceof BluetoothGattCallback) {
            ((BluetoothGattCallback) callback).onConnectionStateChange(gatt, BluetoothGatt.GATT_FAILURE,
                    BluetoothProfile.STATE_DISCONNECTED);
        } else {
            onConnectionStateChanged(gatt, BluetoothGatt.GATT_FAILURE, BluetoothProfile.STATE_DISCONNECTED);
            try {
                gatt.close();
            } catch (SecurityException ex) {
                Log.w(LOG_TAG, "Bluetooth connection close rejected", ex);
            }
        }
    }

    private static void recordWriteOwner(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
            PendingRead pending) {
        if (!WRITE_CHARACTERISTIC.equals(characteristic.getUuid())) {
            return;
        }
        ArrayDeque<PendingRead> owners = WRITE_CALLBACK_OWNERS.get(gatt);
        if (owners == null) {
            owners = new ArrayDeque<>();
            WRITE_CALLBACK_OWNERS.put(gatt, owners);
        }
        owners.addLast((pending != null) ? pending : UNTRACKED_WRITE);
    }

    private static void queryState(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
            PendingRead pending) {
        boolean accepted;
        try {
            synchronized (PENDING) {
                if (PENDING.get(gatt) != pending || DISCONNECTING_GATTS.containsKey(gatt)
                        || RETIRED_GATTS.containsKey(gatt)) {
                    return;
                }
                pending.retryScheduled = false;
                pending.retryRequested = false;
                pending.queryAccepted = false;
                pending.queryStarted = true;
                QUERY_WRITES.put(gatt, pending);
                int properties = characteristic.getProperties();
                int writeType = ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0
                        && (properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0)
                                ? BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                                : BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
                if (Build.VERSION.SDK_INT >= ANDROID_TIRAMISU) {
                    accepted = gatt.writeCharacteristic(characteristic, QUERY.clone(),
                            writeType) == BluetoothStatusCodes.SUCCESS;
                } else {
                    byte[] previousValue = characteristic.getValue();
                    int previousWriteType = characteristic.getWriteType();
                    try {
                        characteristic.setValue(QUERY.clone());
                        characteristic.setWriteType(writeType);
                        accepted = gatt.writeCharacteristic(characteristic);
                    } finally {
                        characteristic.setValue(previousValue);
                        characteristic.setWriteType(previousWriteType);
                    }
                }
                pending.queryAccepted = accepted;
                if (accepted) {
                    recordWriteOwner(gatt, characteristic, pending);
                }
            }
        } catch (RuntimeException ex) {
            rejectQuery(gatt, characteristic, pending);
            Log.w(LOG_TAG, "Xupmix readback failed", ex);
            return;
        }
        if (!accepted) {
            rejectQuery(gatt, characteristic, pending);
            Log.w(LOG_TAG, "Xupmix readback write rejected");
        }
    }

    private static void scheduleRetry(BluetoothGatt gatt, PendingRead pending) {
        synchronized (PENDING) {
            if (PENDING.get(gatt) != pending || !pending.retryRequested || pending.retryScheduled
                    || QUERY_WRITES.containsKey(gatt)) {
                return;
            }
            pending.retryScheduled = true;
        }
        MAIN.postDelayed(() -> queryState(gatt, pending.writer, pending), READ_RETRY_MS);
    }

    private static void rejectQuery(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
            PendingRead pending) {
        synchronized (PENDING) {
            if (QUERY_WRITES.get(gatt) == pending) {
                QUERY_WRITES.remove(gatt);
            }
        }
        cancelPendingRead(gatt, pending);
        completeHostWrite(gatt, characteristic, pending, BluetoothGatt.GATT_FAILURE);
    }

    private static void cancelPendingRead(BluetoothGatt gatt, PendingRead pending) {
        synchronized (PENDING) {
            if (pending != null && PENDING.get(gatt) == pending) {
                PENDING.remove(gatt);
            }
        }
    }

    private static void completeHostWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
            PendingRead pending, int status) {
        ReadbackCallbacks callback;
        synchronized (PENDING) {
            if (pending.confirmed && QUERY_WRITES.get(gatt) != pending && PENDING.get(gatt) == pending) {
                PENDING.remove(gatt);
            }
            if (pending.hostWriteCompleted || pending.callback == null
                    || status == BluetoothGatt.GATT_SUCCESS && (pending.queryFailed || pending.disconnected)) {
                return;
            }
            pending.hostWriteCompleted = true;
            callback = pending.callback.get();
            pending.callback = null;
        }
        if (callback == null) {
            return;
        }
        FORWARDING_CALLBACK.set(true);
        try {
            callback.onReadbackWrite(gatt, characteristic, status);
        } finally {
            FORWARDING_CALLBACK.remove();
        }
    }

    private static final class RetiredConnection {

        private boolean replacing;

        private boolean replaced;

        private boolean retryScheduled;

        private int attempts;

    }

    private enum DisconnectState {

        PENDING, REQUESTED, REJECTED

    }

    private static final class PendingRead {

        private final int requestedState;

        private final BluetoothGattCharacteristic writer;

        private WeakReference<ReadbackCallbacks> callback;

        private boolean accepted;

        private boolean expired;

        private boolean disconnected;

        private boolean queryScheduled;

        private boolean queryStarted;

        private boolean queryAccepted;

        private boolean queryFailed;

        private boolean confirmed;

        private boolean retryRequested;

        private boolean retryScheduled;

        private boolean hostWriteCompleted;

        private PendingRead(int requestedState, BluetoothGattCharacteristic writer) {
            this.requestedState = requestedState;
            this.writer = writer;
        }

    }

}
