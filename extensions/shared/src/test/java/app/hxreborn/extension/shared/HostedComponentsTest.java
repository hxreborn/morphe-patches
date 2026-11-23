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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public final class HostedComponentsTest {

    private static final String SCREEN = "app.hxreborn.extension.test.PatchScreen";

    private static final String HOST = "com.example.app.StockActivity";

    private static final String OTHER_HOST = "com.example.app.OtherActivity";

    private static final Map<String, String> HOSTS = Collections.singletonMap(SCREEN, HOST);

    private static final Map<String, String> FACTORY_HOSTS = Collections.singletonMap(HostedScreen.class.getName(),
            StockScreen.class.getName());

    private static final Map<String, String> RECEIVER_HOSTS = Collections.singletonMap(HostedReceiver.class.getName(),
            StockReceiver.class.getName());

    private static Intent screenIntent(Context context) {
        return new Intent().setClassName(context, SCREEN);
    }

    @Test
    public void routesUndeclaredComponentsThroughTheirHost() {
        Context context = RuntimeEnvironment.getApplication();

        Intent routed = HostedComponents.route(context, screenIntent(context), HOSTS);

        assertEquals(new ComponentName(context, HOST), routed.getComponent());
        assertEquals(SCREEN, routed.getAction());
    }

    @Test
    public void opensDeclaredComponentsDirectly() {
        Context context = RuntimeEnvironment.getApplication();
        ActivityInfo info = new ActivityInfo();
        info.packageName = context.getPackageName();
        info.name = SCREEN;
        Shadows.shadowOf(context.getPackageManager()).addOrUpdateActivity(info);

        Intent routed = HostedComponents.route(context, screenIntent(context), HOSTS);

        assertEquals(SCREEN, routed.getComponent().getClassName());
        assertNull(routed.getAction());
    }

    @Test
    @Config(sdk = 27)
    public void opensComponentsDirectlyWithoutComponentFactories() {
        Context context = RuntimeEnvironment.getApplication();

        Intent routed = HostedComponents.route(context, screenIntent(context), HOSTS);

        assertEquals(SCREEN, routed.getComponent().getClassName());
        assertNull(routed.getAction());
    }

    @Test
    public void leavesComponentsWithoutAHostUnchanged() {
        Context context = RuntimeEnvironment.getApplication();

        Intent routed = HostedComponents.route(context, screenIntent(context), Collections.emptyMap());

        assertEquals(SCREEN, routed.getComponent().getClassName());
        assertNull(routed.getAction());
    }

    @Test
    public void leavesOtherPackagesUnchanged() {
        Context context = RuntimeEnvironment.getApplication();
        Intent intent = new Intent().setClassName("com.example.other", SCREEN);

        Intent routed = HostedComponents.route(context, intent, HOSTS);

        assertEquals("com.example.other", routed.getComponent().getPackageName());
        assertNull(routed.getAction());
    }

    @Test
    public void namesTheHostedClassOnlyForItsOwnHost() {
        Intent hosted = new Intent(SCREEN);

        assertEquals(SCREEN, HostedComponents.hostedClass(HOST, hosted, HOSTS));
        assertNull(HostedComponents.hostedClass(OTHER_HOST, hosted, HOSTS));
        assertNull(HostedComponents.hostedClass(HOST, new Intent(), HOSTS));
        assertNull(HostedComponents.hostedClass(HOST, null, HOSTS));
    }

    @Test
    public void readsEveryWellFormedHostEntry() {
        Map<String, String> hosts = HostedComponents
            .parseHosts(SCREEN + "=" + HOST + ";malformed;" + OTHER_HOST + "=" + HOST);

        assertEquals(2, hosts.size());
        assertEquals(HOST, hosts.get(SCREEN));
        assertEquals(HOST, hosts.get(OTHER_HOST));
        assertTrue(HostedComponents.parseHosts("").isEmpty());
    }

    @Test
    public void createsTheRequestedActivityForUnhostedIntents() throws ReflectiveOperationException {
        Activity activity = new HostedComponentFactory().instantiateActivity(getClass().getClassLoader(),
                Activity.class.getName(), new Intent());

        assertSame(Activity.class, activity.getClass());
    }

    @Test
    public void createsTheHostedActivityForItsHost() throws ReflectiveOperationException {
        Activity activity = new HostedComponentFactory().instantiateActivity(getClass().getClassLoader(),
                StockScreen.class.getName(), new Intent(HostedScreen.class.getName()), FACTORY_HOSTS);

        assertSame(HostedScreen.class, activity.getClass());
    }

    @Test
    public void createsTheHostForAnotherHostedAction() throws ReflectiveOperationException {
        Activity activity = new HostedComponentFactory().instantiateActivity(getClass().getClassLoader(),
                StockScreen.class.getName(), new Intent(SCREEN), FACTORY_HOSTS);

        assertSame(StockScreen.class, activity.getClass());
    }

    @Test
    public void createsTheHostedReceiverForItsHost() throws ReflectiveOperationException {
        BroadcastReceiver receiver = new HostedComponentFactory().instantiateReceiver(getClass().getClassLoader(),
                StockReceiver.class.getName(), new Intent(HostedReceiver.class.getName()), RECEIVER_HOSTS);

        assertSame(HostedReceiver.class, receiver.getClass());
    }

    @Test
    @Config(sdk = 27)
    public void reportsUndeclaredHostedComponentsUnreachableWithoutComponentFactories() {
        Context context = RuntimeEnvironment.getApplication();

        assertFalse(HostedComponents.isReachable(context, screenIntent(context), HOSTS));
        assertTrue(HostedComponents.isReachable(context, screenIntent(context), Collections.emptyMap()));
    }

    @Test
    public void reportsHostedComponentsReachableWithComponentFactories() {
        Context context = RuntimeEnvironment.getApplication();

        assertTrue(HostedComponents.isReachable(context, screenIntent(context), HOSTS));
    }

    public static final class HostedScreen extends Activity {

    }

    public static final class StockScreen extends Activity {

    }

    public static final class HostedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
        }

    }

    public static final class StockReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
        }

    }

}
