/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.function.Function;

import android.widget.Switch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class MaterialSwitchViewTest {

    private Switch control;

    @Before
    public void createSwitch() {
        this.control = new Switch(RuntimeEnvironment.getApplication());
    }

    @SuppressWarnings("unchecked")
    private void update(boolean checked, Object onCheckedChange, boolean enabled) {
        ((Function<Object, Object>) MaterialSwitchView.update(Function.class, checked, onCheckedChange, enabled))
            .apply(this.control);
    }

    @Test
    public void showsTheComposedState() {
        // when
        update(true, new Object(), false);

        // then
        assertTrue(this.control.isChecked());
        assertFalse(this.control.isEnabled());
    }

    @Test
    public void keepsTheComposedStateWhenTapped() {
        // given
        update(false, new Object(), true);

        // when
        this.control.performClick();

        // then
        assertFalse(this.control.isChecked());
    }

    @Test
    public void ignoresTapsWithoutACallback() {
        // when
        update(false, null, true);

        // then
        assertFalse(this.control.isClickable());
    }

}
