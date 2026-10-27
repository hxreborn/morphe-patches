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

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;

@TargetApi(Build.VERSION_CODES.Q)
@SuppressWarnings("unused")
public final class LanHistoryActivity extends HostScreenActivity {

    private static final int BUTTON_ROW_HORIZONTAL_DP = 16;

    private static final int BUTTON_ROW_VERTICAL_DP = 8;

    private static final int BUTTON_HEIGHT_DP = 40;

    private static final int BUTTON_BORDER_DP = 1;

    private static final int BUTTON_GAP_DP = 8;

    private static final String TAG = "hxNetAnalyzer";

    private static final String DATE_PATTERN = "MMM d, H:mm";

    private static final String ADDRESS_SEPARATOR = ".";

    private static final String ADDRESS_PATTERN = "\\.";

    private static final String RANGE_SEPARATOR = " - ";

    private static final int COUNT_GAP_DP = 6;

    private static final int RANGE_END_DP = 4;

    private static final int ACTIVE_DOT_DP = 10;

    private static final int MAX_COMMON_OCTETS = 3;

    private static final int DIALOG_INSET_DP = 24;

    private static final int DIALOG_FIELD_GAP_DP = 8;

    @Override
    String screenTitle() {
        return "LAN Scan History";
    }

    @Override
    void buildContent() {
        addClearHistoryIcon(true, () -> {
            LanHistory.clearUnstarred(this);
            populate();
        });
        addToolbarIcon(MaterialIcons.ALARM, "Wake on LAN",
                (view) -> startActivity(new Intent(this, WakeOnLanActivity.class)));
        if (LiveLanScan.isAvailable()) {
            addToolbarIcon(MaterialIcons.SETTINGS_ETHERNET, "Scan Custom IP Range", (view) -> showCustomRange());
            addToolbarText("Scan", this.palette.startAction, (view) -> {
                if (LiveLanScan.scanNetwork()) {
                    finish();
                }
            });
        }
        populate();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        populate();
    }

    private void showCustomRange() {
        final LanScan current = LiveLanScan.currentScan();
        final LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPaddingRelative(dp(DIALOG_INSET_DP), dp(DIALOG_FIELD_GAP_DP), dp(DIALOG_INSET_DP), 0);
        final FilledField start = rangeField(column, "Start IP", "Start IP address",
                (current != null) ? current.ipStart : "");
        final FilledField end = rangeField(column, "End IP", "End IP address", (current != null) ? current.ipEnd : "");
        final FilledField name = rangeField(column, "Name", "Custom name for history",
                (current != null) ? current.name : "");
        final AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Custom IP Range")
            .setView(column)
            .setPositiveButton("Scan", (button, which) -> {
                if (LiveLanScan.scan(name.input().getText().toString(), rangeAddress(start), rangeAddress(end))) {
                    finish();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
        final Runnable validate = () -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setEnabled(LiveLanScan.isAscending(rangeAddress(start), rangeAddress(end)));
        start.onChange(validate);
        end.onChange(validate);
        validate.run();
        start.input().requestFocus();
    }

    private FilledField rangeField(LinearLayout column, String label, String hint, String value) {
        final FilledField field = new FilledField(this, this.palette, label, hint,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        field.input().setText(value);
        final LinearLayout.LayoutParams params = matchWidth();
        params.bottomMargin = dp(DIALOG_FIELD_GAP_DP);
        column.addView(field, params);
        return field;
    }

    private static InetAddress rangeAddress(FilledField field) {
        return LiveLanScan.parseIpv4(field.input().getText().toString());
    }

    private void discoveryButtons() {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(this.palette.header);
        row.setPaddingRelative(dp(BUTTON_ROW_HORIZONTAL_DP), dp(BUTTON_ROW_VERTICAL_DP), dp(BUTTON_ROW_HORIZONTAL_DP),
                dp(BUTTON_ROW_VERTICAL_DP));
        addDiscoveryButton(row, "Bonjour", BonjourActivity.class);
        addDiscoveryButton(row, "UPnP", UpnpActivity.class);
        if (row.getChildCount() > 0) {
            this.list.addView(row, matchWidth());
        }
    }

    private void addDiscoveryButton(LinearLayout row, String label, Class<?> screen) {
        final Intent intent = new Intent(this, screen);
        if (getPackageManager().resolveActivity(intent, 0) == null) {
            return;
        }
        final TextView button = text(label, TITLE_SMALL, this.palette.onSurfaceVariant);
        button.setGravity(Gravity.CENTER);
        final GradientDrawable outline = new GradientDrawable();
        outline.setCornerRadius(dp(BUTTON_HEIGHT_DP) / 2f);
        outline.setStroke(dp(BUTTON_BORDER_DP), this.palette.buttonBorder);
        button.setBackground(outline);
        button.setOnClickListener((view) -> startActivity(intent));
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(BUTTON_HEIGHT_DP), 1f);
        if (row.getChildCount() > 0) {
            params.setMarginStart(dp(BUTTON_GAP_DP));
        }
        row.addView(button, params);
    }

    private void populate() {
        this.list.removeAllViews();
        final List<JSONObject> entries = LanHistory.load(this);
        if (entries.isEmpty()) {
            introRow("Finished LAN scans appear here");
        }
        discoveryButtons();
        final List<JSONObject> starred = new ArrayList<>();
        final List<JSONObject> others = new ArrayList<>();
        for (JSONObject entry : entries) {
            (entry.optBoolean(LanHistory.FAVORITE) ? starred : others).add(entry);
        }
        final String connected = connectedBssid();
        section("Starred");
        starred.forEach((entry) -> row(entry, connected));
        section("History");
        others.forEach((entry) -> row(entry, connected));
    }

    private void row(JSONObject entry, String connected) {
        final LanScan scan;
        try {
            scan = LanScan.fromJson(entry);
        } catch (JSONException | java.net.UnknownHostException ex) {
            Log.e(TAG, "Cannot read a saved LAN scan", ex);
            return;
        }
        final boolean favorite = entry.optBoolean(LanHistory.FAVORITE);
        final String date = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(new Date(scan.date));
        final LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.addView(HistoryRows.titleLine(this, scan.name, date), matchWidth());
        content.addView(secondLine(scan, connected), lineParams());
        final LinearLayout row = HistoryRows.row(this, favorite, (view) -> {
            LanHistory.setFavorite(this, entry, !favorite);
            populate();
        }, content);
        row.setOnClickListener((view) -> LanScanResultsActivity.show(scan));
        row.setOnLongClickListener((view) -> {
            showActions(entry, scan.name);
            return true;
        });
        addRow(row);
    }

    private LinearLayout secondLine(LanScan scan, String connected) {
        final LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        final TextView range = singleLine(text("", BODY_MEDIUM, this.palette.rowSubsubtitle));
        range.setText(ipRange(scan));
        range.setPaddingRelative(0, range.getPaddingTop(), dp(RANGE_END_DP), range.getPaddingBottom());
        line.addView(range, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        line.addView(singleLine(text(String.valueOf(scan.devices.size()), BODY_MEDIUM, this.palette.rowSubtitle)));
        final boolean active = connected != null && connected.equalsIgnoreCase(scan.bssid);
        final LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(ACTIVE_DOT_DP), dp(ACTIVE_DOT_DP));
        dotParams.setMarginStart(dp(COUNT_GAP_DP));
        line.addView(dot(active ? this.palette.green : this.palette.grey), dotParams);
        return line;
    }

    private CharSequence ipRange(LanScan scan) {
        final String start = scan.ipStart;
        final String end = scan.ipEnd;
        final SpannableStringBuilder text = new SpannableStringBuilder();
        if (scan.customRange) {
            return text.append(start).append(RANGE_SEPARATOR).append(end);
        }
        final String[] startOctets = start.split(ADDRESS_PATTERN);
        final String[] endOctets = end.split(ADDRESS_PATTERN);
        int common = 0;
        while (common < MAX_COMMON_OCTETS && common < startOctets.length && common < endOctets.length
                && startOctets[common].equals(endOctets[common])) {
            common++;
        }
        int prefixLength = 0;
        for (int i = 0; i < common; i++) {
            prefixLength += startOctets[i].length() + ADDRESS_SEPARATOR.length();
        }
        text.append(start.substring(0, prefixLength));
        final int restStart = text.length();
        text.append(start.substring(prefixLength)).append(RANGE_SEPARATOR).append(end.substring(prefixLength));
        text.setSpan(new ForegroundColorSpan(this.palette.rowSubtitle), restStart, text.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return text;
    }

    private String connectedBssid() {
        try {
            final WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            final WifiInfo info = (wifi != null) ? wifi.getConnectionInfo() : null;
            return (info != null) ? info.getBSSID() : null;
        } catch (SecurityException ex) {
            return null;
        }
    }

    private void showActions(JSONObject entry, String name) {
        new AlertDialog.Builder(this).setTitle(name).setItems(new String[] { "Delete" }, (dialog, which) -> {
            LanHistory.delete(this, entry);
            populate();
        }).show();
    }

}
