/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Network;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.S_V2)
@SuppressWarnings("unused")
public final class BonjourActivity extends BrowserScreenActivity {

    private static final int DESCRIPTION_MAX_LINES = 2;

    private static final int START_DP = 16;

    private static final int END_DP = 8;

    private static final int VERTICAL_DP = 4;

    private static final int DESCRIPTION_TOP_DP = 2;

    private static final int ICON_DP = 24;

    private MdnsBrowser browser;

    private ServiceNames names;

    @Override
    String screenTitle() {
        return "Bonjour";
    }

    @Override
    boolean browsing() {
        return true;
    }

    @Override
    void startBrowser(InetAddress address, Network network, NetworkInterface networkInterface) throws IOException {
        if (this.browser == null || !this.browser.local.equals(address)) {
            this.browser = new MdnsBrowser(address, networkInterface, this::refresh);
        }
        this.browser.start();
    }

    @Override
    void stopBrowser() {
        if (this.browser != null) {
            this.browser.stop();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addToolbarIcon(MaterialIcons.MORE, "More Options",
                (view) -> ExportMenu.show(this, view, new BonjourExport(filteredServices())));
    }

    private List<BonjourService> filteredServices() {
        final List<BonjourService> filtered = new ArrayList<>();
        if (this.browser != null) {
            for (BonjourService service : this.browser.services()) {
                if (matches(ipFilter(), service.address4)) {
                    filtered.add(service);
                }
            }
        }
        return filtered;
    }

    @Override
    void buildContent() {
        if (showUnavailable()) {
            return;
        }
        if (this.names == null) {
            this.names = ServiceNames.open(this);
        }
        final Map<String, String> groups = new TreeMap<>();
        for (BonjourService service : filteredServices()) {
            groups.put(service.type, service.application);
        }
        for (Map.Entry<String, String> group : groups.entrySet()) {
            final String type = group.getKey();
            addRow(groupRow(type, this.names.describe(group.getValue())));
        }
    }

    @Override
    protected void onDestroy() {
        if (this.names != null) {
            this.names.close();
        }
        super.onDestroy();
    }

    private LinearLayout groupRow(String type, String description) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(rowBackground());
        row.setPaddingRelative(dp(START_DP), dp(VERTICAL_DP), dp(END_DP), dp(VERTICAL_DP));
        row.setOnClickListener((view) -> openType(type));

        final LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        final TextView typeView = text(type, BODY_MEDIUM, this.palette.rowSubtitle);
        typeView.setSingleLine(true);
        typeView.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(typeView, matchWidth());
        final TextView descriptionView = text(description, BODY_MEDIUM, this.palette.rowSubsubtitle);
        descriptionView.setMaxLines(DESCRIPTION_MAX_LINES);
        descriptionView.setEllipsize(TextUtils.TruncateAt.END);
        final LinearLayout.LayoutParams descriptionParams = matchWidth();
        descriptionParams.topMargin = dp(DESCRIPTION_TOP_DP);
        texts.addView(descriptionView, descriptionParams);
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final ImageView more = new ImageView(this);
        more.setImageDrawable(new VectorIcon(MaterialIcons.CHEVRON, this.palette.rowSubsubtitle));
        more.setContentDescription("More");
        row.addView(more, new LinearLayout.LayoutParams(dp(ICON_DP), dp(ICON_DP)));
        return row;
    }

    private void openType(String type) {
        final List<BonjourService> selected = new ArrayList<>();
        for (BonjourService service : this.browser.services()) {
            if (type.equals(service.type)) {
                selected.add(service);
            }
        }
        startActivity(new Intent(this, BonjourServiceActivity.class).putExtra(EXTRA_IP, ipFilter())
            .putExtra(EXTRA_ARGUMENT, ScreenArguments.put(selected)));
    }

}
