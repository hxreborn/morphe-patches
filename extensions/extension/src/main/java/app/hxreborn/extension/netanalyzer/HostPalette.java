/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.S_V2)
@SuppressWarnings("unused")
public final class HostPalette {

    static final String PREFERENCES_SUFFIX = "_preferences";

    static final String THEME_MODE_KEY = "ThemeModePreference";

    static final String THEME_LIGHT = "light";

    static final String THEME_DARK = "dark";

    static final String THEME_SYSTEM = "system";

    private static final String TAG = "hxNetAnalyzer";

    private static final String REGISTRATION_SEPARATOR = ";";

    private static final String LIST_SEPARATOR = ",";

    private static final String NAME_SEPARATOR = "=";

    private static final int COMPOSE_ARGB_SHIFT = 32;

    private static final float LIGHT_BAR_LUMINANCE = 0.5f;

    private static final Map<String, Integer> FRAMEWORK_ROLES = new HashMap<>();

    static {
        FRAMEWORK_ROLES.put("primary", android.R.attr.colorAccent);
        FRAMEWORK_ROLES.put("onPrimary", android.R.attr.textColorPrimaryInverse);
        FRAMEWORK_ROLES.put("background", android.R.attr.colorBackground);
        FRAMEWORK_ROLES.put("onSurface", android.R.attr.textColorPrimary);
        FRAMEWORK_ROLES.put("surfaceVariant", android.R.attr.colorBackgroundFloating);
        FRAMEWORK_ROLES.put("onSurfaceVariant", android.R.attr.textColorSecondary);
        FRAMEWORK_ROLES.put("outlineVariant", android.R.attr.colorControlHighlight);
        FRAMEWORK_ROLES.put("surfaceContainerHigh", android.R.attr.colorBackgroundFloating);
        FRAMEWORK_ROLES.put("surfaceContainer", android.R.attr.colorBackgroundFloating);
    }

    private static volatile HostPalette captured;

    private static volatile CustomColors customColors;

    final boolean dark;

    final int accent;

    final int onAccent;

    final int background;

    final int surfaceVariant;

    final int surfaceContainer;

    final int onSurfaceVariant;

    final int keyText;

    final int valueText;

    final int title;

    final int bar;

    final int header;

    final int sectionText;

    final int divider;

    final int buttonBorder;

    final int rowTitle;

    final int rowSubtitle;

    final int rowSubsubtitle;

    final int grid;

    final int inactiveTrack;

    final int green;

    final int red;

    final int yellow;

    final int blue;

    final int grey;

    final int startAction;

    final int flagGateway;

    final int flagScanning;

    final int flagWeb;

    final int flagBonjour;

    final int flagUpnp;

    final int flagIpv6;

    final int flagPingable;

    final int stopAction;

    private HostPalette(Roles roles) {
        final int surfaceContainerHigh = roles.color("surfaceContainerHigh");
        this.dark = Color.luminance(surfaceContainerHigh) < LIGHT_BAR_LUMINANCE;
        this.accent = roles.color("primary");
        this.onAccent = roles.color("onPrimary");
        this.background = roles.color("background");
        this.surfaceVariant = roles.color("surfaceVariant");
        this.surfaceContainer = roles.color("surfaceContainer");
        this.onSurfaceVariant = roles.color("onSurfaceVariant");
        this.keyText = roles.color("onSurface");
        final int outlineVariant = roles.color("outlineVariant");
        this.valueText = custom("valueTextColor", this.keyText);
        this.title = custom("chromeContentColor", this.keyText);
        this.bar = custom("toolbarBgColor", surfaceContainerHigh);
        this.header = custom("headerColor", surfaceContainerHigh);
        this.sectionText = custom("headerTextColor", this.onSurfaceVariant);
        this.divider = this.sectionText;
        this.buttonBorder = custom("outlineVariant", outlineVariant);
        this.rowTitle = custom("rowTitleColor", this.keyText);
        this.rowSubtitle = custom("rowSubtitleColor", this.keyText);
        this.rowSubsubtitle = custom("rowSubsubtitleColor", this.onSurfaceVariant);
        this.grid = custom("gridColor", outlineVariant);
        this.inactiveTrack = custom("inactiveTrackColor", this.surfaceVariant);
        this.green = custom("systemGreen", this.accent);
        this.red = custom("systemRed", this.accent);
        this.yellow = custom("systemYellow", this.accent);
        this.blue = custom("systemBlue", this.onSurfaceVariant);
        this.grey = custom("systemGrey", outlineVariant);
        this.startAction = custom("greenToolbar", this.accent);
        this.stopAction = custom("redToolbar", this.accent);
        this.flagGateway = custom("flagGateway", this.accent);
        this.flagScanning = custom("flagScanning", this.accent);
        this.flagWeb = custom("flagWeb", this.accent);
        this.flagBonjour = custom("flagBonjour", this.accent);
        this.flagUpnp = custom("flagUpnp", this.accent);
        this.flagIpv6 = custom("flagIpv6", this.accent);
        this.flagPingable = custom("flagPingable", this.accent);
    }

    public static void registerCustomColors(String registration) {
        final String[] parts = registration.split(REGISTRATION_SEPARATOR);
        customColors = new CustomColors(parts[0], parts[1].split(LIST_SEPARATOR), fieldNames(parts[2]));
    }

    public static void capture(Object colorScheme, String fields) {
        final Map<String, String> names = fieldNames(fields);
        captured = new HostPalette((role) -> argb(readLong(colorScheme, names, role)));
    }

    static HostPalette of(Context context) {
        final HostPalette palette = captured;
        if (palette != null) {
            return palette;
        }
        return new HostPalette((role) -> themeColor(context, FRAMEWORK_ROLES.get(role)));
    }

    private int custom(String property, int fallback) {
        final CustomColors colors = customColors;
        if (colors == null) {
            return fallback;
        }
        try {
            return colors.read(property, this.dark);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            Log.e(TAG, "Cannot read the custom color " + property, ex);
            return fallback;
        }
    }

    private static Map<String, String> fieldNames(String registration) {
        final Map<String, String> fields = new HashMap<>();
        for (String entry : registration.split(LIST_SEPARATOR)) {
            final String[] names = entry.split(NAME_SEPARATOR, 2);
            fields.put(names[0], names[1]);
        }
        return fields;
    }

    private static long readLong(Object owner, Map<String, String> names, String property) {
        try {
            final String name = names.get(property);
            if (name == null) {
                throw new NoSuchFieldException(property);
            }
            final Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.getLong(owner);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Cannot read " + property + " from " + owner.getClass(), ex);
        }
    }

    private static int themeColor(Context context, int attribute) {
        final TypedArray attributes = context.obtainStyledAttributes(new int[] { attribute });
        try {
            return attributes.getColor(0, Color.TRANSPARENT);
        } finally {
            attributes.recycle();
        }
    }

    private static int argb(long composeColor) {
        return (int) (composeColor >>> COMPOSE_ARGB_SHIFT);
    }

    private interface Roles {

        int color(String role);

    }

    private static final class CustomColors {

        private static final String TOOLBAR_BACKGROUND = "toolbarBgColor";

        private final String holderClass;

        private final String[] instanceFields;

        private final Map<String, String> fields;

        CustomColors(String holderClass, String[] instanceFields, Map<String, String> fields) {
            this.holderClass = holderClass;
            this.instanceFields = instanceFields;
            this.fields = fields;
        }

        int read(String property, boolean dark) throws ReflectiveOperationException {
            return argb(readLong(instance(dark), this.fields, property));
        }

        private Object instance(boolean dark) throws ReflectiveOperationException {
            final Class<?> holder = Class.forName(this.holderClass, false, HostPalette.class.getClassLoader());
            for (String name : this.instanceFields) {
                final Field field = holder.getDeclaredField(name);
                field.setAccessible(true);
                final Object palette = field.get(null);
                final int toolbar = argb(readLong(palette, this.fields, TOOLBAR_BACKGROUND));
                if ((Color.luminance(toolbar) < LIGHT_BAR_LUMINANCE) == dark) {
                    return palette;
                }
            }
            throw new NoSuchFieldException("No " + (dark ? "dark" : "light") + " palette in " + this.holderClass);
        }

    }

}
