/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.firefox;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.res.Configuration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class AmoledThemeTest {

    private Context context;

    @Before
    public void useApplicationContext() {
        this.context = RuntimeEnvironment.getApplication();
    }

    @After
    public void restoreFlag() {
        PatchSettings.AMOLED.save(this.context, true);
    }

    @Test
    public void marksTheConfigurationForOledColorsWhenSwitchedOn() {
        // given
        PatchSettings.AMOLED.save(this.context, true);
        Configuration configuration = new Configuration();
        configuration.uiMode = Configuration.UI_MODE_NIGHT_YES;

        // when
        AmoledTheme.oledConfiguration(configuration);

        // then
        assertEquals(Configuration.UI_MODE_TYPE_VR_HEADSET, configuration.uiMode & Configuration.UI_MODE_TYPE_MASK);
        assertEquals(Configuration.UI_MODE_NIGHT_YES, configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK);
    }

    @Test
    public void leavesTheConfigurationAloneWhenSwitchedOff() {
        // given
        PatchSettings.AMOLED.save(this.context, false);
        Configuration configuration = new Configuration();
        configuration.uiMode = Configuration.UI_MODE_NIGHT_YES;

        // when
        AmoledTheme.oledConfiguration(configuration);

        // then
        assertEquals(Configuration.UI_MODE_NIGHT_YES, configuration.uiMode);
    }

}
