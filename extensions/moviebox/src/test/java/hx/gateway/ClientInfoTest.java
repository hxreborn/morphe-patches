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
package hx.gateway;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import org.assertj.core.api.Assertions;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class ClientInfoTest {

    @Test
    public void namesTheInstalledPackage() throws JSONException, PackageManager.NameNotFoundException {
        // when
        JSONObject info = new JSONObject(ClientInfo.create(RuntimeEnvironment.getApplication()));

        // then
        Assertions.assertThat(info.getString("package_name"))
            .isEqualTo(RuntimeEnvironment.getApplication().getPackageName());
    }

    @Test
    public void carriesTheVersionCodeAsANumber() throws JSONException, PackageManager.NameNotFoundException {
        // when
        JSONObject info = new JSONObject(ClientInfo.create(RuntimeEnvironment.getApplication()));

        // then
        Assertions.assertThat(info.get("version_code") instanceof Number).isTrue();
    }

    @Test
    public void carriesTheInstalledVersionCode() throws JSONException, PackageManager.NameNotFoundException {
        // given
        PackageInfo installed = new PackageInfo();
        installed.packageName = RuntimeEnvironment.getApplication().getPackageName();
        installed.versionCode = 50020133;
        Shadows.shadowOf(RuntimeEnvironment.getApplication().getPackageManager()).installPackage(installed);

        // when
        JSONObject info = new JSONObject(ClientInfo.create(RuntimeEnvironment.getApplication()));

        // then
        Assertions.assertThat(info.getLong("version_code")).isEqualTo(50020133L);
    }

    @Test
    public void carriesADeviceIdAndAGaid() throws JSONException, PackageManager.NameNotFoundException {
        // when
        JSONObject info = new JSONObject(ClientInfo.create(RuntimeEnvironment.getApplication()));

        // then
        Assertions.assertThat(info.getString("device_id").matches("[0-9a-f]{32}")).isTrue();
        Assertions.assertThat(info.getString("gaid").matches("[0-9a-f-]{36}")).isTrue();
        Assertions.assertThat(!info.getString("os_version").isEmpty()).isTrue();
    }

    @Test
    public void drawsAFreshIdentityEachTime() throws JSONException, PackageManager.NameNotFoundException {
        // given
        String first = ClientInfo.create(RuntimeEnvironment.getApplication());

        // when
        String second = ClientInfo.create(RuntimeEnvironment.getApplication());

        // then
        Assertions
            .assertThat(
                    !new JSONObject(first).getString("device_id").equals(new JSONObject(second).getString("device_id")))
            .isTrue();
    }

}
