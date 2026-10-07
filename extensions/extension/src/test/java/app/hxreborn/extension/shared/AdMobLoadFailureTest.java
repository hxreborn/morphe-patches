/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import java.util.List;

import android.os.Looper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
public final class AdMobLoadFailureTest {

    private static final String TAG = "AdMobLoadFailure";

    @Test
    public void schedulesNothingForANullCallback() {
        // when
        AdMobLoadFailure.report(null);

        // then
        assertTrue(shadowOf(Looper.getMainLooper()).isIdle());
        assertTrue(ShadowLog.getLogsForTag(TAG).isEmpty());
    }

    @Test
    public void deliversOnTheMainLooperRatherThanInline() {
        AdMobLoadFailure.report(new Object());
        assertFalse(shadowOf(Looper.getMainLooper()).isIdle());
        assertTrue(ShadowLog.getLogsForTag(TAG).isEmpty());

        shadowOf(Looper.getMainLooper()).idle();

        assertTrue(shadowOf(Looper.getMainLooper()).isIdle());
        assertEquals(1, ShadowLog.getLogsForTag(TAG).size());
    }

    @Test
    public void containsAFailureToLoadTheAdsTypes() {
        // given
        AdMobLoadFailure.report(new Object());

        // when
        shadowOf(Looper.getMainLooper()).idle();

        // then
        final List<ShadowLog.LogItem> logs = ShadowLog.getLogsForTag(TAG);
        assertEquals("Could not deliver the load failure", logs.get(0).msg);
        assertTrue(String.valueOf(logs.get(0).throwable), logs.get(0).throwable instanceof ClassNotFoundException);
        assertTrue(logs.get(0).throwable.getMessage(), logs.get(0).throwable.getMessage().endsWith("LoadAdError"));
    }

}
