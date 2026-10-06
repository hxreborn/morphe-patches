/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AppComponentFactory;
import android.content.Intent;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.P)
@SuppressWarnings("unused")
public class PatchesComponentFactory extends AppComponentFactory {

    @Override
    public Activity instantiateActivity(ClassLoader classLoader, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (intent != null && PatchesMenu.ACTION_SHOW_PATCHES_SETTINGS.equals(intent.getAction())) {
            return new PatchesSettingsActivity();
        }
        return super.instantiateActivity(classLoader, className, intent);
    }

}
