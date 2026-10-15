/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.S_V2)
@SuppressWarnings("unused")
public final class SpeedHistoryActivity extends HostScreenActivity {

    private static final String DOWNLOAD_LABEL = "Download:";

    private static final String UPLOAD_LABEL = "Upload:";

    private static final int ROW_HORIZONTAL_DP = 16;

    private static final int ROW_VERTICAL_DP = 6;

    private static final int NAME_GAP_DP = 8;

    private static final int LOCATION_END_DP = 4;

    private static final int LINE_TOP_DP = 2;

    private static final int SWATCH_DP = 10;

    private static final int SWATCH_RADIUS_DP = 3;

    private static final int LABEL_MIN_DP = 75;

    private static final int VALUE_MIN_DP = 95;

    private static final int BITS_END_DP = 4;

    @Override
    String screenTitle() {
        return "Speed History";
    }

    @Override
    void buildContent() {
        addToolbarIcon(MaterialIcons.DELETE, "Clear History", (view) -> confirmClear());
        addToolbarIcon(MaterialIcons.MORE, "More Options",
                (view) -> ExportMenu.show(this, view, new SpeedHistoryExport()));
        populate();
    }

    private void populate() {
        this.list.removeAllViews();
        final List<SpeedHistory.Entry> entries = SpeedHistory.load(this);
        if (entries.isEmpty()) {
            introRow("Run a speed test to see results here.");
            return;
        }
        section("History");
        final DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
        for (int i = entries.size() - 1; i >= 0; i--) {
            addRow(historyRow(entries.get(i), format));
        }
    }

    private LinearLayout historyRow(SpeedHistory.Entry entry, DateFormat format) {
        final LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setBackground(rowBackground());
        column.setPaddingRelative(dp(ROW_HORIZONTAL_DP), dp(ROW_VERTICAL_DP), dp(ROW_HORIZONTAL_DP),
                dp(ROW_VERTICAL_DP));

        final LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(singleLine(text(entry.connectionType, BODY_MEDIUM, this.palette.rowSubtitle)));
        final TextView location = singleLine(text(entry.name, BODY_MEDIUM, this.palette.valueText));
        location.setPaddingRelative(dp(NAME_GAP_DP), location.getPaddingTop(), dp(LOCATION_END_DP),
                location.getPaddingBottom());
        header.addView(location, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final TextView date = singleLine(
                text(format.format(new Date(entry.date)), BODY_MEDIUM, this.palette.rowSubsubtitle));
        date.setGravity(Gravity.END);
        header.addView(date);
        column.addView(header, matchWidth());

        column.addView(speedLine(DOWNLOAD_LABEL, this.palette.green, entry.downloadBytesPerSecond), lineParams());
        column.addView(speedLine(UPLOAD_LABEL, this.palette.red, entry.uploadBytesPerSecond), lineParams());
        return column;
    }

    private LinearLayout.LayoutParams lineParams() {
        final LinearLayout.LayoutParams params = matchWidth();
        params.topMargin = dp(LINE_TOP_DP);
        return params;
    }

    private LinearLayout speedLine(String label, int color, long bytesPerSecond) {
        final LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        final View swatch = new View(this);
        final GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(SWATCH_RADIUS_DP));
        swatch.setBackground(shape);
        line.addView(swatch, new LinearLayout.LayoutParams(dp(SWATCH_DP), dp(SWATCH_DP)));
        final TextView labelView = singleLine(text(label, BODY_MEDIUM, this.palette.rowTitle));
        labelView.setMinWidth(dp(LABEL_MIN_DP));
        final LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.setMarginStart(dp(NAME_GAP_DP));
        line.addView(labelView, labelParams);
        final TextView bits = singleLine(text(SpeedFormat.bits(bytesPerSecond), BODY_MEDIUM, this.palette.valueText));
        bits.setMinWidth(dp(VALUE_MIN_DP));
        bits.setPaddingRelative(0, bits.getPaddingTop(), dp(BITS_END_DP), bits.getPaddingBottom());
        line.addView(bits);
        final TextView bytes = singleLine(text(SpeedFormat.bytes(bytesPerSecond), BODY_MEDIUM, this.palette.valueText));
        bytes.setMinWidth(dp(VALUE_MIN_DP));
        line.addView(bytes);
        return line;
    }

    private void confirmClear() {
        new AlertDialog.Builder(this).setTitle("Warning")
            .setMessage("Do you want to delete all history items?")
            .setPositiveButton("OK", (dialog, which) -> {
                SpeedHistory.clear(this);
                populate();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

}
