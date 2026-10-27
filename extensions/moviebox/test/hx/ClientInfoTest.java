/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.json.JSONObject;
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
        assertEquals(RuntimeEnvironment.getApplication().getPackageName(), info.getString("package_name"));
    }

    @Test
    public void carriesTheVersionCodeAsANumber() throws Exception {
        // when
        JSONObject info = new JSONObject(ClientInfo.create(RuntimeEnvironment.getApplication()));

        // then
        assertTrue(info.get("version_code") instanceof Number);
    }

    @Test
    public void carriesADeviceIdAndAGaid() throws JSONException, Exception {
        // when
        JSONObject info = new JSONObject(ClientInfo.create(RuntimeEnvironment.getApplication()));

        // then
        assertTrue(info.getString("device_id").matches("[0-9a-f]{32}"));
        assertTrue(info.getString("gaid").matches("[0-9a-f-]{36}"));
        assertTrue(!info.getString("os_version").isEmpty());
    }

    @Test
    public void drawsAFreshIdentityEachTime() throws Exception {
        // given
        String first = ClientInfo.create(RuntimeEnvironment.getApplication());

        // when
        String second = ClientInfo.create(RuntimeEnvironment.getApplication());

        // then
        assertTrue(!new JSONObject(first).getString("device_id").equals(new JSONObject(second).getString("device_id")));
    }

}
