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

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

@TargetApi(Build.VERSION_CODES.Q)
@SuppressWarnings("unused")
public final class WifiDetailsActivity extends HostScreenActivity {

    private static final float PLACEHOLDER_ALPHA = 0.4f;

    private static final int ROW_INSET_DP = 16;

    private static final int ROW_VERTICAL_DP = 7;

    private static final int CLEAR_ICON_DP = 20;

    private static final String NOT_AVAILABLE = "N/A";

    private static final int EXTENSION_ELEMENT_ID = 255;

    private static Hotspot selected;

    static void show(Hotspot hotspot) {
        selected = hotspot;
        HostActivity.open(WifiDetailsActivity.class.getName(), null);
    }

    @Override
    String screenTitle() {
        return "Wi-Fi Signal Details";
    }

    @Override
    void buildContent() {
        final Hotspot hotspot = selected;
        if (hotspot == null) {
            finish();
            return;
        }
        section("Network");
        onOffRow("Connected", yesNo(hotspot.connected), Status.of(hotspot.connected, false));
        simpleRow("SSID", hotspot.ssid);
        customNameRow(hotspot);
        simpleRow("BSSID", hotspot.bssid);
        final String vendor = OuiDatabase.vendor(this, hotspot.bssid);
        simpleRow("Vendor", (vendor != null) ? vendor : NOT_AVAILABLE);
        simpleRow("Primary Channel", hotspot.primaryChannel + " (" + hotspot.frequency + "MHz)");
        final int centerFrequency = hotspot.frequencyOfChannel(hotspot.centerChannel);
        simpleRow("Center Channel", hotspot.centerChannel + " (" + centerFrequency + "MHz)");
        simpleRow("Channel Width", hotspot.width + "MHz (" + (centerFrequency - hotspot.width / 2) + "MHz - "
                + (centerFrequency + hotspot.width / 2) + "MHz)");
        simpleRow("Signal Strength", hotspot.level + " dBm");
        if (hotspot.stations != -1 && !hotspot.elementIds.isEmpty()) {
            simpleRow("Channel Utilization", hotspot.utilization + "%");
            simpleRow("Connected Devices", String.valueOf(hotspot.stations));
        }

        section("Properties");
        simpleRow("Capabilities", (hotspot.capabilities != null) ? hotspot.capabilities : NOT_AVAILABLE);
        simpleRow("Encryption", hotspot.protocols.isEmpty() ? NOT_AVAILABLE : hotspot.encryption());
        onOffRow("WPS", yesNo(hotspot.wps), Status.of(hotspot.wps, false));
        if (hotspot.elementIds.isEmpty()) {
            return;
        }
        onOffRow("Single-User Beamforming", yesNo(hotspot.singleUserBeamforming),
                Status.of(hotspot.singleUserBeamforming, false));
        onOffRow("Multi-User Beamforming", yesNo(hotspot.multiUserBeamforming),
                Status.of(hotspot.multiUserBeamforming, false));
        multiValueRow("Max. Speed (MIMO)", maxSpeed(hotspot));
        multiValueRow("Max. Modulation (MCS Index)", maxModulation(hotspot));
        simpleRow("Regulatory Domain", (hotspot.country != null) ? hotspot.country : NOT_AVAILABLE);
        simpleRow("Element IDs", elementIds(hotspot));
    }

    private void customNameRow(Hotspot hotspot) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(this.palette.background);
        row.setPaddingRelative(dp(ROW_INSET_DP), dp(ROW_VERTICAL_DP), dp(ROW_INSET_DP), dp(ROW_VERTICAL_DP));
        row.addView(text("Custom Name", BODY_MEDIUM, this.palette.keyText));

        final EditText input = new EditText(this);
        BODY_MEDIUM.applyTo(input);
        input.setBackground(null);
        input.setMinHeight(0);
        input.setMinimumHeight(0);
        input.setSingleLine(true);
        input.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setTextColor(this.palette.valueText);
        input.setHint("Enter custom name");
        input.setHintTextColor(withAlpha(this.palette.valueText, PLACEHOLDER_ALPHA));
        final String stored = DeviceNames.get(hotspot.bssid);
        input.setText((stored != null) ? stored : "");
        final LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        inputParams.setMarginStart(dp(ROW_INSET_DP));
        row.addView(input, inputParams);

        final ImageView clear = new ImageView(this);
        clear.setImageDrawable(new VectorIcon(MaterialIcons.CLOSE, this.palette.valueText));
        clear.setContentDescription("Clear");
        clear.setOnClickListener((view) -> input.setText(""));
        clear.setVisibility((input.length() > 0) ? View.VISIBLE : View.GONE);
        row.addView(clear, new LinearLayout.LayoutParams(dp(CLEAR_ICON_DP), dp(CLEAR_ICON_DP)));

        input.addTextChangedListener(new AfterTextChanged((text) -> {
            clear.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
            DeviceNames.put(this, hotspot.bssid, text);
            HostActivity.refreshHostOnResume();
        }));
        addRow(row);
    }

    private static String yesNo(boolean value) {
        return value ? "Yes" : "No";
    }

    private static List<String> maxSpeed(Hotspot hotspot) {
        final List<String> lines = new ArrayList<>();
        for (String mode : Hotspot.MODES) {
            final Object speed = hotspot.maxSpeed.get(mode);
            if (speed == null) {
                continue;
            }
            String line = mode + ": " + Math.round(((Number) speed).doubleValue()) + " Mbps";
            final Object streams = hotspot.streams.get(mode);
            if (streams != null && ((Number) streams).intValue() > 1) {
                line += " (" + streams + "x" + streams + ")";
            }
            lines.add(line);
        }
        return lines;
    }

    private static List<String> maxModulation(Hotspot hotspot) {
        final List<String> lines = new ArrayList<>();
        for (String mode : Hotspot.MODES) {
            final Object modulation = hotspot.modulation.get(mode);
            if (modulation != null) {
                lines.add(mode + ": " + modulation + " (" + hotspot.maxMcs.get(mode) + ")");
            }
        }
        return lines;
    }

    private static String elementIds(Hotspot hotspot) {
        final StringBuilder ids = new StringBuilder();
        for (Object id : hotspot.elementIds) {
            if (((Number) id).intValue() == EXTENSION_ELEMENT_ID) {
                for (Object extension : hotspot.extensionIds) {
                    ids.append(", ").append(EXTENSION_ELEMENT_ID).append('(').append(extension).append(')');
                }
            } else {
                ids.append((ids.length() == 0) ? "" : ", ").append(id);
            }
        }
        return ids.toString();
    }

}
