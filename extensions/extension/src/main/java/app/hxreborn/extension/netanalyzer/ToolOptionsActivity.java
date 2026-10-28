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
package app.hxreborn.extension.netanalyzer;

import java.util.Locale;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.Q)
@SuppressWarnings("unused")
public final class ToolOptionsActivity extends HostScreenActivity {

    private static final String[] PROTOCOL_LABELS = { "ICMP", "UDP", "TCP" };

    private static final int TCP_PROTOCOL = 2;

    private static final int[] PROBE_DELAYS_MS = { 200, 500, 1000, 2000, 5000, 10000, 20000, 30000, 60000, 120000 };

    private static final int MILLIS_PER_SECOND = 1000;

    private static final int DEFAULT_PROTOCOL = 0;

    private static final int DEFAULT_PROBE_DELAY_MS = 1000;

    private static final int DEFAULT_TCP_PORT = 443;

    private static final int MIN_PORT = 1;

    private static final int MAX_PORT = 65535;

    private static final int DEFAULT_PAYLOAD_BYTES = 56;

    private static final int MAX_PAYLOAD_BYTES = 1500;

    private static final int DEFAULT_MAX_DELAY_MS = 1000;

    private static final int MIN_MAX_DELAY_MS = 500;

    private static final int MAX_MAX_DELAY_MS = 10000;

    private static final int MAX_DELAY_STEP_MS = 100;

    private static final int DEFAULT_MIN_DELAY_MS = 50;

    private static final int MIN_MIN_DELAY_MS = 50;

    private static final int MAX_MIN_DELAY_MS = 1000;

    private static final int MIN_DELAY_STEP_MS = 50;

    private static final int DEFAULT_PORTSCAN_DELAY_MS = 1;

    private static final int MIN_PORTSCAN_DELAY_MS = 1;

    private static final int MAX_PORTSCAN_DELAY_MS = 200;

    private static final String DEFAULT_SERVER_HINT = "Default";

    private static final int CONTROL_GAP_DP = 16;

    private static final int VALUE_WIDTH_DP = 64;

    private static final int NUMBER_FIELD_WIDTH_DP = 90;

    private static final int SEGMENT_WIDTH_DP = 240;

    private static final int SEGMENT_HEIGHT_DP = 40;

    private static final int UNDERLINE_DP = 1;

    private static final float HINT_ALPHA = 0.4f;

    private SharedPreferences preferences;

    @Override
    String screenTitle() {
        return "Tool Options";
    }

    @Override
    void buildContent() {
        this.preferences = ToolOptions.preferences(this);
        addToolbarIcon(MaterialIcons.MORE, "More", (view) -> {
            new DropdownMenu(this).add("Reset", null, () -> {
                this.preferences.edit().clear().apply();
                rebuild();
            }).show(view);
        });
        populate();
    }

    private void rebuild() {
        this.list.removeAllViews();
        populate();
    }

    private void populate() {
        section("Ping");
        final int protocol = this.preferences.getInt(ToolOptions.PING_PROTOCOL, DEFAULT_PROTOCOL);
        final LinearLayout protocolRow = controlRow("Protocol");
        final LinearLayout.LayoutParams segmentParams = new LinearLayout.LayoutParams(dp(SEGMENT_WIDTH_DP),
                dp(SEGMENT_HEIGHT_DP));
        segmentParams.setMarginStart(dp(CONTROL_GAP_DP));
        protocolRow.addView(new LinearLayout(this), new LinearLayout.LayoutParams(0, 0, 1f));
        protocolRow.addView(segmentedButtons(PROTOCOL_LABELS, protocol, (index) -> {
            this.preferences.edit().putInt(ToolOptions.PING_PROTOCOL, index).apply();
            rebuild();
        }), segmentParams);
        final int delay = this.preferences.getInt(ToolOptions.PING_DELAY, DEFAULT_PROBE_DELAY_MS);
        sliderRow("Probe delay", PROBE_DELAYS_MS.length - 1, nearestDelayIndex(delay), (position) -> {
            this.preferences.edit().putInt(ToolOptions.PING_DELAY, PROBE_DELAYS_MS[position]).apply();
            return seconds(PROBE_DELAYS_MS[position]);
        });
        if (protocol == TCP_PROTOCOL) {
            numberRow("Port", ToolOptions.PING_TCP_PORT, DEFAULT_TCP_PORT, MIN_PORT, MAX_PORT);
        } else {
            numberRow("Payload size (B)", ToolOptions.PAYLOAD_SIZE, DEFAULT_PAYLOAD_BYTES, 0, MAX_PAYLOAD_BYTES);
            booleanRow("Don't fragment", ToolOptions.DONT_FRAGMENT);
        }
        booleanRow("Audible", ToolOptions.AUDIBLE);

        section("Traceroute");
        millisSliderRow("Max. delay", ToolOptions.MAX_DELAY, DEFAULT_MAX_DELAY_MS, MIN_MAX_DELAY_MS, MAX_MAX_DELAY_MS,
                MAX_DELAY_STEP_MS);
        millisSliderRow("Min. delay", ToolOptions.MIN_DELAY, DEFAULT_MIN_DELAY_MS, MIN_MIN_DELAY_MS, MAX_MIN_DELAY_MS,
                MIN_DELAY_STEP_MS);
        booleanRow("UDP probes", ToolOptions.TRACEROUTE_UDP);

        section("Port Scan");
        final int portscanDelay = Math
            .round(this.preferences.getFloat(ToolOptions.PORTSCAN_DELAY, DEFAULT_PORTSCAN_DELAY_MS));
        sliderRow("Min. delay", MAX_PORTSCAN_DELAY_MS - MIN_PORTSCAN_DELAY_MS, portscanDelay - MIN_PORTSCAN_DELAY_MS,
                (position) -> {
                    final int millis = position + MIN_PORTSCAN_DELAY_MS;
                    this.preferences.edit().putFloat(ToolOptions.PORTSCAN_DELAY, millis).apply();
                    return String.format(Locale.US, "%d ms", millis);
                });

        section("Whois");
        serverRow("Whois server", ToolOptions.WHOIS_SERVER);
        serverRow("RDAP server", ToolOptions.RDAP_SERVER);

        section("DNS");
        booleanRow("Show DNSSEC", ToolOptions.DNSSEC);
        booleanRow("Show all sections", ToolOptions.SHOW_ALL_SECTIONS);
        booleanRow("No recursion", ToolOptions.NO_RECURSION);
    }

    private void millisSliderRow(String title, String key, int fallback, int min, int max, int step) {
        final int value = this.preferences.getInt(key, fallback);
        sliderRow(title, (max - min) / step, Math.round((value - min) / (float) step), (position) -> {
            final int millis = min + position * step;
            this.preferences.edit().putInt(key, millis).apply();
            return seconds(millis);
        });
    }

    private void sliderRow(String title, int steps, int position, SliderValueDescriber value) {
        final LinearLayout row = controlRow(title);
        final MaterialSlider slider = new MaterialSlider(this, this.palette, steps);
        slider.setPosition(position);
        final LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        sliderParams.setMarginStart(dp(CONTROL_GAP_DP));
        row.addView(slider, sliderParams);
        final TextView label = text(value.describe(position), BODY_MEDIUM, this.palette.valueText);
        label.setGravity(Gravity.END);
        row.addView(label, new LinearLayout.LayoutParams(dp(VALUE_WIDTH_DP), ViewGroup.LayoutParams.WRAP_CONTENT));
        slider.setListener((next) -> label.setText(value.describe(next)));
    }

    private void numberRow(String title, String key, int fallback, int min, int max) {
        final LinearLayout row = controlRow(title);
        row.addView(new LinearLayout(this), new LinearLayout.LayoutParams(0, 0, 1f));
        final EditText field = field(String.valueOf(this.preferences.getInt(key, fallback)), "");
        field.setInputType(InputType.TYPE_CLASS_NUMBER);
        field.addTextChangedListener(new AfterTextChanged((text) -> {
            try {
                final int value = Integer.parseInt(text);
                this.preferences.edit().putInt(key, Math.max(min, Math.min(max, value))).apply();
            } catch (NumberFormatException ex) {
                this.preferences.edit().remove(key).apply();
            }
        }));
        row.addView(field,
                new LinearLayout.LayoutParams(dp(NUMBER_FIELD_WIDTH_DP), ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void serverRow(String title, String key) {
        final LinearLayout row = controlRow(title);
        final EditText field = field(this.preferences.getString(key, ""), DEFAULT_SERVER_HINT);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        field.addTextChangedListener(
                new AfterTextChanged((text) -> this.preferences.edit().putString(key, text.trim()).apply()));
        final LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        fieldParams.setMarginStart(dp(CONTROL_GAP_DP));
        row.addView(field, fieldParams);
    }

    private EditText field(String value, String hint) {
        final EditText field = new EditText(this);
        BODY_LARGE.applyTo(field);
        field.setTextColor(this.palette.keyText);
        field.setHintTextColor(withAlpha(this.palette.valueText, HINT_ALPHA));
        field.setHint(hint);
        field.setText(value);
        field.setSingleLine(true);
        field.setGravity(Gravity.END);
        final GradientDrawable underline = new GradientDrawable();
        underline.setColor(this.palette.onSurfaceVariant);
        final LayerDrawable background = new LayerDrawable(new GradientDrawable[] { underline });
        background.setLayerGravity(0, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL);
        background.setLayerHeight(0, dp(UNDERLINE_DP));
        field.setBackground(background);
        return field;
    }

    private void booleanRow(String title, String key) {
        switchRow(title, null, this.preferences.getBoolean(key, false),
                (checked) -> this.preferences.edit().putBoolean(key, checked).apply());
    }

    private static int nearestDelayIndex(int millis) {
        int nearest = 0;
        for (int i = 1; i < PROBE_DELAYS_MS.length; i++) {
            if (Math.abs(PROBE_DELAYS_MS[i] - millis) < Math.abs(PROBE_DELAYS_MS[nearest] - millis)) {
                nearest = i;
            }
        }
        return nearest;
    }

    private static String seconds(int millis) {
        return String.format(Locale.US, "%.1f s", millis / (float) MILLIS_PER_SECOND);
    }

}
