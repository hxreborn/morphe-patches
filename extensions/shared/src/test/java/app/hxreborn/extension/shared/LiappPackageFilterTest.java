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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import com.liapp.LiappCaller;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

@RunWith(RobolectricTestRunner.class)
public final class LiappPackageFilterTest {

    private static final String EDITOR_PACKAGE = "com.gmail.heagoo.apkeditor.modpro";

    private static final String SYSTEM_PACKAGE = "com.android.settings";

    private Context context;

    private Object previousPackageManager;

    @Before
    public void installThePackages() throws ReflectiveOperationException {
        this.context = RuntimeEnvironment.getApplication();
        this.previousPackageManager = packageManagerField().get(null);
        givenInstalled(EDITOR_PACKAGE, 0);
        givenInstalled(SYSTEM_PACKAGE, ApplicationInfo.FLAG_SYSTEM);
    }

    @After
    public void restoreThePackageManager() throws ReflectiveOperationException {
        packageManagerField().set(null, this.previousPackageManager);
    }

    @Test
    public void hidesAnInstalledAppFromLiapp() throws Exception {
        // given
        LiappPackageFilter.install(this.context);

        // when
        Object info = LiappCaller.call(() -> applicationInfo(filteredPackageManager(), EDITOR_PACKAGE));

        // then
        Assertions.assertThat(info).isNull();
    }

    @Test
    public void showsSystemAppsToLiapp() throws Exception {
        // given
        LiappPackageFilter.install(this.context);

        // when
        Object info = LiappCaller.call(() -> applicationInfo(filteredPackageManager(), SYSTEM_PACKAGE));

        // then
        Assertions.assertThat(info).isInstanceOf(ApplicationInfo.class);
    }

    @Test
    public void showsTheAppItselfToLiapp() throws Exception {
        // given
        String ownPackage = this.context.getPackageName();
        LiappPackageFilter.install(this.context);

        // when
        Object info = LiappCaller.call(() -> applicationInfo(filteredPackageManager(), ownPackage));

        // then
        Assertions.assertThat(info).isInstanceOf(ApplicationInfo.class);
    }

    @Test
    public void showsInstalledAppsToTheAppsOwnCode() throws Exception {
        // given
        LiappPackageFilter.install(this.context);

        // when
        Object info = applicationInfo(filteredPackageManager(), EDITOR_PACKAGE);

        // then
        Assertions.assertThat(((ApplicationInfo) info).packageName).isEqualTo(EDITOR_PACKAGE);
    }

    @Test
    public void filtersLiappLookupsThroughTheContextPackageManager() throws Exception {
        // given
        LiappPackageFilter.install(this.context);
        Object contextBinder = contextPackageManagerField().get(this.context.getPackageManager());

        // when
        Object info = LiappCaller.call(() -> applicationInfo(contextBinder, EDITOR_PACKAGE));

        // then
        Assertions.assertThat(info).isNull();
    }

    @Test
    public void rethrowsTheSystemPackageManagersExceptionUnwrapped() throws Exception {
        // given
        LiappPackageFilter.install(this.context);

        // when
        Throwable thrown = Assertions.catchThrowable(() -> invoke(filteredPackageManager(), "getInstallerPackageName",
                EDITOR_PACKAGE));

        // then
        Assertions.assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
    }

    private void givenInstalled(String packageName, int flags) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;
        packageInfo.applicationInfo = new ApplicationInfo();
        packageInfo.applicationInfo.packageName = packageName;
        packageInfo.applicationInfo.flags = flags;
        Shadows.shadowOf(this.context.getPackageManager()).installPackage(packageInfo);
    }

    private static Object filteredPackageManager() throws ReflectiveOperationException {
        return packageManagerField().get(null);
    }

    private static Object applicationInfo(Object packageManager, String packageName) throws Exception {
        return invoke(packageManager, "getApplicationInfo", packageName, 0L, 0);
    }

    private static Object invoke(Object target, String name, Object... args) throws Exception {
        for (Method method : Class.forName("android.content.pm.IPackageManager").getMethods()) {
            if (method.getName().equals(name) && method.getParameterTypes().length == args.length) {
                try {
                    return method.invoke(target, args);
                } catch (InvocationTargetException ex) {
                    throw (Exception) ex.getCause();
                }
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static Field packageManagerField() throws ReflectiveOperationException {
        Field field = Class.forName("android.app.ActivityThread").getDeclaredField("sPackageManager");
        field.setAccessible(true);
        return field;
    }

    private static Field contextPackageManagerField() throws ReflectiveOperationException {
        Field field = Class.forName("android.app.ApplicationPackageManager").getDeclaredField("mPM");
        field.setAccessible(true);
        return field;
    }

}
