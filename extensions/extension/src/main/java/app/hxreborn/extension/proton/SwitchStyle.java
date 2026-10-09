/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

import java.util.Arrays;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.StateSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.widget.Switch;

public final class SwitchStyle {

    private static final String WIDGET_STYLE = "Widget.Material3.CompoundButton.MaterialSwitch";

    private static final String COLORS_STYLE = "ThemeOverlay.Patches.MaterialSwitch";

    private static final int ICON_SIZE_DP = 16;

    private static final int[] CHECKED = { android.R.attr.state_checked };

    private static final int[][] TINT_STATE_SPECS = { { -android.R.attr.state_enabled, -android.R.attr.state_checked },
            { -android.R.attr.state_enabled }, { android.R.attr.state_checked, android.R.attr.state_pressed },
            { android.R.attr.state_checked }, { android.R.attr.state_pressed }, StateSet.WILD_CARD, };

    private static final int[][] TINT_STATE_SAMPLES = { {}, { android.R.attr.state_checked },
            { android.R.attr.state_enabled, android.R.attr.state_checked, android.R.attr.state_pressed },
            { android.R.attr.state_enabled, android.R.attr.state_checked },
            { android.R.attr.state_enabled, android.R.attr.state_pressed }, { android.R.attr.state_enabled }, };

    private SwitchStyle() {

    }

    public static void apply(Switch control, int primaryColor) {
        final MaterialStyle style = MaterialStyle.resolve(control.getContext(), primaryColor);
        if (style == null) {
            WebToggleSwitchStyle.apply(control);
            return;
        }
        control.setSplitTrack(false);
        control.setSwitchPadding(style.dimension("switchPadding"));
        control.setThumbDrawable(style.thumb());
        control.setTrackDrawable(style.track());
        control.refreshDrawableState();
    }

    public static Drawable thumb(Context context, int primaryColor) {
        final MaterialStyle style = MaterialStyle.resolve(context, primaryColor);
        return (style != null) ? style.thumb() : null;
    }

    public static Drawable track(Context context, int primaryColor) {
        final MaterialStyle style = MaterialStyle.resolve(context, primaryColor);
        return (style != null) ? style.track() : null;
    }

    private static final class MaterialStyle {

        private final Context context;

        private final int widgetStyle;

        private final int themePrimaryColor;

        private final int primaryColor;

        private MaterialStyle(Context context, int widgetStyle, int themePrimaryColor, int primaryColor) {
            this.context = context;
            this.widgetStyle = widgetStyle;
            this.themePrimaryColor = themePrimaryColor;
            this.primaryColor = primaryColor;
        }

        static MaterialStyle resolve(Context context, int primaryColor) {
            final int widgetStyle = identifier(context, WIDGET_STYLE, "style");
            final int colorsStyle = identifier(context, COLORS_STYLE, "style");
            if (widgetStyle == 0 || colorsStyle == 0) {
                return null;
            }
            final Context themed = new ContextThemeWrapper(context, colorsStyle);
            final TypedValue value = new TypedValue();
            themed.getTheme().resolveAttribute(identifier(context, "colorPrimary", "attr"), value, true);
            return new MaterialStyle(themed, widgetStyle, value.data, primaryColor);
        }

        Drawable thumb() {
            final Drawable shape = new WithIconState(styleDrawable(android.R.attr.thumb),
                    identifier(this.context, "state_with_icon", "attr"));
            shape.setTintList(tint("thumbTint"));

            final StateListDrawable icon = new StateListDrawable();
            icon.addState(CHECKED, drawable("ic_proton_checkmark"));
            icon.addState(StateSet.WILD_CARD, drawable("ic_proton_cross"));
            icon.setTintList(tint("thumbIconTint"));

            final int inset = thumbInset();
            final int iconSize = PatchesTheme.dpToPx(this.context, ICON_SIZE_DP);
            final LayerDrawable thumb = new LayerDrawable(new Drawable[] { shape, icon });
            thumb.setLayerInset(0, -inset, 0, -inset, 0);
            thumb.setLayerInset(1, -inset, 0, -inset, 0);
            thumb.setLayerGravity(1, Gravity.CENTER);
            thumb.setLayerSize(1, iconSize, iconSize);
            return thumb;
        }

        Drawable track() {
            final Drawable shape = styleDrawable(attribute("track"));
            shape.setTintList(tint("trackTint"));
            final Drawable outline = styleDrawable(attribute("trackDecoration"));
            outline.setTintList(tint("trackDecorationTint"));

            final int inset = thumbInset();
            final LayerDrawable track = new LayerDrawable(new Drawable[] { shape, outline });
            track.setPadding(inset, 0, inset, 0);
            return track;
        }

        int dimension(String name) {
            final TypedArray values = styleValues(attribute(name));
            try {
                return values.getDimensionPixelSize(0, 0);
            } finally {
                values.recycle();
            }
        }

        private int thumbInset() {
            return styleDrawable(android.R.attr.thumb).getIntrinsicWidth()
                    - styleDrawable(attribute("track")).getIntrinsicWidth() / 2;
        }

        private ColorStateList tint(String name) {
            final TypedArray values = styleValues(attribute(name));
            final ColorStateList tint;
            try {
                tint = values.getColorStateList(0);
            } finally {
                values.recycle();
            }
            if (tint == null || this.primaryColor == this.themePrimaryColor) {
                return tint;
            }

            final int[] colors = new int[TINT_STATE_SPECS.length];
            for (int index = 0; index < colors.length; index++) {
                final int color = tint.getColorForState(TINT_STATE_SAMPLES[index], tint.getDefaultColor());
                colors[index] = (color == this.themePrimaryColor) ? this.primaryColor : color;
            }
            return new ColorStateList(TINT_STATE_SPECS, colors);
        }

        private Drawable styleDrawable(int attribute) {
            final TypedArray values = styleValues(attribute);
            try {
                return values.getDrawable(0);
            } finally {
                values.recycle();
            }
        }

        private TypedArray styleValues(int attribute) {
            return this.context.getTheme().obtainStyledAttributes(this.widgetStyle, new int[] { attribute });
        }

        private Drawable drawable(String name) {
            return this.context.getDrawable(identifier(this.context, name, "drawable"));
        }

        private int attribute(String name) {
            return identifier(this.context, name, "attr");
        }

        private static int identifier(Context context, String name, String type) {
            return context.getResources().getIdentifier(name, type, context.getPackageName());
        }

    }

    private static final class WithIconState extends DrawableWrapper {

        private final int stateWithIcon;

        WithIconState(Drawable drawable, int stateWithIcon) {
            super(drawable);
            this.stateWithIcon = stateWithIcon;
        }

        @Override
        protected boolean onStateChange(int[] state) {
            final int[] withIconState = Arrays.copyOf(state, state.length + 1);
            withIconState[state.length] = this.stateWithIcon;
            return super.onStateChange(withIconState);
        }

    }

}
