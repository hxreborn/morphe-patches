/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package hx.gateway;

import java.util.UUID;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import hx.platform.InstalledApp;
import hx.util.Logs;
import org.json.JSONException;
import org.json.JSONObject;

final class ClientInfo {

    private ClientInfo() {
    }

    static String forInstalledApp() {
        try {
            return create(InstalledApp.context());
        } catch (ReflectiveOperationException | PackageManager.NameNotFoundException ex) {
            Log.w(Logs.TAG, "client info has no package identity", ex);
            return withoutPackageIdentity();
        }
    }

    static String create(Context application) throws PackageManager.NameNotFoundException {
        JSONObject info = identity();
        try {
            info.put("package_name", application.getPackageName())
                .put("version_code",
                        application.getPackageManager().getPackageInfo(application.getPackageName(), 0).versionCode);
        } catch (JSONException ex) {
            throw new IllegalStateException(ex);
        }
        return info.toString();
    }

    private static String withoutPackageIdentity() {
        return identity().toString();
    }

    private static JSONObject identity() {
        try {
            return new JSONObject().put("device_id", UUID.randomUUID().toString().replace("-", ""))
                .put("os_version", Build.VERSION.RELEASE)
                .put("gaid", UUID.randomUUID().toString());
        } catch (JSONException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
