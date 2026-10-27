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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import app.morphe.extension.shared.Utils;

import app.hxreborn.extension.proton.PatchContext;
import app.hxreborn.extension.proton.PatchSettings;
import app.hxreborn.extension.protonmail.UniffiCalls;

@SuppressWarnings("unused")
public final class AccountAvatarSwipe {

    private static final String TAG = "AccountAvatarSwipe";

    private static final String HAPTIC_FEEDBACK_KEY = "account_swipe_haptic_feedback";

    private static final String SWITCH_STYLE_KEY = "account_swipe_animation";

    private static final AtomicBoolean SWITCHING = new AtomicBoolean();

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static volatile Object mailSession;

    private final Object onClick;

    private final int touchSlop;

    private final int avatarSizePx;

    private float downX;

    private float downY;

    private int avatarLeft;

    private int avatarTop;

    private boolean tracking;

    private boolean swiped;

    public AccountAvatarSwipe(Object onClick, float avatarSizeDp) {
        this.onClick = onClick;
        this.touchSlop = ViewConfiguration.get(Utils.getContext()).getScaledTouchSlop();
        this.avatarSizePx = Math.round(avatarSizeDp * Utils.getContext().getResources().getDisplayMetrics().density);
    }

    public static void captureMailSession(Object session) {
        mailSession = session;
    }

    public static boolean isHapticFeedbackEnabled() {
        return PatchSettings.isEnabled(HAPTIC_FEEDBACK_KEY, true);
    }

    public static void setHapticFeedbackEnabled(boolean enabled) {
        PatchSettings.setEnabled(HAPTIC_FEEDBACK_KEY, enabled);
    }

    public static AvatarSwitchStyle getSwitchStyle() {
        return AvatarSwitchStyle.fromName(PatchSettings.getString(SWITCH_STYLE_KEY, AvatarSwitchStyle.SLIDE.name()));
    }

    public static void setSwitchStyle(AvatarSwitchStyle style) {
        PatchSettings.setString(SWITCH_STYLE_KEY, style.name());
    }

    public static Object withSwipe(Object modifier, Object onClick) {
        return null;
    }

    public Object invoke(Object motionEvent) {
        final MotionEvent event = (MotionEvent) motionEvent;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                this.downX = event.getRawX();
                this.downY = event.getRawY();
                this.avatarLeft = Math.round(event.getRawX() - event.getX());
                this.avatarTop = Math.round(event.getRawY() - event.getY());
                this.tracking = true;
                this.swiped = false;
                break;
            case MotionEvent.ACTION_MOVE:
                final float deltaY = event.getRawY() - this.downY;
                if (this.tracking && !this.swiped && Math.abs(deltaY) > this.touchSlop
                        && Math.abs(deltaY) > Math.abs(event.getRawX() - this.downX)) {
                    this.swiped = true;
                    switchAccount((deltaY > 0) ? 1 : -1, new Rect(this.avatarLeft, this.avatarTop,
                            this.avatarLeft + this.avatarSizePx, this.avatarTop + this.avatarSizePx));
                }
                break;
            case MotionEvent.ACTION_UP:
                if (this.tracking && !this.swiped && Math.abs(event.getRawX() - this.downX) <= this.touchSlop
                        && Math.abs(event.getRawY() - this.downY) <= this.touchSlop) {
                    invokeOnClick(this.onClick);
                }
                this.tracking = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                this.tracking = false;
                break;
            default:
                break;
        }
        return Boolean.TRUE;
    }

    private static void invokeOnClick(Object onClick) {
    }

    private static void switchAccount(int step, Rect avatarBounds) {
        final Object session = mailSession;
        if (session == null) {
            Log.w(TAG, "No mail session captured");
            return;
        }
        if (!SWITCHING.compareAndSet(false, true)) {
            return;
        }

        new Thread(() -> {
            final TargetAccount target = targetAccount(session, step);
            MAIN.post(() -> {
                try {
                    animateSwitch(session, target, step, avatarBounds);
                } catch (RuntimeException ex) {
                    Log.e(TAG, "Failed to animate the account switch", ex);
                    SWITCHING.set(false);
                }
            });
        }, "hx-account-swipe").start();
    }

    private static void animateSwitch(Object session, TargetAccount target, int step, Rect avatarBounds) {
        final Runnable finishSwitching = () -> SWITCHING.set(false);
        final Runnable setPrimary = () -> new Thread(() -> setPrimaryAccount(session, target.userId),
                "hx-account-swipe")
            .start();
        final Activity activity = PatchContext.resumedActivity();
        final boolean haptics = isHapticFeedbackEnabled();
        final AvatarSwitchStyle style = getSwitchStyle();

        if (activity == null || style == AvatarSwitchStyle.NONE || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (target != null) {
                if (activity != null && haptics) {
                    activity.getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                }
                setPrimary.run();
            }
            finishSwitching.run();
        } else if (target == null) {
            AvatarSwitchAnimation.bounce(activity, avatarBounds, step, haptics, finishSwitching);
        } else {
            AvatarSwitchAnimation.animateToAccount(activity, avatarBounds, step, style, haptics, target.initials,
                    target.color, setPrimary, finishSwitching);
        }
    }

    private static TargetAccount targetAccount(Object session, int step) {
        try {
            final List<Object> accounts = signedInAccounts(session);
            final Object primary = UniffiCalls.okValueOrNull(callSession(session, "getPrimaryAccount"));
            final String primaryUserId = (primary != null) ? (String) UniffiCalls.callNoArg(primary, "userId") : null;
            final int count = accounts.size();
            int primaryIndex = -1;
            for (int index = 0; index < count; index++) {
                if (UniffiCalls.callNoArg(accounts.get(index), "userId").equals(primaryUserId)) {
                    primaryIndex = index;
                }
            }
            if (count < 2 || primaryIndex < 0) {
                return null;
            }
            final Object account = accounts.get((primaryIndex + step + count) % count);
            final Object avatar = UniffiCalls.callNoArg(UniffiCalls.callNoArg(account, "details"),
                    "getAvatarInformation");
            return new TargetAccount((String) UniffiCalls.callNoArg(account, "userId"),
                    (String) UniffiCalls.callNoArg(avatar, "getText"),
                    (String) UniffiCalls.callNoArg(avatar, "getColor"));
        } catch (Throwable throwable) {
            Log.e(TAG, "Failed to read accounts", throwable);
            return null;
        }
    }

    private static void setPrimaryAccount(Object session, String userId) {
        try {
            final Object result = callSession(session, "setPrimaryAccount", userId);
            if (!"Ok".equals(UniffiCalls.simpleNameOf(result))) {
                Log.w(TAG, "setPrimaryAccount returned " + UniffiCalls.simpleNameOf(result));
            }
        } catch (Throwable throwable) {
            Log.e(TAG, "Failed to switch account", throwable);
        }
    }

    private static List<Object> signedInAccounts(Object session) throws Exception {
        final Object accounts = UniffiCalls.okValueOrNull(callSession(session, "getAccounts"));
        if (!(accounts instanceof List)) {
            return new ArrayList<>();
        }

        final Map<String, Object> accountsByEmail = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Object account : (List<?>) accounts) {
            if ("LoggedIn".equals(UniffiCalls.simpleNameOf(UniffiCalls.callNoArg(account, "state")))) {
                final Object details = UniffiCalls.callNoArg(account, "details");
                accountsByEmail.put((String) UniffiCalls.callNoArg(details, "getEmail"), account);
            }
        }
        return new ArrayList<>(accountsByEmail.values());
    }

    private static Object callSession(Object session, String name, Object... args) throws Exception {
        return UniffiCalls.invokeSuspending(UniffiCalls.methodNamed(session.getClass(), name), session, args);
    }

    private static final class TargetAccount {

        private final String userId;

        private final String initials;

        private final String color;

        TargetAccount(String userId, String initials, String color) {
            this.userId = userId;
            this.initials = initials;
            this.color = color;
        }

    }

}
