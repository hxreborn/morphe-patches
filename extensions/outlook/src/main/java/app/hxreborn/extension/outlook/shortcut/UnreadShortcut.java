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
package app.hxreborn.extension.outlook.shortcut;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import app.hxreborn.extension.outlook.settings.PatchSettings;
import app.hxreborn.extension.shared.ActivityCallbacksAdapter;

@SuppressWarnings("unused")
@TargetApi(Build.VERSION_CODES.N_MR1)
public final class UnreadShortcut {

    private static final String SHORTCUT_ID = "hx_unread";

    private static final List<String> SHORTCUT_IDS = Collections.singletonList(SHORTCUT_ID);

    private static final Uri UNREAD_SEARCH = Uri.parse("ms-outlook://search?querytext=isread:no");

    private static final String OPEN_UNREAD_SEARCH_EXTRA = "app.hxreborn.outlook.OPEN_UNREAD_SEARCH";

    private static final String DISABLED_MESSAGE = "The Unread shortcut is turned off.";

    private static volatile Context applicationContext;

    private static WeakReference<Activity> unreadSearchLauncher = new WeakReference<>(null);

    private UnreadShortcut() {
    }

    public static void update(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return;
        }
        if (applicationContext == null && context instanceof Application) {
            ((Application) context).registerActivityLifecycleCallbacks(new UnreadSearchLauncher());
        }
        applicationContext = context.getApplicationContext();
        ShortcutManager manager = applicationContext.getSystemService(ShortcutManager.class);
        try {
            if (!PatchSettings.addUnreadShortcut()) {
                manager.removeDynamicShortcuts(SHORTCUT_IDS);
                manager.disableShortcuts(SHORTCUT_IDS, DISABLED_MESSAGE);
                return;
            }
            manager.enableShortcuts(SHORTCUT_IDS);
            if (!isPublished(manager) && !manager.addDynamicShortcuts(Collections.singletonList(shortcut()))) {
                Log.w(PatchSettings.LOG_TAG, "The launcher rate-limited the Unread shortcut");
            }
        } catch (IllegalStateException | IllegalArgumentException ex) {
            Log.w(PatchSettings.LOG_TAG, "Cannot update the Unread shortcut", ex);
        }
    }

    public static boolean setDynamicShortcuts(ShortcutManager manager, List<ShortcutInfo> shortcuts) {
        if (applicationContext == null || !PatchSettings.addUnreadShortcut()) {
            return manager.setDynamicShortcuts(shortcuts);
        }
        int room = manager.getMaxShortcutCountPerActivity() - manager.getManifestShortcuts().size() - 1;
        List<ShortcutInfo> withUnread = new ArrayList<>();
        withUnread.add(shortcut());
        withUnread.addAll(shortcuts.subList(0, Math.max(0, Math.min(room, shortcuts.size()))));
        return manager.setDynamicShortcuts(withUnread);
    }

    private static boolean isPublished(ShortcutManager manager) {
        Intent expected = unreadSearchIntent(applicationContext);
        for (ShortcutInfo shortcut : manager.getDynamicShortcuts()) {
            Intent intent = shortcut.getIntent();
            if (SHORTCUT_ID.equals(shortcut.getId()) && intent != null && expected.filterEquals(intent)
                    && expected.getFlags() == intent.getFlags()
                    && intent.getBooleanExtra(OPEN_UNREAD_SEARCH_EXTRA, false)) {
                return true;
            }
        }
        return false;
    }

    private static ShortcutInfo shortcut() {
        Context context = applicationContext;
        return new ShortcutInfo.Builder(context, SHORTCUT_ID).setShortLabel(context.getString(labelId()))
            .setIcon(icon(context))
            .setIntent(unreadSearchIntent(context))
            .setRank(0)
            .build();
    }

    private static Intent unreadSearchIntent(Context context) {
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launch == null) {
            launch = new Intent(Intent.ACTION_VIEW, UNREAD_SEARCH).setPackage(context.getPackageName());
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra(OPEN_UNREAD_SEARCH_EXTRA, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            launch.setIdentifier(SHORTCUT_ID);
        }
        return launch;
    }

    private static Icon icon(Context context) {
        ShortcutManager manager = context.getSystemService(ShortcutManager.class);
        Drawable glyph = context.getDrawable(iconId()).mutate();
        glyph.setTint(context.getColor(colorId()));
        Bitmap bitmap = Bitmap.createBitmap(manager.getIconMaxWidth(), manager.getIconMaxHeight(),
                Bitmap.Config.ARGB_8888);
        glyph.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
        glyph.draw(new Canvas(bitmap));
        return Icon.createWithBitmap(bitmap);
    }

    private static int iconId() {
        return 0;
    }

    private static int labelId() {
        return 0;
    }

    private static int colorId() {
        return 0;
    }

    private static final class UnreadSearchLauncher extends ActivityCallbacksAdapter {

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            Intent intent = activity.getIntent();
            if (savedInstanceState == null && intent != null
                    && intent.getBooleanExtra(OPEN_UNREAD_SEARCH_EXTRA, false)) {
                intent.removeExtra(OPEN_UNREAD_SEARCH_EXTRA);
                unreadSearchLauncher = new WeakReference<>(activity);
            }
        }

        @Override
        public void onActivityResumed(Activity activity) {
            if (unreadSearchLauncher.get() == null || unreadSearchLauncher.get() == activity) {
                return;
            }
            unreadSearchLauncher = new WeakReference<>(null);
            Intent search = new Intent(Intent.ACTION_VIEW, UNREAD_SEARCH).setPackage(activity.getPackageName());
            activity.getWindow().getDecorView().post(() -> activity.startActivity(search));
        }

    }

}
