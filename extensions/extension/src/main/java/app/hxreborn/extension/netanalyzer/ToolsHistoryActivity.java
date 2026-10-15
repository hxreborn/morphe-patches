/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

@TargetApi(Build.VERSION_CODES.S_V2)
@SuppressWarnings("unused")
public final class ToolsHistoryActivity extends HostScreenActivity {

    private static final String[] TOOL_NAMES = { "Ping", "Route", "Ports", "Whois", "DNS" };

    private static final String STORED_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    private static final String SHOWN_DATE_PATTERN = "MMM d, H:mm";

    private static final String TOOL_SEPARATOR = ", ";

    private static final int TYPES_TOP_DP = 2;

    @Override
    String screenTitle() {
        return "Tools History";
    }

    @Override
    void buildContent() {
        addToolbarIcon(MaterialIcons.DELETE, "Clear History", (view) -> confirmClear());
        populate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        populate();
    }

    private void populate() {
        this.list.removeAllViews();
        final List<JSONObject> entries = ToolsHistory.load(this);
        if (entries.isEmpty()) {
            introRow("Tap the New button above to perform ping, traceroute, port scan, whois, or DNS query");
        }
        final List<JSONObject> starred = new ArrayList<>();
        final List<JSONObject> others = new ArrayList<>();
        for (JSONObject entry : entries) {
            (entry.optBoolean(ToolsHistory.FAVORITE) ? starred : others).add(entry);
        }
        section("Starred");
        starred.forEach(this::row);
        section("History");
        others.forEach(this::row);
    }

    private void row(JSONObject entry) {
        final boolean favorite = entry.optBoolean(ToolsHistory.FAVORITE);
        final LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.addView(HistoryRows.titleLine(this, entry.optString(ToolsHistory.ADDRESS), date(entry)), matchWidth());
        final TextView types = singleLine(text("", BODY_MEDIUM, this.palette.rowSubsubtitle));
        types.setText(tools(entry));
        final LinearLayout.LayoutParams typesParams = matchWidth();
        typesParams.topMargin = dp(TYPES_TOP_DP);
        content.addView(types, typesParams);
        final LinearLayout row = HistoryRows.row(this, favorite, (view) -> {
            ToolsHistory.setFavorite(this, entry, !favorite);
            populate();
        }, content);
        row.setOnClickListener((view) -> {
            if (ToolsHistory.open(entry)) {
                finish();
            } else {
                Toast.makeText(this, "Open Tools once, then try again", Toast.LENGTH_SHORT).show();
            }
        });
        row.setOnLongClickListener((view) -> {
            showActions(entry);
            return true;
        });
        addRow(row);
    }

    private void showActions(JSONObject entry) {
        new AlertDialog.Builder(this).setTitle(entry.optString(ToolsHistory.ADDRESS))
            .setItems(new String[] { "Delete" }, (dialog, which) -> {
                ToolsHistory.delete(this, entry);
                populate();
            })
            .show();
    }

    private void confirmClear() {
        new AlertDialog.Builder(this).setTitle("Warning")
            .setMessage("Do you want to delete all history items? (Starred items will be preserved.)")
            .setPositiveButton("OK", (dialog, which) -> {
                ToolsHistory.clearUnstarred(this);
                populate();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private CharSequence tools(JSONObject entry) {
        final JSONArray stack = entry.optJSONArray(ToolsHistory.TYPE_STACK);
        final SpannableStringBuilder names = new SpannableStringBuilder();
        for (int i = 0; stack != null && i < stack.length(); i++) {
            final int tool = stack.optInt(i, -1);
            if (tool < 0 || tool >= TOOL_NAMES.length) {
                continue;
            }
            final boolean first = names.length() == 0;
            final String name = first ? TOOL_NAMES[tool] : TOOL_SEPARATOR + TOOL_NAMES[tool].toLowerCase(Locale.ROOT);
            final int start = names.length();
            names.append(name);
            names.setSpan(new ForegroundColorSpan(first ? this.palette.rowSubtitle : this.palette.rowSubsubtitle),
                    start, names.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return names;
    }

    private static String date(JSONObject entry) {
        try {
            return new SimpleDateFormat(SHOWN_DATE_PATTERN, Locale.getDefault())
                .format(new SimpleDateFormat(STORED_DATE_PATTERN, Locale.US).parse(entry.optString(ToolsHistory.DATE)));
        } catch (ParseException ex) {
            return "";
        }
    }

}
