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
package hx;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public final class RetryScheduleTest {

    private final RetrySchedule schedule = new RetrySchedule(3, 50L, 2, 1000L);

    @Test
    public void retriesQuicklyDuringTheFastAttempts() {
        // when
        long delay = this.schedule.delayAfter(2);

        // then
        Assertions.assertThat(delay).isEqualTo(50L);
    }

    @Test
    public void keepsRetryingSlowlyAfterTheFastAttempts() {
        // when
        long delay = this.schedule.delayAfter(3);

        // then
        Assertions.assertThat(delay).isEqualTo(1000L);
    }

    @Test
    public void stopsAfterTheSlowAttempts() {
        // when
        long delay = this.schedule.delayAfter(5);

        // then
        Assertions.assertThat(delay).isEqualTo(RetrySchedule.STOP);
    }

    @Test
    public void reportsOnceWhenTheFastAttemptsRunOut() {
        // when
        boolean[] reports = { this.schedule.reportsAfter(2), this.schedule.reportsAfter(3),
                this.schedule.reportsAfter(4) };

        // then
        Assertions.assertThat(reports).containsExactly(false, true, false);
    }

}
