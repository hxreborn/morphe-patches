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

import android.content.Context;
import android.content.SharedPreferences;
import org.assertj.core.api.Assertions;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class ClientIdentityTest {

    private static final String FALLBACK = "{\"package_name\":\"com.community.oneroom\",\"version_code\":50020133,"
            + "\"device_id\":\"random\",\"gaid\":\"hook-gaid\",\"os_version\":\"16\"}";

    private final SharedPreferences preferences = RuntimeEnvironment.getApplication()
        .getSharedPreferences("client_identity_test", Context.MODE_PRIVATE);

    private final ClientIdentity identity = new ClientIdentity(FALLBACK, this.preferences);

    @Test
    public void sendsTheAppDeviceIdUntilAnUpgradeNotice() throws JSONException {
        // when
        JSONObject info = new JSONObject(this.identity.rewrite("{\"device_id\":\"app-device\",\"gaid\":\"g\"}"));

        // then
        Assertions.assertThat(info.getString("device_id")).isEqualTo("app-device");
    }

    @Test
    public void sendsA32HexDeviceIdAfterAnUpgradeNotice() throws JSONException {
        // given
        this.identity.replaceDeviceId(null);

        // when
        JSONObject info = new JSONObject(this.identity.rewrite("{\"device_id\":\"flagged\",\"gaid\":\"g\"}"));

        // then
        Assertions.assertThat(info.getString("device_id")).matches("[0-9a-f]{32}").isEqualTo(this.identity.deviceId());
    }

    @Test
    public void keepsTheReplacementDeviceIdForTheInstall() {
        // given
        this.identity.replaceDeviceId(null);

        // when
        ClientIdentity relaunched = new ClientIdentity(FALLBACK, this.preferences);

        // then
        Assertions.assertThat(relaunched.deviceId()).isEqualTo(this.identity.deviceId());
    }

    @Test
    public void keepsTheReplacementForTheProcessWithoutPreferences() {
        // given
        ClientIdentity unstored = new ClientIdentity(FALLBACK, null);

        // when
        unstored.replaceDeviceId(null);

        // then
        Assertions.assertThat(unstored.deviceId()).matches("[0-9a-f]{32}");
    }

    @Test
    public void replacesTheReplacementWhenItIsRejectedToo() {
        // given
        this.identity.replaceDeviceId(null);
        String rejected = this.identity.deviceId();

        // when
        this.identity.replaceDeviceId(rejected);

        // then
        Assertions.assertThat(this.identity.deviceId()).isNotEqualTo(rejected).matches("[0-9a-f]{32}");
    }

    @Test
    public void keepsTheReplacementWhenTheAppDeviceIdIsRejectedLater() {
        // given
        this.identity.replaceDeviceId(null);
        String current = this.identity.deviceId();

        // when
        this.identity.replaceDeviceId(null);

        // then
        Assertions.assertThat(this.identity.deviceId()).isEqualTo(current);
    }

    @Test
    public void addsADeviceIdTheAppLeftOut() throws JSONException {
        // when
        JSONObject info = new JSONObject(this.identity.rewrite("{}"));

        // then
        Assertions.assertThat(info.getString("device_id")).isEqualTo("random");
    }

    @Test
    public void addsAGaidTheAppLeftOut() throws JSONException {
        // when
        JSONObject info = new JSONObject(this.identity.rewrite("{\"device_id\":\"flagged\"}"));

        // then
        Assertions.assertThat(info.getString("gaid")).isEqualTo("hook-gaid");
    }

    @Test
    public void addsThePackageIdentityTheAppLeftOut() throws JSONException {
        // when
        JSONObject info = new JSONObject(this.identity.rewrite("{}"));

        // then
        Assertions.assertThat(info.getString("package_name")).isEqualTo("com.community.oneroom");
        Assertions.assertThat(info.getLong("version_code")).isEqualTo(50020133L);
    }

    @Test
    public void keepsTheOtherValuesTheAppSent() throws JSONException {
        // when
        JSONObject info = new JSONObject(this.identity
            .rewrite("{\"package_name\":\"p\",\"version_code\":7,\"device_id\":\"flagged\",\"gaid\":\"app-gaid\"}"));

        // then
        Assertions.assertThat(info.getString("gaid")).isEqualTo("app-gaid");
        Assertions.assertThat(info.getLong("version_code")).isEqualTo(7L);
    }

    @Test
    public void leavesOtherFieldsOut() throws JSONException {
        // when
        JSONObject info = new JSONObject(this.identity.rewrite("{}"));

        // then
        Assertions.assertThat(info.has("os_version")).isFalse();
    }

    @Test
    public void returnsACompleteClientInfoUnchanged() {
        // given
        String complete = "{\"package_name\":\"p\",\"version_code\":7,\"device_id\":\"d\",\"gaid\":\"g\"}";

        // when
        String info = this.identity.rewrite(complete);

        // then
        Assertions.assertThat(info).isSameAs(complete);
    }

    @Test
    public void returnsUnreadableClientInfoUnchanged() {
        // when
        String info = this.identity.rewrite("not json");

        // then
        Assertions.assertThat(info).isEqualTo("not json");
    }

}
