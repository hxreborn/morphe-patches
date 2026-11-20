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
package app.hxreborn.extension.protonpass;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.view.animation.PathInterpolator;
import androidx.compose.foundation.lazy.LazyListState;
import androidx.compose.runtime.ParcelableSnapshotMutableFloatState;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection;
import androidx.compose.ui.unit.Velocity;

import app.hxreborn.extension.proton.PatchSettings;
import app.hxreborn.extension.shared.ProxyHandler;

@SuppressWarnings("unused")
public final class NavigationBarAutoHide {

    public static final String KEY = "auto_hide_navigation_bar";

    private static final long SHOW_DURATION_MS = 225;

    private static final long HIDE_DURATION_MS = 175;

    private static final float BOTTOM_NAVIGATION_HEIGHT_DP = 56f;

    private static float hiddenDpValue;

    private static float barHeightDp;

    private static ValueAnimator settle;

    private static float settleTarget;

    private static Boolean enabled;

    private static NestedScrollConnection scrollConnection;

    private static boolean awaitingScrolledList;

    private static WeakReference<LazyListState> scrolledList = new WeakReference<>(null);

    private NavigationBarAutoHide() {

    }

    public static boolean isPatched() {
        return false;
    }

    public static boolean isEnabled() {
        if (enabled == null) {
            enabled = isPatched() && PatchSettings.isEnabled(KEY, true);
        }
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        PatchSettings.setEnabled(KEY, enabled);
        NavigationBarAutoHide.enabled = null;
        moveTo(0f);
    }

    public static synchronized NestedScrollConnection scrollConnection() {
        if (scrollConnection == null) {
            scrollConnection = (NestedScrollConnection) Proxy.newProxyInstance(
                    NavigationBarAutoHide.class.getClassLoader(), new Class<?>[] { NestedScrollConnection.class },
                    new ScrollHandler());
        }
        return scrollConnection;
    }

    public static float contentBottomPadding(float paddingDp) {
        final boolean barReturned = paddingDp > 0f && barHeightDp <= 0f;
        barHeightDp = paddingDp;
        if (!isEnabled()) {
            return paddingDp;
        }
        if (barReturned) {
            moveTo(0f);
        }
        return paddingDp - Math.min(HiddenOffset.hiddenDp.getFloatValue(), paddingDp);
    }

    public static Modifier contentModifier(Modifier padded, Modifier sizeAnimated) {
        return (isEnabled() && HiddenOffset.hiddenDp.getFloatValue() > 0f) ? padded : sizeAnimated;
    }

    public static float barOffsetDp() {
        return (isEnabled()) ? HiddenOffset.hiddenDp.getFloatValue() : 0f;
    }

    public static float snackbarOffsetDp() {
        return (barHeightDp > 0f) ? Math.min(barOffsetDp(), BOTTOM_NAVIGATION_HEIGHT_DP) : 0f;
    }

    public static void onListMeasured(LazyListState list) {
        if (awaitingScrolledList && list.isScrollInProgress()) {
            awaitingScrolledList = false;
            scrolledList = new WeakReference<>(list);
        }
        if (hiddenDpValue > 0f && scrolledList.get() == list && !list.getCanScrollBackward()) {
            animateTo(0f);
        }
    }

    static void onScroll(long consumedOffset, long availableOffset) {
        if (!isEnabled() || barHeightDp <= 0f) {
            return;
        }
        if (offsetY(availableOffset) > 0f) {
            animateTo(0f);
            return;
        }
        final float consumedY = offsetY(consumedOffset);
        if (consumedY != 0f) {
            awaitingScrolledList = true;
        }
        final float density = Resources.getSystem().getDisplayMetrics().density;
        moveTo(hiddenDpValue - consumedY / density);
    }

    static float offsetY(long packedOffset) {
        return Float.intBitsToFloat((int) (packedOffset & 0xFFFFFFFFL));
    }

    static void onScrollEnd() {
        if (!isEnabled() || barHeightDp <= 0f || (settle != null && settle.isRunning())) {
            return;
        }
        if (hiddenDpValue > 0f && hiddenDpValue < barHeightDp) {
            animateTo((hiddenDpValue * 2 >= barHeightDp) ? barHeightDp : 0f);
        }
    }

    private static void moveTo(float hidden) {
        if (settle != null) {
            settle.cancel();
        }
        setHiddenDp(Math.max(0f, Math.min(hidden, barHeightDp)));
    }

    private static void setHiddenDp(float hidden) {
        hiddenDpValue = hidden;
        HiddenOffset.hiddenDp.setFloatValue(hidden);
    }

    private static void animateTo(float target) {
        final float from = hiddenDpValue;
        final boolean settling = settle != null && settle.isRunning();
        if ((settling) ? settleTarget == target : from == target) {
            return;
        }
        if (settling) {
            settle.cancel();
        }
        final boolean showing = target < from;
        settleTarget = target;
        settle = ValueAnimator.ofFloat(from, target);
        settle.setDuration((showing) ? SHOW_DURATION_MS : HIDE_DURATION_MS);
        settle.setInterpolator(
                (showing) ? new PathInterpolator(0f, 0f, 0.2f, 1f) : new PathInterpolator(0.4f, 0f, 1f, 1f));
        settle.addUpdateListener((animator) -> setHiddenDp((float) animator.getAnimatedValue()));
        settle.start();
    }

    private static final class HiddenOffset {

        private static final ParcelableSnapshotMutableFloatState hiddenDp = new ParcelableSnapshotMutableFloatState(0f);

    }

    private static final class ScrollHandler extends ProxyHandler {

        private static final long NO_SCROLL = 0L;

        private Object noVelocity;

        @Override
        protected Object handle(Method method, Object[] args) {
            final String name = method.getName();
            if (name.startsWith("onPostScroll")) {
                onScroll((long) args[0], (long) args[1]);
                return NO_SCROLL;
            }
            if (name.startsWith("onPreScroll")) {
                return NO_SCROLL;
            }
            if (name.startsWith("onPostFling")) {
                onScrollEnd();
            }
            return noVelocity();
        }

        private Object noVelocity() {
            if (this.noVelocity == null) {
                try {
                    final Constructor<Velocity> constructor = Velocity.class.getDeclaredConstructor(long.class);
                    constructor.setAccessible(true);
                    this.noVelocity = constructor.newInstance(NO_SCROLL);
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException(ex);
                }
            }
            return this.noVelocity;
        }

    }

}
