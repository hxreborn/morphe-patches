/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

final class CameraScreenState {

    private static final String LIFECYCLE_STATE = "androidx.lifecycle.Lifecycle$State";

    private static final String SURFACE_REQUEST = "androidx.camera.core.SurfaceRequest";

    private static final String VIEWFINDER_PACKAGE = "androidx.camera.viewfinder.";

    private static Field surfaceField;

    private CameraScreenState() {
    }

    static boolean lifecycleActive(Object owner) {
        if (owner == null) {
            return true;
        }
        final String state = lifecycleState(owner);
        return "STARTED".equals(state) || "RESUMED".equals(state);
    }

    static boolean lifecycleDestroyed(Object owner) {
        return "DESTROYED".equals(lifecycleState(owner));
    }

    private static String lifecycleState(Object owner) {
        if (owner == null) {
            return null;
        }
        try {
            final Object lifecycle = owner.getClass().getMethod("getLifecycle").invoke(owner);
            for (Method method : lifecycle.getClass().getMethods()) {
                if (method.getParameterTypes().length == 0
                        && method.getReturnType().getName().equals(LIFECYCLE_STATE)) {
                    return ((Enum<?>) method.invoke(lifecycle)).name();
                }
            }
        } catch (ReflectiveOperationException | ClassCastException | NullPointerException ex) {
            return null;
        }
        return null;
    }

    static boolean hasVisibleViewfinder(View view) {
        if (view.getClass().getName().startsWith(VIEWFINDER_PACKAGE)) {
            return view.isShown() && view.getGlobalVisibleRect(new Rect());
        }
        if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (hasVisibleViewfinder(group.getChildAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean previewLive(Object cameraViewModel) {
        if (cameraViewModel == null) {
            return true;
        }
        try {
            final Field known = surfaceField;
            if (known != null && known.getDeclaringClass() == cameraViewModel.getClass()) {
                return isSurfaceRequest(currentValue(known.get(cameraViewModel)));
            }
            for (Field field : cameraViewModel.getClass().getDeclaredFields()) {
                if (holdsSurfaceRequest(field, cameraViewModel)) {
                    surfaceField = field;
                    return true;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return false;
        }
        return false;
    }

    private static boolean holdsSurfaceRequest(Field field, Object cameraViewModel) {
        try {
            field.setAccessible(true);
            return isSurfaceRequest(currentValue(field.get(cameraViewModel)));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return false;
        }
    }

    private static Object currentValue(Object holder) throws ReflectiveOperationException {
        if (holder == null || holder instanceof Serializable) {
            return null;
        }
        final Method getValue;
        try {
            getValue = holder.getClass().getMethod("getValue");
        } catch (NoSuchMethodException ex) {
            return null;
        }
        if (getValue.getReturnType() != Object.class) {
            return null;
        }
        getValue.setAccessible(true);
        return getValue.invoke(holder);
    }

    private static boolean isSurfaceRequest(Object value) {
        for (Class<?> type = (value != null) ? value.getClass() : null; type != null; type = type.getSuperclass()) {
            if (type.getName().equals(SURFACE_REQUEST)) {
                return true;
            }
        }
        return false;
    }

}
