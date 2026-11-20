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
import android.os.Build;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.Q)
@SuppressWarnings("unused")
public final class LanCustomNameActivity extends HostScreenActivity {

    private static final String EXPLANATION = "Since Android SDK 32, which is now required for Play Store app "
            + "distribution, apps aren't allowed to access devices' MAC addresses. This page allows you to assign a "
            + "custom name to an IP address instead.\n\nHowever, if the device's IP address changes, which commonly "
            + "happens when using DHCP, the name will be assigned to the wrong device. Use this feature only when "
            + "you are absolutely sure IP addresses cannot change.\n\nCustom names are kept per Wi-Fi network.";

    private static final String MAC_SEPARATOR = ":";

    private static final String HEX_DIGITS = "[0-9a-fA-F]{12}";

    private static final int CONTENT_INSET_DP = 16;

    private static final int FIELD_GAP_DP = 12;

    private static String network;

    private static String ip;

    private FilledField name;

    private FilledField mac;

    private FilledField vendor;

    static void show(String networkKey, String address) {
        network = networkKey;
        ip = address;
        HostActivity.open(LanCustomNameActivity.class.getName(), null);
    }

    @Override
    String screenTitle() {
        return "Assign Name to IP";
    }

    @Override
    void buildContent() {
        if (network == null || ip == null) {
            finish();
            return;
        }
        final String[] stored = LanNames.get(network, ip);
        final LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(CONTENT_INSET_DP), dp(CONTENT_INSET_DP), dp(CONTENT_INSET_DP), dp(CONTENT_INSET_DP));
        this.name = field(column, "Name", "Custom name", (stored != null) ? stored[LanNames.NAME] : "", 0);
        this.mac = field(column, "MAC", "MAC address (optional)", (stored != null) ? stored[LanNames.MAC] : "",
                dp(FIELD_GAP_DP));
        this.vendor = field(column, "Vendor", "Vendor (optional)", (stored != null) ? stored[LanNames.VENDOR] : "",
                dp(FIELD_GAP_DP));
        this.mac.onChange(() -> this.mac.setError(!isBlank(this.mac) && !validMac(macText())));
        this.mac.setError(!isBlank(this.mac) && !validMac(macText()));

        final TextView explanation = text(EXPLANATION, BODY_MEDIUM, this.palette.onSurfaceVariant);
        final LinearLayout.LayoutParams explanationParams = matchWidth();
        explanationParams.topMargin = dp(CONTENT_INSET_DP);
        column.addView(explanation, explanationParams);

        final ScrollView scroll = new ScrollView(this);
        scroll.addView(column,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        replaceBody(scroll);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.name == null) {
            return;
        }
        final String[] stored = LanNames.get(network, ip);
        final String typedMac = macText();
        final String keptMac = validMac(typedMac) ? typedMac.toUpperCase(Locale.ROOT)
                : ((stored != null) ? stored[LanNames.MAC] : "");
        LanNames.put(network, ip, new String[] { this.name.input().getText().toString(), keptMac,
                this.vendor.input().getText().toString() });
        HostActivity.refreshHostOnResume();
    }

    private FilledField field(LinearLayout column, String label, String hint, String value, int topMargin) {
        final FilledField field = new FilledField(this, this.palette, label, hint, InputType.TYPE_CLASS_TEXT);
        field.input().setText(value);
        final LinearLayout.LayoutParams params = matchWidth();
        params.topMargin = topMargin;
        column.addView(field, params);
        return field;
    }

    private String macText() {
        return this.mac.input().getText().toString().trim();
    }

    private static boolean isBlank(FilledField field) {
        return field.input().getText().toString().trim().isEmpty();
    }

    private static boolean validMac(String text) {
        return text.replace(MAC_SEPARATOR, "").matches(HEX_DIGITS);
    }

}
