/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import android.content.Context;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

@SuppressWarnings("unused")
public final class MaterialSwitchView {

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
        return function(functionType, new UpdateHandler(checked, onCheckedChange, enabled));
    }

    static void invokeCallback(Object onCheckedChange, boolean checked) {
    }

    private static Object function(Class<?> functionType, FunctionHandler handler) {
        return Proxy.newProxyInstance(MaterialSwitchView.class.getClassLoader(), new Class<?>[] { functionType },
                handler);
    }

    private static Switch createSwitch(Context context) {
        final Switch control = new Switch(context);
        SwitchStyle.apply(control, AccentColor.getAccentColor(PatchesTheme.isNightMode(context)));
        return control;
    }

    private abstract static class FunctionHandler implements InvocationHandler {

        @Override
        public final Object invoke(Object proxy, Method method, Object[] args) {
            final String name = method.getName();
            if ("toString".equals(name)) {
                return getClass().getSimpleName();
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            return apply(args[0]);
        }

        abstract Object apply(Object argument);

    }

    private static final class FactoryHandler extends FunctionHandler {

        @Override
        Object apply(Object context) {
            return createSwitch((Context) context);
        }

    }

    private static final class UpdateHandler extends FunctionHandler {

        private final boolean checked;

        private final Object onCheckedChange;

        private final boolean enabled;

        UpdateHandler(boolean checked, Object onCheckedChange, boolean enabled) {
            this.checked = checked;
            this.onCheckedChange = onCheckedChange;
            this.enabled = enabled;
        }

        @Override
        Object apply(Object view) {
            final Switch control = (Switch) view;
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
