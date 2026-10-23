/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal.latelabel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import app.hxreborn.extension.bereal.PatchSettings;
import app.morphe.extension.shared.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class LateLabelTest {

    @Before
    public void attachContext() {
        Utils.setContext(RuntimeEnvironment.getApplication());
    }

    @After
    public void restoreDefaults() {
        PatchSettings.REMOVE_LATE_LABEL.save(true);
    }

    @Test
    public void clearsTheLateFlagByDefault() {
        assertFalse(LateLabel.isLate(true));
    }

    @Test
    public void zeroesTheLatenessByDefault() {
        assertEquals(0L, LateLabel.lateBy(8228L));
    }

    @Test
    public void dropsTheLateSecondsByDefault() {
        assertNull(LateLabel.lateSeconds(8228));
    }

    @Test
    public void keepsTheLateValuesWhenOff() {
        PatchSettings.REMOVE_LATE_LABEL.save(false);

        assertTrue(LateLabel.isLate(true));
        assertEquals(8228L, LateLabel.lateBy(8228L));
        assertEquals(Integer.valueOf(8228), LateLabel.lateSeconds(8228));
    }

    @Test
    public void keepsAnOnTimeFlagWhenOff() {
        PatchSettings.REMOVE_LATE_LABEL.save(false);

        assertFalse(LateLabel.isLate(false));
    }

}
