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
package app.hxreborn.extension.candycrushsaga;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

@SuppressWarnings("unused")
public final class Boosters {

    static final String LOG_TAG = "hxCandyCrush";

    static final int NOT_SUBMITTED = 0;

    static final int SUBMITTED = 1;

    static final int SUBMISSION_UNKNOWN = 2;

    private static final String CLIENT_API_PATH = "/rpc/ClientApi";

    private static final int[] ITEM_IDS = boosterIds();

    private static final int AMOUNT = 1000;

    private static final byte[] GAME_SHA256 = gameSha256();

    private static final byte[] ALTERNATE_GAME_SHA256 = alternateGameSha256();

    private static final int CONFIRMED = 3;

    private static final int CONFIRMATION_PENDING = 0;

    private static final int CONFIRMATION_INTERVAL_MS = 100;

    private static final Handler confirmationHandler = new Handler(Looper.getMainLooper());

    private static final int HASH_BUFFER_SIZE = 65536;

    private static final List<Boosters> rewards = createRewards();

    private static WeakReference<Activity> activity = new WeakReference<>(null);

    private static boolean verifying;

    private static boolean verified;

    private final int[] itemIds;

    private final int amount;

    private final BoosterGrant currentGrant;

    private boolean confirmationQueued;

    private Boosters(int[] itemIds, int amount, BoosterGrant currentGrant) {
        this.itemIds = itemIds;
        this.amount = amount;
        this.currentGrant = currentGrant;
    }

    private static List<Boosters> createRewards() {
        List<Boosters> enabled = new ArrayList<>();
        if (boostersEnabled()) {
            enabled.add(new Boosters(ITEM_IDS, AMOUNT,
                    new BoosterGrant(ITEM_IDS, AMOUNT, false, nativeConfirmationEnabled())));
        }
        if (livesEnabled()) {
            enabled.add(new Boosters(new int[] { livesItemId() }, livesAmount(),
                    new BoosterGrant(new int[] { timedLivesItemId() }, livesAmount() * secondsPerLifeUnit(), true,
                            nativeConfirmationEnabled())));
        }
        return enabled;
    }

    private static int[] boosterIds() {
        return new int[0];
    }

    private static byte[] gameSha256() {
        return new byte[0];
    }

    private static byte[] alternateGameSha256() {
        return new byte[0];
    }

    private static int livesItemId() {
        return 0;
    }

    private static int timedLivesItemId() {
        return 0;
    }

    private static int livesAmount() {
        return 0;
    }

    private static int secondsPerLifeUnit() {
        return 0;
    }

    private static boolean nativeConfirmationEnabled() {
        return false;
    }

    private static boolean boostersEnabled() {
        return false;
    }

    private static boolean livesEnabled() {
        return false;
    }

    public static void onNativeHandle(Activity current, long handle) {
        synchronized (Boosters.class) {
            if (handle == 0) {
                if (activity.get() == current) {
                    activity.clear();
                }
                return;
            }
            activity = new WeakReference<>(current);
        }
        for (Boosters reward : rewards) {
            reward.schedule();
        }
    }

    public static void onRequest(long requestId, int method, String url, int timeout, boolean redirect, Object headers,
            byte[] body) {
        if (nativeConfirmationEnabled()) {
            return;
        }
        if (url != null && url.contains(CLIENT_API_PATH)) {
            int[] observations = new int[rewards.size()];
            synchronized (Boosters.class) {
                for (int index = 0; index < rewards.size(); index++) {
                    observations[index] = rewards.get(index).currentGrant.reserveObservation();
                }
            }
            for (int index = 0; index < rewards.size(); index++) {
                rewards.get(index).currentGrant.onRequest(requestId, body, observations[index]);
            }
        }
    }

    public static void onData(long requestId, byte[] bytes, int length) {
        for (Boosters reward : rewards) {
            reward.currentGrant.onData(requestId, bytes, length);
        }
    }

    public static void onResponse(long requestId, boolean open) {
        if (!open) {
            for (Boosters reward : rewards) {
                reward.currentGrant.onResponse(requestId);
            }
            verifyLibrary();
            for (Boosters reward : rewards) {
                reward.schedule();
            }
        }
    }

    private void schedule() {
        if (nativeConfirmationEnabled() && this.currentGrant.state() == BoosterGrant.State.AWAITING) {
            queueConfirmation();
            return;
        }
        Activity current;
        synchronized (Boosters.class) {
            current = activity.get();
            if (!verified || current == null || current.isFinishing() || current.isDestroyed()
                    || !this.currentGrant.reserve()) {
                return;
            }
        }
        try {
            runOnGameThread(current, () -> submit(current));
        } catch (LinkageError | RuntimeException ex) {
            this.currentGrant.fail();
            Log.e(LOG_TAG, "Game thread scheduling failed", ex);
        }
    }

    private void submit(Activity current) {
        synchronized (Boosters.class) {
            if (activity.get() != current || current.isFinishing() || current.isDestroyed()) {
                this.currentGrant.cancelQueued();
                return;
            }
            if (!this.currentGrant.begin()) {
                return;
            }
        }
        try {
            this.currentGrant.submitted(grant(getNativeHandle(current), this.itemIds, this.amount));
            if (nativeConfirmationEnabled()) {
                queueConfirmation();
            }
        } catch (LinkageError | SecurityException ex) {
            this.currentGrant.fail();
            Log.e(LOG_TAG, "Booster submission failed", ex);
        }
    }

    private void queueConfirmation() {
        synchronized (Boosters.class) {
            if (this.confirmationQueued || this.currentGrant.state() != BoosterGrant.State.AWAITING
                    || activity.get() == null) {
                return;
            }
            if (!this.currentGrant.reserveNativeConfirmation()) {
                return;
            }
            this.confirmationQueued = true;
        }
        confirmationHandler.postDelayed(() -> {
            Activity current;
            synchronized (Boosters.class) {
                current = activity.get();
                if (current == null || current.isFinishing() || current.isDestroyed()) {
                    this.confirmationQueued = false;
                    return;
                }
            }
            try {
                runOnGameThread(current, () -> confirmDelivery(current));
            } catch (LinkageError | RuntimeException ex) {
                synchronized (Boosters.class) {
                    this.confirmationQueued = false;
                }
                this.currentGrant.fail();
                Log.e(LOG_TAG, "Reward confirmation scheduling failed", ex);
            }
        }, CONFIRMATION_INTERVAL_MS);
    }

    private void confirmDelivery(Activity current) {
        try {
            synchronized (Boosters.class) {
                if (activity.get() != current || current.isFinishing() || current.isDestroyed()
                        || this.currentGrant.state() != BoosterGrant.State.AWAITING) {
                    return;
                }
            }
            int result = confirm(this.itemIds[0]);
            if (result == CONFIRMED) {
                this.currentGrant.nativeConfirmed();
            } else if (result != CONFIRMATION_PENDING) {
                this.currentGrant.fail();
            }
        } catch (LinkageError | RuntimeException ex) {
            this.currentGrant.fail();
            Log.e(LOG_TAG, "Reward confirmation failed", ex);
        } finally {
            synchronized (Boosters.class) {
                this.confirmationQueued = false;
            }
            queueConfirmation();
        }
    }

    private static boolean hasReadyReward() {
        for (Boosters reward : rewards) {
            if (reward.currentGrant.state() == BoosterGrant.State.READY) {
                return true;
            }
        }
        return false;
    }

    private static void verifyLibrary() {
        synchronized (Boosters.class) {
            if (verified || verifying || !hasReadyReward()) {
                return;
            }
            verifying = true;
        }
        boolean matches = false;
        try {
            System.loadLibrary("ccsboosters");
            String path = libraryPath();
            if (path != null) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                try (InputStream input = new FileInputStream(path)) {
                    byte[] buffer = new byte[HASH_BUFFER_SIZE];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        digest.update(buffer, 0, count);
                    }
                }
                byte[] actual = digest.digest();
                matches = Arrays.equals(GAME_SHA256, actual) || Arrays.equals(ALTERNATE_GAME_SHA256, actual);
            }
        } catch (IOException | NoSuchAlgorithmException | LinkageError | SecurityException ex) {
            Log.e(LOG_TAG, "Game library validation failed", ex);
        }
        synchronized (Boosters.class) {
            verifying = false;
            verified = matches;
            if (!matches) {
                for (Boosters reward : rewards) {
                    reward.currentGrant.fail();
                }
                Log.e(LOG_TAG, "Game library identity does not match");
            }
        }
    }

    private static void runOnGameThread(Activity current, Runnable runnable) {
    }

    private static long getNativeHandle(Activity current) {
        return 0;
    }

    private static native int grant(long handle, int[] itemIds, int amount);

    private static native int confirm(int firstItemId);

    private static native String libraryPath();

}
