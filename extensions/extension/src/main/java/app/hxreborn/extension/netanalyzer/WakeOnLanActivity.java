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

import java.io.IOException;

import android.annotation.TargetApi;
import android.os.Build;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.Q)
@SuppressWarnings("unused")
public final class WakeOnLanActivity extends HostScreenActivity {

    static final String EXTRA_MAC = "mac";

    static final String EXTRA_NAME = "name";

    private static final String[] MODES = { "Local", "Remote" };

    private static final int REMOTE_MODE = 1;

    private static final int CONTENT_INSET_DP = 16;

    private static final int FIELD_GAP_DP = 12;

    private static final int ROW_GAP_DP = 8;

    private static final int PORT_WIDTH_DP = 110;

    private static final int SEGMENT_HEIGHT_DP = 40;

    private static final float DISABLED_ALPHA = 0.4f;

    private static final float ENABLED_ALPHA = 1f;

    private FilledField macField;

    private FilledField hostField;

    private FilledField portField;

    private View remoteRow;

    private TextView sendAction;

    private boolean remote;

    @Override
    String screenTitle() {
        return "Wake on LAN";
    }

    @Override
    void buildContent() {
        final LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(CONTENT_INSET_DP), dp(CONTENT_INSET_DP), dp(CONTENT_INSET_DP), dp(CONTENT_INSET_DP));

        this.macField = new FilledField(this, this.palette, "MAC", "MAC Address",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        this.macField.input().setText(getIntent().getStringExtra(EXTRA_MAC));
        column.addView(this.macField, matchWidth());

        final FilledField nameField = new FilledField(this, this.palette, "Name", "Name for history (optional)",
                InputType.TYPE_CLASS_TEXT);
        nameField.input().setText(getIntent().getStringExtra(EXTRA_NAME));
        column.addView(nameField, topGap(FIELD_GAP_DP));

        final LinearLayout.LayoutParams segmentParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(SEGMENT_HEIGHT_DP));
        segmentParams.topMargin = dp(CONTENT_INSET_DP);
        column.addView(segmentedButtons(MODES, 0, (index) -> {
            this.remote = index == REMOTE_MODE;
            refresh();
        }), segmentParams);

        this.hostField = new FilledField(this, this.palette, "Address", "Remote address",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        this.portField = new FilledField(this, this.palette, "Port", "Port", InputType.TYPE_CLASS_NUMBER);
        this.portField.input().setText(String.valueOf(WakeOnLan.DEFAULT_PORT));
        final LinearLayout remoteFields = new LinearLayout(this);
        remoteFields.setOrientation(LinearLayout.HORIZONTAL);
        remoteFields.addView(this.hostField, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final LinearLayout.LayoutParams portParams = new LinearLayout.LayoutParams(dp(PORT_WIDTH_DP),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        portParams.setMarginStart(dp(ROW_GAP_DP));
        remoteFields.addView(this.portField, portParams);
        this.remoteRow = remoteFields;
        column.addView(remoteFields, topGap(CONTENT_INSET_DP));

        final ScrollView scroll = new ScrollView(this);
        scroll.addView(column,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        replaceBody(scroll);

        this.sendAction = addToolbarText("Send", this.palette.startAction, (view) -> send());
        this.macField.onChange(this::refresh);
        this.hostField.onChange(this::refresh);
        refresh();
    }

    private LinearLayout.LayoutParams topGap(int topMarginDp) {
        final LinearLayout.LayoutParams params = matchWidth();
        params.topMargin = dp(topMarginDp);
        return params;
    }

    private void refresh() {
        this.remoteRow.setVisibility(this.remote ? View.VISIBLE : View.INVISIBLE);
        final boolean validMac = WakeOnLan.isValidMac(this.macField.input().getText().toString().trim());
        this.macField.setError(!validMac);
        final boolean missingHost = this.remote && this.hostField.input().getText().toString().trim().isEmpty();
        this.hostField.setError(missingHost);
        final boolean sendable = validMac && !missingHost;
        this.sendAction.setEnabled(sendable);
        this.sendAction.setAlpha(sendable ? ENABLED_ALPHA : DISABLED_ALPHA);
    }

    private void send() {
        final String mac = this.macField.input().getText().toString();
        final boolean toRemote = this.remote;
        final String host = this.hostField.input().getText().toString();
        final int port = parsePort(this.portField.input().getText().toString());
        new Thread(() -> {
            String result = "Wake on LAN packet sent";
            try {
                WakeOnLan.send(mac, toRemote, host, port);
            } catch (IOException ex) {
                result = "Failed to send Wake on LAN packet: " + ex.getMessage();
            }
            final String message = result;
            runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
        }).start();
    }

    private static int parsePort(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            return WakeOnLan.DEFAULT_PORT;
        }
    }

}
