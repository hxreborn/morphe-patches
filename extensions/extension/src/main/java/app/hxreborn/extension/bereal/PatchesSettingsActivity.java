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
package app.hxreborn.extension.bereal;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.window.OnBackInvokedDispatcher;

import app.hxreborn.extension.bereal.download.DownloadFolder;

@SuppressWarnings({ "unused", "deprecation" })
public final class PatchesSettingsActivity extends Activity implements PageHost {

    private static final String TAG = "PatchesSettingsActivity";

    private static final int BAR_HEIGHT_DP = 52;

    private static final String BACK_ICON_NAME = "ic_arrow_left";

    private static final long UNDO_WINDOW_MS = 5000;

    private static final String UNDO_DEADLINE_KEY = "undo_deadline_ms";

    private static final int DOWNLOAD_FOLDER_REQUEST = 1;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable undoTimeout = this::expireUndo;

    private SettingsStyle style;

    private SettingsPages pages;

    private PatchCatalog catalog;

    private LinearLayout content;

    private ScrollView scroll;

    private View undoBar;

    private SettingsSnapshot undoSnapshot;

    private long undoDeadline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(SettingsStyle.SCREEN_BACKGROUND);
        getWindow().setNavigationBarColor(SettingsStyle.SCREEN_BACKGROUND);
        this.style = new SettingsStyle(this);
        this.catalog = PatchCatalog.collect();
        this.pages = new SettingsPages(this, this.style, new SettingsRows(this, this.style), this.catalog, this,
                hostVersion());

        final int inset = this.style.dp(SettingsStyle.SCREEN_INSET_DP);
        this.content = new ColumnLayout(this, this.style.dp(SettingsStyle.CONTENT_MAX_DP));
        this.content.setPadding(inset, 0, inset, this.style.dp(88));
        this.scroll = new ScrollView(this);
        this.scroll.addView(this.content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));

        final LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.addView(toolbar());
        page.addView(this.scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        final FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(SettingsStyle.SCREEN_BACKGROUND);
        root.setFitsSystemWindows(true);
        root.addView(page,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(undoBar(), undoBarParams());
        setContentView(root);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    this::handleBack);
        }
        showPage();
        restoreUndo(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.undoSnapshot != null) {
            this.undoSnapshot.writeTo(outState);
            outState.putLong(UNDO_DEADLINE_KEY, this.undoDeadline);
        }
    }

    @Override
    protected void onDestroy() {
        this.handler.removeCallbacks(this.undoTimeout);
        if (isFinishing()) {
            expireUndo();
        }
        super.onDestroy();
    }

    @Override
    @SuppressLint("GestureBackNavigation")
    public void onBackPressed() {
        handleBack();
    }

    @Override
    public void resetToDefaults() {
        if (this.undoSnapshot == null) {
            this.undoSnapshot = SettingsSnapshot.capture();
        }
        SettingsSnapshot.resetToDefaults();
        showPage();
        this.undoBar.setVisibility(View.VISIBLE);
        this.undoDeadline = SystemClock.elapsedRealtime() + UNDO_WINDOW_MS;
        this.handler.removeCallbacks(this.undoTimeout);
        this.handler.postDelayed(this.undoTimeout, UNDO_WINDOW_MS);
    }

    @Override
    public void chooseDownloadFolder() {
        try {
            startActivityForResult(DownloadFolder.pickerIntent(), DOWNLOAD_FOLDER_REQUEST);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Folder picker not found", ex);
        }
    }

    @Override
    public void useDownloadPreset(String folder) {
        final String replacedFolder = PatchSettings.DOWNLOAD_FOLDER.get();
        DownloadFolder.usePreset(folder);
        releaseReplacedFolderGrant(replacedFolder);
        showPage();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != DOWNLOAD_FOLDER_REQUEST || resultCode != RESULT_OK || data == null
                || data.getData() == null) {
            return;
        }
        final String replacedFolder = PatchSettings.DOWNLOAD_FOLDER.get();
        try {
            DownloadFolder.choose(getContentResolver(), data.getData());
        } catch (RuntimeException ex) {
            Log.e(TAG, "Download folder not saved", ex);
        }
        releaseReplacedFolderGrant(replacedFolder);
        showPage();
    }

    private void releaseReplacedFolderGrant(String folder) {
        if (this.undoSnapshot == null || !this.undoSnapshot.holdsFolder(folder)) {
            DownloadFolder.releaseGrantIfUnused(getContentResolver(), folder);
        }
    }

    private void restoreUndo(Bundle state) {
        if (state == null) {
            return;
        }
        final long deadline = state.getLong(UNDO_DEADLINE_KEY);
        final long remaining = Math.min(deadline - SystemClock.elapsedRealtime(), UNDO_WINDOW_MS);
        final SettingsSnapshot snapshot = SettingsSnapshot.readFrom(state);
        if (snapshot == null) {
            return;
        }
        if (remaining <= 0) {
            snapshot.releaseUnusedGrants(getContentResolver());
            return;
        }
        this.undoSnapshot = snapshot;
        this.undoDeadline = deadline;
        this.undoBar.setVisibility(View.VISIBLE);
        this.handler.postDelayed(this.undoTimeout, remaining);
    }

    private void handleBack() {
        finish();
    }

    private void showPage() {
        this.content.removeAllViews();
        this.pages.page(this.content);
    }

    private void expireUndo() {
        this.undoBar.setVisibility(View.GONE);
        if (this.undoSnapshot != null) {
            this.undoSnapshot.releaseUnusedGrants(getContentResolver());
            this.undoSnapshot = null;
        }
    }

    private void undoReset() {
        this.handler.removeCallbacks(this.undoTimeout);
        this.undoBar.setVisibility(View.GONE);
        if (this.undoSnapshot != null) {
            this.undoSnapshot.restore(getContentResolver());
            this.undoSnapshot = null;
            showPage();
        }
    }

    private String hostVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ex) {
            Log.e(TAG, "Host version not read", ex);
            return "";
        }
    }

    private View toolbar() {
        final FrameLayout bar = new FrameLayout(this);
        bar.setMinimumHeight(this.style.dp(BAR_HEIGHT_DP));

        final FrameLayout.LayoutParams backParams = new FrameLayout.LayoutParams(this.style.dp(48), this.style.dp(48),
                Gravity.START | Gravity.CENTER_VERTICAL);
        backParams.leftMargin = this.style.dp(4);
        bar.addView(backButton(), backParams);

        final TextView title = this.style.text("hxreborn patches", SettingsStyle.BAR_TITLE_SP,
                SettingsStyle.PRIMARY_TEXT, this.style.fonts.semibold);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        final FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        titleParams.leftMargin = this.style.dp(56);
        titleParams.rightMargin = this.style.dp(56);
        bar.addView(title, titleParams);
        return bar;
    }

    private View backButton() {
        final ImageView image = this.style.hostIconView(BACK_ICON_NAME);
        final View back;
        if (image != null) {
            image.setPadding(this.style.dp(12), this.style.dp(12), this.style.dp(12), this.style.dp(12));
            back = image;
        } else {
            final TextView glyph = this.style.text("←", SettingsStyle.TITLE_SP, SettingsStyle.PRIMARY_TEXT,
                    this.style.fonts.medium);
            glyph.setGravity(Gravity.CENTER);
            back = glyph;
        }
        back.setBackground(this.style.ripple(Color.TRANSPARENT, 24));
        back.setContentDescription("Back");
        back.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        back.setClickable(true);
        back.setOnClickListener((view) -> handleBack());
        return back;
    }

    private View undoBar() {
        final LinearLayout bar = new ColumnLayout(this,
                this.style.dp(SettingsStyle.CONTENT_MAX_DP - 2 * SettingsStyle.SCREEN_INSET_DP));
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackground(this.style.rounded(SettingsStyle.FIELD_SURFACE, SettingsStyle.FIELD_RADIUS_DP));
        bar.setPadding(this.style.dp(16), 0, 0, 0);
        bar.setVisibility(View.GONE);
        bar.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        bar.addView(
                this.style.text("Defaults restored", SettingsStyle.ROW_TITLE_SP, SettingsStyle.PRIMARY_TEXT,
                        this.style.fonts.regular),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final TextView undo = this.style.text("Undo", SettingsStyle.ROW_TITLE_SP, SettingsStyle.ACCENT,
                this.style.fonts.semibold);
        undo.setGravity(Gravity.CENTER);
        undo.setMinWidth(this.style.dp(64));
        undo.setMinHeight(this.style.dp(48));
        undo.setPadding(this.style.dp(16), 0, this.style.dp(16), 0);
        undo.setBackground(this.style.ripple(Color.TRANSPARENT, SettingsStyle.FIELD_RADIUS_DP));
        undo.setClickable(true);
        undo.setOnClickListener((view) -> undoReset());
        bar.addView(undo);
        this.undoBar = bar;
        return bar;
    }

    private FrameLayout.LayoutParams undoBarParams() {
        final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        final int inset = this.style.dp(SettingsStyle.SCREEN_INSET_DP);
        params.setMargins(inset, 0, inset, this.style.dp(16));
        return params;
    }

}
