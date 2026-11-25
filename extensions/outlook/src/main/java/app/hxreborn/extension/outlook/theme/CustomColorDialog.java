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
package app.hxreborn.extension.outlook.theme;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

final class CustomColorDialog implements TextWatcher {

    private static final String TITLE = "Custom color";

    private static final String HINT = "#RRGGBB";

    private static final int MAX_LENGTH = 7;

    private static final int SWATCH_SIZE_DP = 32;

    private static final int SWATCH_GAP_DP = 16;

    private static final int CONTENT_TOP_DP = 8;

    private static final int SECTION_GAP_DP = 16;

    private static final int WHEEL_SIZE_DP = 240;

    private static final int SLIDER_HEIGHT_DP = 32;

    private static final int THUMB_RADIUS_DP = 12;

    private static final int THUMB_STROKE_DP = 2;

    private final Context context;

    private final ColorPickedListener listener;

    private final GradientDrawable swatch = new GradientDrawable();

    private final EditText input;

    private final HsvColor pickedColor = new HsvColor();

    private final HueSaturationWheel wheel;

    private final BrightnessSlider brightness;

    private AlertDialog dialog;

    private boolean writingHexFromPicker;

    private CustomColorDialog(Context context, ColorPickedListener listener) {
        this.context = context;
        this.listener = listener;
        this.input = new EditText(context);
        PickerThumb thumb = new PickerThumb(dp(THUMB_RADIUS_DP), dp(THUMB_STROKE_DP));
        this.wheel = new HueSaturationWheel(context, this.pickedColor, thumb, this::showPickedColorAsHex);
        this.brightness = new BrightnessSlider(context, this.pickedColor, thumb, this::showPickedColorAsHex);
    }

    static void show(Context context, int initialColor, ColorPickedListener listener) {
        new CustomColorDialog(context, listener).show(initialColor);
    }

    private void show(int initialColor) {
        this.dialog = new AlertDialog.Builder(this.context).setTitle(TITLE)
            .setView(content(initialColor))
            .setPositiveButton(android.R.string.ok, this::confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .show();
        updateSwatchAndConfirmButton(enteredColor());
    }

    private View content(int initialColor) {
        this.pickedColor.set(initialColor);
        this.swatch.setShape(GradientDrawable.OVAL);
        this.swatch.setColor(initialColor);
        View preview = new View(this.context);
        preview.setBackground(this.swatch);

        this.input.setSingleLine(true);
        this.input.setHint(HINT);
        this.input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        this.input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(MAX_LENGTH) });
        this.input.setText(HexColor.format(initialColor));
        this.input.setSelection(this.input.length());
        this.input.addTextChangedListener(this);

        LinearLayout hexRow = new LinearLayout(this.context);
        hexRow.setOrientation(LinearLayout.HORIZONTAL);
        hexRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams swatchParams = new LinearLayout.LayoutParams(dp(SWATCH_SIZE_DP), dp(SWATCH_SIZE_DP));
        swatchParams.setMarginEnd(dp(SWATCH_GAP_DP));
        hexRow.addView(preview, swatchParams);
        hexRow.addView(this.input, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout layout = new LinearLayout(this.context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = dialogPadding();
        layout.setPaddingRelative(padding, dp(CONTENT_TOP_DP), padding, 0);
        LinearLayout.LayoutParams wheelParams = new LinearLayout.LayoutParams(dp(WHEEL_SIZE_DP), dp(WHEEL_SIZE_DP));
        wheelParams.gravity = Gravity.CENTER_HORIZONTAL;
        layout.addView(this.wheel, wheelParams);
        LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                dp(SLIDER_HEIGHT_DP));
        sliderParams.topMargin = dp(SECTION_GAP_DP);
        layout.addView(this.brightness, sliderParams);
        LinearLayout.LayoutParams hexParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        hexParams.topMargin = dp(SECTION_GAP_DP);
        layout.addView(hexRow, hexParams);
        ScrollView scroll = new ScrollView(this.context);
        scroll.addView(layout);
        return scroll;
    }

    private void showPickedColorAsHex() {
        this.writingHexFromPicker = true;
        try {
            this.input.setText(HexColor.format(this.pickedColor.toColor()));
            this.input.setSelection(this.input.length());
        } finally {
            this.writingHexFromPicker = false;
        }
        redrawPicker();
    }

    private void redrawPicker() {
        this.wheel.invalidate();
        this.brightness.invalidate();
    }

    private void confirm(DialogInterface dialogInterface, int which) {
        int color = enteredColor();
        if (color != HexColor.INVALID) {
            this.listener.onColorPicked(color);
        }
    }

    private int enteredColor() {
        return HexColor.parse(this.input.getText().toString());
    }

    private void updateSwatchAndConfirmButton(int color) {
        Button confirm = this.dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (confirm != null) {
            confirm.setEnabled(color != HexColor.INVALID);
        }
        if (color != HexColor.INVALID) {
            this.swatch.setColor(color);
        }
    }

    private int dialogPadding() {
        TypedValue padding = new TypedValue();
        if (!this.context.getTheme().resolveAttribute(android.R.attr.dialogPreferredPadding, padding, true)) {
            return 0;
        }
        return TypedValue.complexToDimensionPixelSize(padding.data, this.context.getResources().getDisplayMetrics());
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                this.context.getResources().getDisplayMetrics());
    }

    @Override
    public void beforeTextChanged(CharSequence text, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence text, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable text) {
        if (this.dialog == null) {
            return;
        }
        int color = enteredColor();
        updateSwatchAndConfirmButton(color);
        if (!this.writingHexFromPicker && color != HexColor.INVALID) {
            this.pickedColor.set(color);
            redrawPicker();
        }
    }

}
