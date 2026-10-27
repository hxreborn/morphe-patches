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
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import app.hxreborn.extension.shared.ProxyHandler;

@SuppressWarnings("unused")
public final class MaterialSwitchView {

    private static final long COLOR_SPACE_MASK = 0x3F;

    private static volatile Object factory;

    private MaterialSwitchView() {

    }

    public static synchronized Object factory(Class<?> functionType) {
        if (factory == null) {
            factory = function(functionType, new FactoryHandler());
        }
        return factory;
    }

    public static Object update(Class<?> functionType, boolean checked, Object onCheckedChange, boolean enabled) {
        return update(functionType, checked, onCheckedChange, enabled, 0);
    }

    public static Object update(Class<?> functionType, boolean checked, Object onCheckedChange, boolean enabled,
            int primaryColor) {
        return function(functionType, new UpdateHandler(checked, onCheckedChange, enabled, primaryColor));
    }

    public static int composeColorArgb(long color) {
        return ((color & COLOR_SPACE_MASK) == 0) ? (int) (color >>> 32) : 0;
    }

    static void invokeCallback(Object onCheckedChange, boolean checked) {
    }

    private static Object function(Class<?> functionType, FunctionHandler handler) {
        return Proxy.newProxyInstance(MaterialSwitchView.class.getClassLoader(), new Class<?>[] { functionType },
                handler);
    }

    private static void applyStyleIfColorChanged(Switch control, int primaryColor) {
        final Context context = control.getContext();
        final boolean night = PatchesTheme.isAppNightMode(context);
        final int color = (primaryColor != 0) ? primaryColor : AccentColor.getAccentColor(night);
        final Long styleKey = ((long) color << 1) | (night ? 1 : 0);
        if (!styleKey.equals(control.getTag())) {
            control.setTag(styleKey);
            SwitchStyle.apply(control, color);
        }
    }

    private abstract static class FunctionHandler extends ProxyHandler {

        @Override
        protected final Object handle(Method method, Object[] args) {
            return apply(args[0]);
        }

        abstract Object apply(Object argument);

    }

    private static final class FactoryHandler extends FunctionHandler {

        @Override
        Object apply(Object context) {
            return new Switch((Context) context);
        }

    }

    private static final class UpdateHandler extends FunctionHandler {

        private final boolean checked;

        private final Object onCheckedChange;

        private final boolean enabled;

        private final int primaryColor;

        UpdateHandler(boolean checked, Object onCheckedChange, boolean enabled, int primaryColor) {
            this.checked = checked;
            this.onCheckedChange = onCheckedChange;
            this.enabled = enabled;
            this.primaryColor = primaryColor;
        }

        @Override
        Object apply(Object view) {
            final Switch control = (Switch) view;
            applyStyleIfColorChanged(control, this.primaryColor);
            control.setOnCheckedChangeListener(null);
            control.setChecked(this.checked);
            control.setEnabled(this.enabled);

            final boolean interactive = this.onCheckedChange != null;
            control.setClickable(interactive);
            control.setFocusable(interactive);
            control.setImportantForAccessibility(
                    interactive ? View.IMPORTANT_FOR_ACCESSIBILITY_AUTO : View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            if (interactive) {
                control.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                        button.setOnCheckedChangeListener(null);
                        button.setChecked(UpdateHandler.this.checked);
                        button.setOnCheckedChangeListener(this);
                        invokeCallback(UpdateHandler.this.onCheckedChange, isChecked);
                    }
                });
            }
            return null;
        }

    }

}
