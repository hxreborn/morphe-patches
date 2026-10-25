/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.function.Function;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import app.hxreborn.extension.shared.LoadingIndicatorView;

@RunWith(RobolectricTestRunner.class)
public final class LoadingIndicatorTest {

    private Context context;

    @Before
    public void useApplicationContext() {
        this.context = PatchedBuild.useApplicationContext();
    }

    @Test
    public void staysOffInAnUnpatchedBuild() {
        assertFalse(LoadingIndicator.isEnabled());
    }

    @Test
    @Config(shadows = PatchedBuild.Indicator.class)
    public void turnsOnWhenPatched() {
        assertTrue(LoadingIndicator.isEnabled());
    }

    @Test
    @Config(shadows = PatchedBuild.Indicator.class)
    public void turnsOffFromSettings() {
        // given
        PatchedBuild.setLoadingIndicatorEnabled(false);

        // when
        boolean enabled = LoadingIndicator.isEnabled();

        // then
        assertFalse(enabled);
    }

    @Test
    @Config(shadows = PatchedBuild.Indicator.class)
    public void sizesTheContainerAt48DpWhenOn() {
        assertEquals(48f, LoadingIndicator.containerSize(40f), 0f);
    }

    @Test
    @Config(shadows = PatchedBuild.Indicator.class)
    public void keepsTheStockContainerSizeWhenTurnedOff() {
        // given
        PatchedBuild.setLoadingIndicatorEnabled(false);

        // when
        float size = LoadingIndicator.containerSize(40f);

        // then
        assertEquals(40f, size, 0f);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void createsALoadingIndicatorView() {
        // given
        Function<Object, Object> factory = (Function<Object, Object>) LoadingIndicator.factory(Function.class);

        // when
        Object view = factory.apply(this.context);

        // then
        assertTrue(view instanceof LoadingIndicatorView);
    }

    @Test
    @Config(shadows = PatchedBuild.Indicator.class)
    public void listsItselfAmongAppliedPatches() {
        assertTrue(AppliedPatches.names().contains("Material 3 loading indicator"));
    }

}
