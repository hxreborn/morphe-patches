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
package app.hxreborn.extension.ultimea;

import java.lang.ref.WeakReference;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.ComponentCallbacks;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

final class AccentObserver implements Application.ActivityLifecycleCallbacks, ComponentCallbacks {

    private final Application application;

    private final Handler main = new Handler(Looper.getMainLooper());

    private WeakReference<Activity> activity = new WeakReference<>(null);

    private int renderedAccent;

    private boolean resumed;

    private boolean recreating;

    private boolean paletteChanged;

    AccentObserver(Application application) {
        this.application = application;
        application.registerActivityLifecycleCallbacks(this);
        application.registerComponentCallbacks(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            observeWallpaper();
        }
    }

    void attach(Activity activity) {
        this.activity = new WeakReference<>(activity);
        this.renderedAccent = AccentPalette.primary(this.application, false);
        this.resumed = false;
        this.recreating = false;
        this.paletteChanged = !AccentColor.prepare(activity);
    }

    void changed() {
        this.paletteChanged = true;
        this.main.post(this::refresh);
    }

    private void refresh() {
        int current = AccentPalette.primary(this.application, false);
        if ((current == this.renderedAccent && !this.paletteChanged) || !AccentColor.prepare(this.application)) {
            return;
        }
        Activity target = this.activity.get();
        if (this.resumed && !this.recreating && target != null && !target.isFinishing() && !target.isDestroyed()) {
            this.recreating = true;
            target.recreate();
        }
    }

    @TargetApi(Build.VERSION_CODES.S)
    private void observeWallpaper() {
        this.application.getSystemService(WallpaperManager.class)
            .addOnColorsChangedListener(new WallpaperListener(this), this.main);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        this.main.post(this::refresh);
    }

    @Override
    public void onLowMemory() {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (this.activity.get() == activity) {
            this.resumed = true;
            this.main.post(this::refresh);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (this.activity.get() == activity) {
            this.resumed = false;
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (this.activity.get() == activity) {
            this.activity.clear();
            this.resumed = false;
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle state) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle state) {
    }

    @TargetApi(Build.VERSION_CODES.S)
    private static final class WallpaperListener implements WallpaperManager.OnColorsChangedListener {

        private final AccentObserver observer;

        WallpaperListener(AccentObserver observer) {
            this.observer = observer;
        }

        @Override
        public void onColorsChanged(WallpaperColors colors, int which) {
            this.observer.main.post(this.observer::refresh);
        }

    }

}
