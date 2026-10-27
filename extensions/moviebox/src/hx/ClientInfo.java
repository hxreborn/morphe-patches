/*
 * Copyright (C) 2026 hxreborn
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
 */
package hx;

import java.util.UUID;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
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
