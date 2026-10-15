/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.S_V2)
@SuppressWarnings("unused")
public final class LanScanResultsActivity extends HostScreenActivity {

    private static final String NOT_AVAILABLE = "N/A";

    private static final int ROW_START_DP = 16;

    private static final int ROW_VERTICAL_DP = 7;

    private static final int ROW_END_DP = 8;

    private static final int LINE_TOP_DP = 2;

    private static final int RIGHT_GAP_DP = 8;

    private static final int CHEVRON_DP = 24;

    private static final int BADGE_GAP_DP = 2;

    private static final int BADGE_RADIUS_DP = 2;

    private static final int BADGE_PADDING_DP = 2;

    private static final int BADGE_TEXT_SP = 11;

    private static LanScan scan;

    static void show(LanScan saved) {
        scan = saved;
        HostActivity.open(LanScanResultsActivity.class.getName(), null);
    }

    @Override
    String screenTitle() {
        return (scan != null) ? scan.name : "LAN";
    }

    @Override
    void buildContent() {
        final LanScan saved = scan;
        if (saved == null) {
            finish();
            return;
        }
        addToolbarIcon(MaterialIcons.MORE, "More Options", (view) -> ExportMenu.show(this, view, new LanExport(saved)));
        populate(saved);
    }

    private void populate(LanScan saved) {
        this.list.removeAllViews();
        simpleRow("IP Range", saved.ipStart + " - " + saved.ipEnd);
        section(saved.name + " (" + saved.devices.size() + ")");
        for (LanRecord device : saved.devices) {
            addRow(deviceRow(saved, device));
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (scan != null) {
            populate(scan);
        }
    }

    private LinearLayout deviceRow(LanScan saved, LanRecord device) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(rowBackground());
        row.setPaddingRelative(dp(ROW_START_DP), dp(ROW_VERTICAL_DP), dp(ROW_END_DP), dp(ROW_VERTICAL_DP));

        final LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        final String name = saved.name(device);
        if (name != null && !name.isEmpty() && !name.equals(device.ip())) {
            left.addView(singleLine(text(name, BODY_MEDIUM, this.palette.valueText)), matchWidth());
        }
        final TextView address = singleLine(text("", BODY_MEDIUM, this.palette.valueText));
        address.setText(address(device.ip()));
        left.addView(address, lineParams());
        final String vendor = saved.vendor(this, device);
        if (vendor != null && !vendor.isEmpty()) {
            left.addView(singleLine(text(vendor, BODY_MEDIUM, this.palette.rowSubsubtitle)), lineParams());
        }
        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END);
        final String mac = saved.mac(device);
        final TextView macView = singleLine(
                text((mac != null) ? mac : NOT_AVAILABLE, BODY_MEDIUM, this.palette.rowSubsubtitle));
        macView.setGravity(Gravity.END);
        right.addView(macView, matchWidth());
        right.addView(flags(device), lineParams());
        final LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rightParams.setMarginStart(dp(RIGHT_GAP_DP));
        row.addView(right, rightParams);

        final ImageView details = new ImageView(this);
        details.setImageDrawable(new VectorIcon(MaterialIcons.CHEVRON, this.palette.rowSubsubtitle));
        details.setContentDescription("Details");
        row.addView(details, new LinearLayout.LayoutParams(dp(CHEVRON_DP), dp(CHEVRON_DP)));
        row.setOnClickListener((view) -> LanDeviceActivity.show(device, saved, false, null));
        return row;
    }

    private LinearLayout.LayoutParams lineParams() {
        final LinearLayout.LayoutParams params = matchWidth();
        params.topMargin = dp(LINE_TOP_DP);
        return params;
    }

    private CharSequence address(String ip) {
        final int split = ip.lastIndexOf('.') + 1;
        final SpannableStringBuilder text = new SpannableStringBuilder(ip);
        text.setSpan(new ForegroundColorSpan(this.palette.rowSubsubtitle), 0, split, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return text;
    }

    private LinearLayout flags(LanRecord device) {
        final LinearLayout flags = new LinearLayout(this);
        flags.setOrientation(LinearLayout.HORIZONTAL);
        flags.setGravity(Gravity.END);
        addBadge(flags, device.gateway, 'G', this.palette.flagGateway);
        addBadge(flags, device.scanning, 'S', this.palette.flagScanning);
        addBadge(flags, device.webProtocol != null, 'W', this.palette.flagWeb);
        addBadge(flags, device.bonjour, 'B', this.palette.flagBonjour);
        addBadge(flags, device.upnp, 'U', this.palette.flagUpnp);
        addBadge(flags, !device.addresses6.isEmpty(), '6', this.palette.flagIpv6);
        addBadge(flags, device.pingable, 'P', this.palette.flagPingable);
        return flags;
    }

    private void addBadge(LinearLayout flags, boolean present, char letter, int color) {
        if (!present) {
            return;
        }
        final TextView badge = new TextView(this);
        badge.setText(String.valueOf(letter));
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, BADGE_TEXT_SP);
        badge.setIncludeFontPadding(false);
        badge.setPadding(dp(BADGE_PADDING_DP), 0, dp(BADGE_PADDING_DP), 0);
        final GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(BADGE_RADIUS_DP));
        badge.setBackground(shape);
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        if (flags.getChildCount() > 0) {
            params.setMarginStart(dp(BADGE_GAP_DP));
        }
        flags.addView(badge, params);
    }

}
