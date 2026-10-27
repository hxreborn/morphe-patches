/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import org.json.JSONException;
import org.json.JSONObject;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class ClientInfoTest {

    @Test
    public void namesTheInstalledPackage() throws Exception {
        // when
        JSONObject info = new JSONObject(ClientInfo.create(RuntimeEnvironment.getApplication()));

        // then
        Assertions.assertThat(info.getString("package_name"))
            .isEqualTo(RuntimeEnvironment.getApplication().getPackageName());
    }

    @Test
    public void carriesTheVersionCodeAsANumber() throws Exception {
        // when
        JSONObject info = new JSONObject(ClientInfo.create(RuntimeEnvironment.getApplication()));

        // then
        Assertions.assertThat(info.get("version_code") instanceof Number).isTrue();
    }

    @Test
    public void carriesADeviceIdAndAGaid() throws JSONException, Exception {
        // when
        JSONObject info = new JSONObject(ClientInfo.create(RuntimeEnvironment.getApplication()));

        // then
        Assertions.assertThat(info.getString("device_id").matches("[0-9a-f]{32}")).isTrue();
        Assertions.assertThat(info.getString("gaid").matches("[0-9a-f-]{36}")).isTrue();
        Assertions.assertThat(!info.getString("os_version").isEmpty()).isTrue();
    }

    @Test
    public void drawsAFreshIdentityEachTime() throws Exception {
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
