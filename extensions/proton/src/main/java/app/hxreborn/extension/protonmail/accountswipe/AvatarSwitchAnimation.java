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
package app.hxreborn.extension.protonmail.accountswipe;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.PixelCopy;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.PopupWindow;
import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.O)
final class AvatarSwitchAnimation {

    private static final long FADE_MS = 140L;

    private static final long BOUNCE_MS = 320L;

    private static final long SETTLE_TIMEOUT_MS = 1500L;

    private static final long CAPTURE_INTERVAL_MS = 16L;

    private static final int MATCHING_CAPTURES = 2;

    private static final int COLOR_TOLERANCE = 8;

    private static final int EDGE_PX = 2;

    private static final float BOUNCE_DISTANCE = 0.22f;

    private final Activity activity;

    private final Window window;

    private final Rect area;

    private final int direction;

    private final AvatarSwitchStyle style;

    private final boolean haptics;

    private final Runnable onFinished;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private AvatarSlotView slotView;

    private PopupWindow popup;

    private Bitmap outgoing;

    private int incomingColor;

    private int matchingCaptures;

    private long settleStartedAt;

    private AvatarSwitchAnimation(Activity activity, Rect avatar, int direction, AvatarSwitchStyle style,
            boolean haptics, Runnable onFinished) {
        this.activity = activity;
        this.window = activity.getWindow();
        this.area = new Rect(avatar);
        this.area.inset(-EDGE_PX, -EDGE_PX);
        this.direction = direction;
        this.style = style;
        this.haptics = haptics;
        this.onFinished = onFinished;
    }

    static void animateToAccount(Activity activity, Rect avatar, int direction, AvatarSwitchStyle style,
            boolean haptics, String initials, String color, Runnable switchAccount, Runnable onFinished) {
        final AvatarSwitchAnimation animation = new AvatarSwitchAnimation(activity, avatar, direction, style, haptics,
                onFinished);
        animation.capture((bitmap) -> {
            if (bitmap == null || !animation.show(bitmap)) {
                switchAccount.run();
                onFinished.run();
                return;
            }
            animation.slotView.setAvatars(bitmap, animation.drawIncoming(initials, color));
            animation.transition(switchAccount);
        });
    }

    static void bounce(Activity activity, Rect avatar, int direction, boolean haptics, Runnable onFinished) {
        final AvatarSwitchAnimation animation = new AvatarSwitchAnimation(activity, avatar, direction,
                AvatarSwitchStyle.SLIDE, haptics, onFinished);
        animation.capture((bitmap) -> {
            if (bitmap == null || !animation.show(bitmap)) {
                onFinished.run();
                return;
            }
            animation.slotView.setAvatars(bitmap, null);
            animation.bounce();
        });
    }

    private boolean show(Bitmap outgoing) {
        if (this.activity.isFinishing() || this.activity.isDestroyed()) {
            return false;
        }

        this.outgoing = outgoing;
        final View decor = this.window.getDecorView();
        this.slotView = new AvatarSlotView(this.activity, outgoing.getPixel(0, 0));
        this.popup = new PopupWindow(this.slotView, this.area.width(), this.area.height(), false);
        this.popup.setTouchable(false);
        this.popup.setAnimationStyle(0);
        this.popup.setBackgroundDrawable(null);
        this.popup.setClippingEnabled(false);
        try {
            this.popup.showAtLocation(decor, Gravity.NO_GRAVITY, this.area.left, this.area.top);
        } catch (WindowManager.BadTokenException ex) {
            return false;
        }
        if (this.haptics) {
            decor.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
        return true;
    }

    private void transition(Runnable switchAccount) {
        final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(AvatarSlotView.TRANSITION_MS);
        animator.setInterpolator(AvatarSlotView.EASING);
        animator.addUpdateListener(
                (update) -> this.slotView.setFrame(this.style, (float) update.getAnimatedValue(), this.direction));
        animator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                switchAccount.run();
                AvatarSwitchAnimation.this.settleStartedAt = SystemClock.uptimeMillis();
                AvatarSwitchAnimation.this.waitForSettledAvatar();
            }

        });
        animator.start();
    }

    private void waitForSettledAvatar() {
        capture((bitmap) -> {
            final boolean matches = bitmap != null && isClose(fillColor(bitmap), this.incomingColor);
            this.matchingCaptures = matches ? this.matchingCaptures + 1 : 0;
            final boolean settled = this.matchingCaptures >= MATCHING_CAPTURES;
            if (settled || SystemClock.uptimeMillis() - this.settleStartedAt > SETTLE_TIMEOUT_MS) {
                fadeOut();
                return;
            }
            this.handler.postDelayed(this::waitForSettledAvatar, CAPTURE_INTERVAL_MS);
        });
    }

    private void fadeOut() {
        this.slotView.animate().alpha(0f).setDuration(FADE_MS).withEndAction(this::dismiss).start();
    }

    private void bounce() {
        final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f, 0f);
        animator.setDuration(BOUNCE_MS);
        animator.setInterpolator(new OvershootInterpolator(1.5f));
        animator.addUpdateListener((update) -> {
            final float progress = (float) update.getAnimatedValue();
            this.slotView.setBounceOffset(this.direction * progress * BOUNCE_DISTANCE * this.area.height());
        });
        animator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                AvatarSwitchAnimation.this.dismiss();
            }

        });
        animator.start();
    }

    private void dismiss() {
        this.popup.dismiss();
        this.onFinished.run();
    }

    private Bitmap drawIncoming(String initials, String color) {
        this.incomingColor = parseColor(color, fillColor(this.outgoing));
        return AvatarSlotView.drawAvatar(this.activity, this.area.width(), this.area.height(), EDGE_PX,
                this.incomingColor, brightestColor(this.outgoing), initials);
    }

    private void capture(AvatarCaptureCallback callback) {
        final int[] decorOnScreen = new int[2];
        this.window.getDecorView().getLocationOnScreen(decorOnScreen);
        final Rect source = new Rect(this.area);
        source.offset(-decorOnScreen[0], -decorOnScreen[1]);
        final Bitmap bitmap = Bitmap.createBitmap(this.area.width(), this.area.height(), Bitmap.Config.ARGB_8888);
        try {
            PixelCopy.request(this.window, source, bitmap,
                    (result) -> callback.onCaptured((result == PixelCopy.SUCCESS) ? bitmap : null), this.handler);
        } catch (IllegalArgumentException ex) {
            callback.onCaptured(null);
        }
    }

    private static int parseColor(String color, int fallback) {
        try {
            return (color != null) ? Color.parseColor(color) : fallback;
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private static boolean isClose(int first, int second) {
        return Math.abs(Color.red(first) - Color.red(second)) <= COLOR_TOLERANCE
                && Math.abs(Color.green(first) - Color.green(second)) <= COLOR_TOLERANCE
                && Math.abs(Color.blue(first) - Color.blue(second)) <= COLOR_TOLERANCE;
    }

    private static int fillColor(Bitmap bitmap) {
        return bitmap.getPixel(bitmap.getWidth() / 6, bitmap.getHeight() / 2);
    }

    private static int brightestColor(Bitmap bitmap) {
        int brightest = Color.WHITE;
        float brightestLuminance = -1f;
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                final int pixel = bitmap.getPixel(x, y);
                final float luminance = Color.luminance(pixel);
                if (luminance > brightestLuminance) {
                    brightestLuminance = luminance;
                    brightest = pixel;
                }
            }
        }
        return brightest;
    }

}
