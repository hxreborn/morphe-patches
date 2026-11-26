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
package app.hxreborn.extension.magicposer;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class ProtectionCrashFilterTest {

    private static final String START_METHOD_NAME = "start";

    public static final class FinishedThreadHolder {

        public void start() {
            Thread thread = new Thread(() -> { });
            thread.start();
            thread.start();
        }

    }

    public static final class FailingHolder {

        public void start() {
            throw new IllegalArgumentException("unrelated failure");
        }

    }

    public static final class WorkingHolder {

        int starts;

        public void start() {
            this.starts++;
        }

    }

    private static Method startOf(Class<?> holder) throws NoSuchMethodException {
        return holder.getMethod(START_METHOD_NAME);
    }

    @Test
    public void ignoresRestartOfFinishedThread() throws NoSuchMethodException {
        ProtectionCrashFilter.startProtectionThread(startOf(FinishedThreadHolder.class), new FinishedThreadHolder(),
                null);
    }

    @Test
    public void forwardsUnrelatedRuntimeFailure() throws NoSuchMethodException {
        Assert.assertThrows(IllegalArgumentException.class, () -> ProtectionCrashFilter
            .startProtectionThread(startOf(FailingHolder.class), new FailingHolder(), null));
    }

    @Test
    public void runsStartMethodOnce() throws NoSuchMethodException {
        WorkingHolder holder = new WorkingHolder();

        ProtectionCrashFilter.startProtectionThread(startOf(WorkingHolder.class), holder, null);

        Assert.assertEquals(1, holder.starts);
    }

}
