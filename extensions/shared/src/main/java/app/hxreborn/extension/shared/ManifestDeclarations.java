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
package app.hxreborn.extension.shared;

import java.util.Arrays;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

@SuppressWarnings("unused")
public final class ManifestDeclarations {

    private static final String TAG = "hxManifestDeclarations";

    private ManifestDeclarations() {
    }

    public static boolean isActivityDeclared(Context context, ComponentName component) {
        try {
            context.getPackageManager().getActivityInfo(component, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ex) {
            return false;
        }
    }

    public static boolean isReceiverDeclared(Context context, ComponentName component) {
        try {
            context.getPackageManager().getReceiverInfo(component, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ex) {
            return false;
        }
    }

    public static boolean isServiceDeclared(Context context, ComponentName component) {
        try {
            context.getPackageManager().getServiceInfo(component, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ex) {
            return false;
        }
    }

    public static boolean isProviderDeclared(Context context, String authority) {
        return context.getPackageManager().resolveContentProvider(authority, 0) != null;
    }

    public static boolean isPermissionRequested(Context context, String permission) {
        try {
            String[] requested = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS).requestedPermissions;
            return requested != null && Arrays.asList(requested).contains(permission);
        } catch (PackageManager.NameNotFoundException ex) {
            Log.w(TAG, "Package info lookup failed for " + context.getPackageName(), ex);
            return false;
        }
    }

}
