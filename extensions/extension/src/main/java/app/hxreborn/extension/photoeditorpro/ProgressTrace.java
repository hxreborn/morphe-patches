/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.photoeditorpro;

import android.os.SystemClock;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ProgressTrace {

    private static final String TAG = "hxreborn/progress";

    private static final int STAGE_UPLOAD = 0;
    private static final int STAGE_PREDICT = 1;
    private static final int STAGE_POLL = 2;
    private static final int STAGE_DELIVER = 3;

    private static final String[] STAGE_NAMES = {
            "Uploading", "Processing", "Waiting for result", "Finishing",
    };

    private static final int[] STAGE_TARGET = {45, 60, 60, 100};
    private static final long[] STAGE_DURATION_MS = {4000, 1500, 300, 400};

    private static final int STOCK_PROGRESS_FLOOR = 50;
    private static final int POLL_CEILING = 95;
    private static final long POLL_FLOOR_MS = 1500;

    private static final int RESULT_DELIVERED = 0x400;
    private static final float STAGE_SP = 16f;
    private static final int LIVE_ROWS = 8;

    private static final Object LOCK = new Object();
    private static final List<String> LAST_RUN = new ArrayList<>();

    private static WeakReference<TextView> liveView;
    private static CharSequence liveStock;

    private static String feature;
    private static long runStartedAt;
    private static long runStartedAtEpochMs;
    private static long stageStartedAt;
    private static int stage = -1;
    private static int pollCount;
    private static boolean settled;

    private ProgressTrace() {
    }

    private static void enter(int newStage) {
        String closed = null;
        long total;
        synchronized (LOCK) {
            long now = SystemClock.elapsedRealtime();
            if (stage >= 0) {
                closed = String.format(Locale.US, "%s %.2fs",
                        STAGE_NAMES[stage], (now - stageStartedAt) / 1000f);
                LAST_RUN.add(closed);
            }
            if (stage < 0) {
                runStartedAtEpochMs = System.currentTimeMillis();
            }
            total = now - runStartedAt;
            stage = newStage;
            stageStartedAt = now;
        }
        if (closed != null) {
            log(closed);
        }
        log(String.format(Locale.US, "-> %-18s (t=%.2fs)", STAGE_NAMES[newStage], total / 1000f));
    }

    public static void beginUpload(String startingFeature) {
        try {
            synchronized (LOCK) {
                feature = startingFeature;
                runStartedAt = SystemClock.elapsedRealtime();
                runStartedAtEpochMs = System.currentTimeMillis();
                stageStartedAt = runStartedAt;
                stage = -1;
                pollCount = 0;
                settled = false;
                LAST_RUN.clear();
            }
            log("=== " + startingFeature + " started ===");
            enter(STAGE_UPLOAD);
        } catch (Exception ex) {
            Log.w(TAG, "beginUpload", ex);
        }
    }

    public static void predict() {
        try {
            enter(STAGE_PREDICT);
        } catch (Exception ex) {
            Log.w(TAG, "predict", ex);
        }
    }

    public static void polling() {
        try {
            boolean entering;
            int count;
            synchronized (LOCK) {
                entering = stage != STAGE_POLL;
                pollCount++;
                count = pollCount;
            }
            if (entering) {
                enter(STAGE_POLL);
                return;
            }
            long since;
            synchronized (LOCK) {
                since = SystemClock.elapsedRealtime() - stageStartedAt;
            }
            log(String.format(Locale.US, "poll #%d  +%.2fs", count, since / 1000f));
        } catch (Exception ex) {
            Log.w(TAG, "polling", ex);
        }
    }

    public static void finished(int resultCode) {
        try {
            long total;
            int count;
            synchronized (LOCK) {
                if (settled || runStartedAt == 0) {
                    return;
                }
                settled = true;
            }
            enter(STAGE_DELIVER);
            synchronized (LOCK) {
                total = SystemClock.elapsedRealtime() - runStartedAt;
                count = pollCount;
                LAST_RUN.add(String.format(Locale.US, "server result %.2fs (%d polls) code=0x%x",
                        total / 1000f, count, resultCode));
            }
            LiveOverlay.linger(PatchSettings.OVERLAY_LINGER.get().millis);
            if (resultCode == RESULT_DELIVERED) {
                RunHistory.add(currentFeature(), total);
            }
            log(String.format(Locale.US, "=== server result in %.2fs, %d polls, code=0x%x ===",
                    total / 1000f, count, resultCode));
        } catch (Exception ex) {
            Log.w(TAG, "finished", ex);
        }
    }

    public static int progress(int stock) {
        try {
            if (stock >= 100) {
                synchronized (LOCK) {
                    stage = -1;
                }
                return 100;
            }
            int current = currentStage();
            if (!PatchSettings.SHOW_AI_PROGRESS.get() || current < 0) {
                return Math.max(STOCK_PROGRESS_FLOOR, stock);
            }
            int target = STAGE_TARGET[current];
            if (current == STAGE_POLL) {
                long waited;
                long enteredAt;
                synchronized (LOCK) {
                    waited = SystemClock.elapsedRealtime() - stageStartedAt;
                    enteredAt = stageStartedAt - runStartedAt;
                }
                int span = POLL_CEILING - target;
                target += (int) Math.min(span, waited * span / pollScaleMs(enteredAt));
            }
            return Math.max(stock, target);
        } catch (Exception ex) {
            return stock;
        }
    }

    private static long pollScaleMs(long alreadySpent) {
        return Math.max(POLL_FLOOR_MS, RunHistory.expected(currentFeature()) - alreadySpent);
    }

    public static long progressDurationMs(long stock) {
        try {
            int current = currentStage();
            if (!PatchSettings.SHOW_AI_PROGRESS.get() || current < 0) {
                return stock;
            }
            return STAGE_DURATION_MS[current];
        } catch (Exception ex) {
            return stock;
        }
    }

    public static CharSequence label(TextView view, CharSequence stock) {
        try {
            synchronized (LOCK) {
                liveView = new WeakReference<>(view);
                liveStock = stock;
            }
            int current = currentStage();
            if (stock == null || current < 0 || !PatchSettings.SHOW_AI_PROGRESS.get()) {
                return stock;
            }
            String name = STAGE_NAMES[current] + "…";
            CharSequence percent = percentOf(stock);
            if (percent == null) {
                return withLiveCall(view, name);
            }
            float scale = stageScale(view);
            if (scale == 1f) {
                return withLiveCall(view, name + " " + percent);
            }
            SpannableString label = new SpannableString(name + "\n" + percent);
            label.setSpan(new RelativeSizeSpan(scale), 0, name.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return withLiveCall(view, label);
        } catch (Exception ex) {
            return stock;
        }
    }

    static void refreshLive() {
        TextView view;
        CharSequence stock;
        synchronized (LOCK) {
            view = liveView == null ? null : liveView.get();
            stock = liveStock;
        }
        if (view == null || stock == null || !PatchSettings.LOG_ENDPOINTS.get()) {
            return;
        }
        view.post(() -> {
            try {
                view.setText(label(view, stock));
            } catch (Exception ex) {
                Log.w(TAG, "refreshLive", ex);
            }
        });
    }

    private static CharSequence withLiveCall(TextView view, CharSequence label) {
        if (!PatchSettings.LOG_ENDPOINTS.get()) {
            LiveOverlay.hide();
            return label;
        }
        LiveOverlay.render(view, AiTrace.liveRows(runStartedAtEpoch(), LIVE_ROWS),
                "waiting for the first traced call");
        return label;
    }

    private static long runStartedAtEpoch() {
        synchronized (LOCK) {
            return runStartedAtEpochMs;
        }
    }

    private static CharSequence percentOf(CharSequence stock) {
        for (int i = 0; i < stock.length(); i++) {
            if (Character.isDigit(stock.charAt(i))) {
                return stock.subSequence(i, stock.length());
            }
        }
        return null;
    }

    private static float stageScale(TextView view) {
        float target = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, STAGE_SP,
                view.getResources().getDisplayMetrics());
        float actual = view.getTextSize();
        return actual <= target ? 1f : target / actual;
    }

    private static String currentFeature() {
        synchronized (LOCK) {
            return feature;
        }
    }

    static boolean uploading() {
        return currentStage() == STAGE_UPLOAD;
    }

    private static int currentStage() {
        synchronized (LOCK) {
            return stage;
        }
    }

    public static List<String> stages() {
        synchronized (LOCK) {
            return new ArrayList<>(LAST_RUN);
        }
    }

    public static String headline() {
        synchronized (LOCK) {
            if (LAST_RUN.isEmpty()) {
                return "No AI run yet this session";
            }
            return LAST_RUN.get(LAST_RUN.size() - 1);
        }
    }

    private static void log(String message) {
        if (PatchSettings.LOG_ENDPOINTS.get()) {
            Log.i(TAG, message);
        }
    }
}
