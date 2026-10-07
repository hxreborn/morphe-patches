/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public final class AppliedPatchesTest {

    private static int assertEveryAccessor(boolean expected) throws ReflectiveOperationException {
        int accessors = 0;
        for (Method method : AppliedPatches.class.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers())
                    && method.getParameterCount() == 0 && method.getReturnType() == boolean.class) {
                assertEquals(method.getName(), expected, method.invoke(null));
                accessors++;
            }
        }
        return accessors;
    }

    @Test
    public void unpatchedBuildListsNoAppliedPatch() {
        assertTrue(AppliedPatches.names().isEmpty());
    }

    @Test
    public void everyAccessorReportsUnpatched() throws ReflectiveOperationException {
        assertTrue(assertEveryAccessor(false) > 0);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void patchedBuildListsEveryPatchInDisplayOrder() {
        // when
        List<String> names = AppliedPatches.names();

        // then
        assertEquals(Arrays.asList("Custom accent color", "AMOLED dark theme", "Hide upgrade promotions",
                "Hide promotional messages", "Remove 'Sent from' signature", "Remove free accounts limit",
                "Scheduled Trash and Spam deletion", "Unlock custom time picker", "Remove server change delay",
                "Unlock split tunneling", "Unlock LAN connections", "Unlock custom DNS", "Unlock NetShield",
                "Unlock connection preferences", "Unlock profiles", "Show free server locations", "Disable telemetry"),
                names);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void everyAccessorReportsPatched() throws ReflectiveOperationException {
        assertTrue(assertEveryAccessor(true) > 0);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void themeAccessorsFollowTheirOwnPatchFlag() {
        assertTrue(AppliedPatches.accentColor());
        assertTrue(AppliedPatches.amoledDarkTheme());
        assertTrue(AppliedPatches.hideUpgradePromotions());
        assertFalse(AppliedPatches.names().isEmpty());
    }

}
