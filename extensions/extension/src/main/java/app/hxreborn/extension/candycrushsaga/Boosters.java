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
import android.util.Log;

@SuppressWarnings("unused")
public final class Boosters {

    static final String LOG_TAG = "hxCandyCrush";

    static final int NOT_SUBMITTED = 0;

    static final int SUBMITTED = 1;

    private static final String CLIENT_API_PATH = "/rpc/ClientApi";

    private static final int[] ITEM_IDS = { 3101, 3105, 3106, 3108, 3118, 3122, 3124, 3240, 3241, 3257 };

    private static final int AMOUNT = 1000;

    private static final byte[] GAME_SHA256 = { (byte) 0xf6, 0x58, (byte) 0xc3, (byte) 0xae, 0x18, (byte) 0x8b, 0x11,
            (byte) 0xf1, 0x0d, 0x55, 0x47, 0x59, 0x0e, 0x1a, 0x01, 0x72, (byte) 0xcf, (byte) 0x95, (byte) 0xc3, 0x25,
            0x5f, 0x1e, 0x4f, 0x55, 0x76, (byte) 0x84, 0x1a, (byte) 0xcf, 0x68, (byte) 0xfb, 0x48, 0x7e };

    private static final int HASH_BUFFER_SIZE = 65536;

    private static final int LIVES_ITEM_ID = 3669;

    private static final int TIMED_LIVES_ITEM_ID = 3646;

    private static final int LIVES_HOURS = 24;

    private static final int SECONDS_PER_HOUR = 3600;

    private static final List<Boosters> rewards = createRewards();

    private static WeakReference<Activity> activity = new WeakReference<>(null);

    private static boolean verifying;

    private static boolean verified;

    private final int[] itemIds;

    private final int amount;

    private final BoosterGrant currentGrant;

    private Boosters(int[] itemIds, int amount, BoosterGrant currentGrant) {
        this.itemIds = itemIds;
        this.amount = amount;
        this.currentGrant = currentGrant;
    }

    private static List<Boosters> createRewards() {
        List<Boosters> enabled = new ArrayList<>();
        if (boostersEnabled()) {
            enabled.add(new Boosters(ITEM_IDS, AMOUNT, new BoosterGrant(ITEM_IDS, AMOUNT)));
        }
        if (livesEnabled()) {
            enabled.add(new Boosters(new int[] { LIVES_ITEM_ID }, LIVES_HOURS,
                    new BoosterGrant(new int[] { TIMED_LIVES_ITEM_ID }, LIVES_HOURS * SECONDS_PER_HOUR, true)));
        }
        return enabled;
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
        } catch (LinkageError | SecurityException ex) {
            this.currentGrant.fail();
            Log.e(LOG_TAG, "Booster submission failed", ex);
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
                matches = Arrays.equals(GAME_SHA256, digest.digest());
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

    private static native String libraryPath();

}
