/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.time.Instant;

import app.morphe.extension.shared.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public final class LateFlagTest {

    private static final Instant START = Instant.parse("2026-10-23T12:00:00Z");

    private static final Instant END = START.plusSeconds(120);

    @Before
    public void enableSetting() {
        Utils.setContext(RuntimeEnvironment.getApplication());
        PatchSettings.REMOVE_LATE_FLAG.save(true);
    }

    @After
    public void restoreDefaults() {
        LateFlag.onMoment(null);
        PatchSettings.REMOVE_LATE_FLAG.save(false);
    }

    @Test
    public void movesALateCaptureIntoTheMomentWindow() {
        // given
        LateFlag.onMoment(new Moment(new Timestamp(START), new Timestamp(END)));

        // when
        final Timestamp sent = (Timestamp) LateFlag.takenAt(new Timestamp(END.plusSeconds(3600)));

        // then
        assertFalse(sent.value.isBefore(START));
        assertFalse(sent.value.isAfter(END));
    }

    @Test
    public void keepsTheCaptureTimeAfterAMissingMoment() {
        // given
        LateFlag.onMoment(new Moment(new Timestamp(START), new Timestamp(END)));
        LateFlag.onMoment(null);
        final Timestamp late = new Timestamp(END.plusSeconds(86400));

        // when
        final Object sent = LateFlag.takenAt(late);

        // then
        assertSame(late, sent);
    }

    @Test
    public void keepsTheCaptureTimeAfterAnUnreadableMoment() {
        // given
        LateFlag.onMoment(new Moment(new Timestamp(START), new Timestamp(END)));
        LateFlag.onMoment(new Object());
        final Timestamp late = new Timestamp(END.plusSeconds(86400));

        // when
        final Object sent = LateFlag.takenAt(late);

        // then
        assertSame(late, sent);
    }

    @Test
    public void keepsAnOnTimeCapture() {
        // given
        LateFlag.onMoment(new Moment(new Timestamp(START), new Timestamp(END)));
        final Timestamp onTime = new Timestamp(START.plusSeconds(30));

        // when
        final Object sent = LateFlag.takenAt(onTime);

        // then
        assertSame(onTime, sent);
    }

    @Test
    public void keepsTheCaptureTimeWhenTheSettingIsOff() {
        // given
        LateFlag.onMoment(new Moment(new Timestamp(START), new Timestamp(END)));
        PatchSettings.REMOVE_LATE_FLAG.save(false);
        final Timestamp late = new Timestamp(END.plusSeconds(3600));

        // when
        final Object sent = LateFlag.takenAt(late);

        // then
        assertSame(late, sent);
    }

    @Test
    @Config(sdk = 25)
    public void keepsTheCaptureTimeBelowAndroid8() {
        // given
        LateFlag.onMoment(new Moment(new Timestamp(START), new Timestamp(END)));
        final Timestamp late = new Timestamp(END.plusSeconds(3600));

        // when
        final Object sent = LateFlag.takenAt(late);

        // then
        assertSame(late, sent);
    }

    private static final class Moment {

        private final Timestamp creationDate;

        private final Timestamp expirationDate;

        Moment(Timestamp creationDate, Timestamp expirationDate) {
            this.creationDate = creationDate;
            this.expirationDate = expirationDate;
        }

    }

    private static final class Timestamp {

        private final Instant value;

        Timestamp(Instant value) {
            this.value = value;
        }

    }

}
