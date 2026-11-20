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
package app.hxreborn.extension.proton;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import android.content.Context;

import app.hxreborn.extension.shared.LoadingIndicatorView;
import app.hxreborn.extension.shared.ProxyHandler;

@SuppressWarnings("unused")
public final class LoadingIndicator {

    private static final String KEY = "loading_indicator";

    private static final float FULL_TURN_DEGREES = 360f;

    private static final float CONTAINER_SIZE_DP = 48f;

    private static volatile Object factory;

    private LoadingIndicator() {

    }

    public static boolean isPatched() {
        return false;
    }

    public static boolean isEnabled() {
        return PatchSettings.isFeatureEnabled(isPatched(), KEY);
    }

    static void setEnabled(boolean enabled) {
        PatchSettings.setEnabled(KEY, enabled);
    }

    public static float containerSize(float stockSizeDp) {
        return (isEnabled()) ? CONTAINER_SIZE_DP : stockSizeDp;
    }

    public static synchronized Object factory(Class<?> functionType) {
        if (factory == null) {
            factory = function(functionType, new FactoryHandler());
        }
        return factory;
    }

    public static Object update(Class<?> functionType, boolean indeterminate, float rotationDegrees) {
        return function(functionType, new UpdateHandler(indeterminate, rotationDegrees / FULL_TURN_DEGREES));
    }

    static void show(Object composer, boolean indeterminate, float rotationDegrees) {
    }

    public static void showProgress(Object composer, float progress) {
        show(composer, false, progress * FULL_TURN_DEGREES);
    }

    private static Object function(Class<?> functionType, ProxyHandler handler) {
        return Proxy.newProxyInstance(LoadingIndicator.class.getClassLoader(), new Class<?>[] { functionType },
                handler);
    }

    private static final class FactoryHandler extends ProxyHandler {

        @Override
        protected Object handle(Method method, Object[] args) {
            final Context context = (Context) args[0];
            final LoadingIndicatorView view = new LoadingIndicatorView(context);
            view.setIndicatorColor(AccentColor.getAccentColor(PatchesTheme.isAppNightMode(context)));
            return view;
        }

    }

    private static final class UpdateHandler extends ProxyHandler {

        private final boolean indeterminate;

        private final float progress;

        UpdateHandler(boolean indeterminate, float progress) {
            this.indeterminate = indeterminate;
            this.progress = progress;
        }

        @Override
        protected Object handle(Method method, Object[] args) {
            final LoadingIndicatorView view = (LoadingIndicatorView) args[0];
            view.setIndeterminate(this.indeterminate);
            view.setProgress(this.progress);
            return null;
        }

    }

}
