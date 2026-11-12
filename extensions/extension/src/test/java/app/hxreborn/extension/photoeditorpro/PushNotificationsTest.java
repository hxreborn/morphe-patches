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
package app.hxreborn.extension.photoeditorpro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.app.NotificationManager;
import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class PushNotificationsTest {

    private static final String PLAY_SERVICES_PACKAGE = "com.google.android.gms";

    private static final String OTHER_DELEGATE_PACKAGE = "com.example.delegate";

    private Context context;

    private NotificationManager notificationManager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        notificationManager = context.getSystemService(NotificationManager.class);
    }

    @Test
    public void clearsPlayServicesDelegate() {
        // given
        notificationManager.setNotificationDelegate(PLAY_SERVICES_PACKAGE);

        // when
        PushNotifications.clearPlayServicesDelegate(context);

        // then
        assertNull(notificationManager.getNotificationDelegate());
    }

    @Test
    public void keepsOtherDelegate() {
        // given
        notificationManager.setNotificationDelegate(OTHER_DELEGATE_PACKAGE);

        // when
        PushNotifications.clearPlayServicesDelegate(context);

        // then
        assertEquals(OTHER_DELEGATE_PACKAGE, notificationManager.getNotificationDelegate());
    }

}
