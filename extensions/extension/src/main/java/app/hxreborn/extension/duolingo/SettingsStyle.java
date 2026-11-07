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
package app.hxreborn.extension.duolingo;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Checkable;
import android.widget.Switch;
import android.widget.TextView;

final class SettingsStyle {

    static final int SECTION_SP = 14;

    static final int TITLE_SP = 22;

    static final int BAR_TITLE_SP = 24;

    static final int ROW_TITLE_SP = 17;

    static final int SUMMARY_SP = 15;

    static final int VALUE_SP = 15;

    static final int ICON_DP = 24;

    static final int MESSAGE_SP = 17;

    static final int BORDER_DP = 2;

    static final int LIP_DP = 4;

    static final int CARD_RADIUS_DP = 16;

    static final int SCREEN_INSET_DP = 16;

    static final int MIN_TOUCH_TARGET_DP = 48;

    private static final int FACE_LAYER = 1;

    private static final int BUTTON_HORIZONTAL_PADDING_DP = 16;

    private static final int SOLID_BUTTON_HEIGHT_DP = 52;

    final Context context;

    final int snow;

    final int polar;

    final int swan;

    final int eel;

    final int wolf;

    final int macaw;

    final int whale;

    final boolean night;

    private final Typeface bold;

    private final Typeface regular;

    SettingsStyle(Context context) {
        this.context = context;
        this.night = (this.context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        this.snow = context.getColor(snowColor());
        this.polar = context.getColor(polarColor());
        this.swan = context.getColor(swanColor());
        this.eel = context.getColor(eelColor());
        this.wolf = context.getColor(wolfColor());
        this.macaw = context.getColor(macawColor());
        this.whale = context.getColor(whaleColor());
        this.bold = font(boldFont(), Typeface.DEFAULT_BOLD);
        this.regular = font(regularFont(), Typeface.DEFAULT);
    }

    static int snowColor() {
        return 0;
    }

    static int polarColor() {
        return 0;
    }

    static int swanColor() {
        return 0;
    }

    static int eelColor() {
        return 0;
    }

    static int wolfColor() {
        return 0;
    }

    static int macawColor() {
        return 0;
    }

    static int whaleColor() {
        return 0;
    }

    static int boldFont() {
        return 0;
    }

    static int regularFont() {
        return 0;
    }

    static int backIconDrawable() {
        return 0;
    }

    int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                this.context.getResources().getDisplayMetrics()));
    }

    TextView boldText(CharSequence text, int sp, int color) {
        return text(text, sp, color, this.bold);
    }

    TextView regularText(CharSequence text, int sp, int color) {
        return text(text, sp, color, this.regular);
    }

    TextView textButton(String text, View.OnClickListener listener) {
        TextView button = boldText(text, MESSAGE_SP, this.macaw);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(MIN_TOUCH_TARGET_DP));
        button.setMinWidth(dp(MIN_TOUCH_TARGET_DP));
        button.setPadding(dp(BUTTON_HORIZONTAL_PADDING_DP), 0, dp(BUTTON_HORIZONTAL_PADDING_DP), 0);
        button.setOnClickListener(listener);
        return button;
    }

    TextView solidButton(String text, View.OnClickListener listener) {
        TextView button = boldText(text, MESSAGE_SP, this.snow);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(SOLID_BUTTON_HEIGHT_DP));
        button.setPadding(dp(BUTTON_HORIZONTAL_PADDING_DP), 0, dp(BUTTON_HORIZONTAL_PADDING_DP), dp(LIP_DP));
        button.setBackground(lippedBackground(this.macaw, this.whale, this.macaw));
        button.setOnClickListener(listener);
        return button;
    }

    GradientDrawable rounded(int fill, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    GradientDrawable outlined(int fill, int stroke, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setStroke(dp(BORDER_DP), stroke);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    RippleDrawable pressedFeedback() {
        return new RippleDrawable(ColorStateList.valueOf(this.swan), null, new ColorDrawable(Color.WHITE));
    }

    Drawable backIcon(int tint) {
        return tinted(this.context.getDrawable(backIconDrawable()), tint);
    }

    Drawable hostIcon(String name, int tint) {
        int id = this.context.getResources().getIdentifier(name, "drawable", this.context.getPackageName());
        return tinted((id != 0) ? this.context.getDrawable(id) : null, tint);
    }

    private static Drawable tinted(Drawable icon, int tint) {
        if (icon == null) {
            return null;
        }
        icon = icon.mutate();
        icon.setTint(tint);
        return icon;
    }

    static void markHeading(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.setAccessibilityHeading(true);
        }
    }

    View divider() {
        View divider = new View(this.context);
        divider.setBackgroundColor(this.swan);
        return divider;
    }

    private LayerDrawable lippedBackground(int face, int lip, int border) {
        GradientDrawable lipLayer = outlined(lip, lip, CARD_RADIUS_DP);
        GradientDrawable faceLayer = outlined(face, border, CARD_RADIUS_DP);
        LayerDrawable drawable = new LayerDrawable(new Drawable[] { lipLayer, faceLayer });
        drawable.setLayerInset(FACE_LAYER, 0, 0, 0, dp(LIP_DP));
        return drawable;
    }

    private TextView text(CharSequence text, int sp, int color, Typeface typeface) {
        TextView view = new TextView(this.context);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(color);
        view.setTypeface(typeface);
        return view;
    }

    private Typeface font(int id, Typeface fallback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return fallback;
        }
        try {
            return this.context.getResources().getFont(id);
        } catch (Resources.NotFoundException ex) {
            Log.w(PatchSettings.LOG_TAG, "Font lookup failed for " + id, ex);
            return fallback;
        }
    }

    static final class Toggle extends View implements Checkable {

        private static final int WIDTH_DP = 56;

        private static final int HEIGHT_DP = 32;

        private static final int TRACK_HEIGHT_DP = 19;

        private static final int THUMB_WIDTH_DP = 30;

        private static final int THUMB_RADIUS_DP = 8;

        private final SettingsStyle style;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final RectF rect = new RectF();

        private boolean checked;

        Toggle(SettingsStyle style, boolean checked) {
            super(style.context);
            this.style = style;
            this.checked = checked;
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(this.style.dp(WIDTH_DP), this.style.dp(HEIGHT_DP));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int accent = this.checked ? this.style.macaw : this.style.swan;
            float width = getWidth();
            float height = getHeight();
            float trackHeight = this.style.dp(TRACK_HEIGHT_DP);
            float thumbWidth = this.style.dp(THUMB_WIDTH_DP);
            float border = this.style.dp(SettingsStyle.BORDER_DP);
            float lip = this.style.dp(SettingsStyle.LIP_DP);
            float radius = this.style.dp(THUMB_RADIUS_DP);

            this.paint.setColor(accent);
            this.rect.set(0, (height - trackHeight) / 2, width, (height + trackHeight) / 2);
            canvas.drawRoundRect(this.rect, trackHeight / 2, trackHeight / 2, this.paint);

            float thumbLeft = this.checked ? width - thumbWidth : 0;
            this.rect.set(thumbLeft, 0, thumbLeft + thumbWidth, height);
            canvas.drawRoundRect(this.rect, radius, radius, this.paint);
            this.paint.setColor(this.style.snow);
            this.rect.set(thumbLeft + border, border, thumbLeft + thumbWidth - border, height - lip);
            canvas.drawRoundRect(this.rect, radius - border, radius - border, this.paint);
        }

        @Override
        public boolean isChecked() {
            return this.checked;
        }

        @Override
        public void setChecked(boolean checked) {
            this.checked = checked;
            invalidate();
        }

        @Override
        public void toggle() {
            setChecked(!this.checked);
        }

        static void describeAsSwitch(View row, Checkable toggle) {
            row.setAccessibilityDelegate(new AccessibilityDelegate() {
                @Override
                public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(view, info);
                    info.setClassName(Switch.class.getName());
                    info.setCheckable(true);
                    info.setChecked(toggle.isChecked());
                }
            });
        }

    }

}
