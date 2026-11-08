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
package app.hxreborn.extension.ultimea.equalizer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;

import app.hxreborn.extension.ultimea.devicesettings.DeviceSettingsShortcuts;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class FineEqualizerTest {

    @Test
    public void retainsHostSizeThroughBothWrappers() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            FrameLayout host = new FrameLayout(activity);
            activity.setContentView(host);
            DeviceSettingsShortcuts.attach(activity);
            FineEqualizer.attach(activity);
            ViewGroup content = activity.findViewById(android.R.id.content);
            content.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
            content.layout(0, 0, 1080, 1920);
            Assert.assertEquals(1080, host.getMeasuredWidth());
            Assert.assertEquals(1920, host.getMeasuredHeight());
            Assert.assertSame(content, host.getRootView().findViewById(android.R.id.content));
        }
    }

    @Test
    public void ignoresMalformedNavigation() {
        FineEqualizer.navigation("flutter/navigation", ByteBuffer.wrap("{".getBytes(StandardCharsets.UTF_8)));
        FineEqualizer.navigation("other", ByteBuffer.wrap(new byte[0]));
        FineEqualizer.navigation("flutter/navigation", null);
    }

}
