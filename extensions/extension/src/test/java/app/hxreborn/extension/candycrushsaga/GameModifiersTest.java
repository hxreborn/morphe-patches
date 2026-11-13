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
package app.hxreborn.extension.candycrushsaga;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
public class GameModifiersTest {

    private GameModifiers modifiers;

    @Before
    public void setUp() throws Exception {
        Constructor<GameModifiers> constructor = GameModifiers.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        this.modifiers = constructor.newInstance();
        ShadowLog.clear();
    }

    @Test
    public void retainsPendingRecoveryUntilNativeRollbackCompletes() throws Exception {
        record(-2);
        Assert.assertFalse(flag("finished"));
        Assert.assertTrue(flag("recovering"));
        Assert.assertEquals(1, ShadowLog.getLogsForTag(Boosters.LOG_TAG).size());
        Assert.assertTrue(ShadowLog.getLogsForTag(Boosters.LOG_TAG).get(0).msg.contains("recovery pending"));
        record(-2);
        Assert.assertFalse(flag("finished"));
        Assert.assertEquals(1, ShadowLog.getLogsForTag(Boosters.LOG_TAG).size());
        record(-3);
        Assert.assertTrue(flag("finished"));
        Assert.assertFalse(flag("recovering"));
        Assert.assertEquals(2, ShadowLog.getLogsForTag(Boosters.LOG_TAG).size());
        Assert.assertTrue(
                ShadowLog.getLogsForTag(Boosters.LOG_TAG).get(1).msg.contains("pointers and permissions restored"));
    }

    @Test
    public void distinguishesValidationRejectionFromCompletedActivation() throws Exception {
        record(0);
        Assert.assertFalse(flag("finished"));
        Assert.assertFalse(flag("recovering"));
        Assert.assertTrue(ShadowLog.getLogsForTag(Boosters.LOG_TAG).isEmpty());
        record(1);
        Assert.assertTrue(flag("finished"));
        Assert.assertTrue(ShadowLog.getLogsForTag(Boosters.LOG_TAG).isEmpty());
        record(-1);
        Assert.assertTrue(flag("finished"));
        Assert.assertTrue(ShadowLog.getLogsForTag(Boosters.LOG_TAG).get(0).msg.contains("validation failed"));
    }

    private void record(int result) throws Exception {
        Method method = GameModifiers.class.getDeclaredMethod("recordActivation", int.class);
        method.setAccessible(true);
        method.invoke(this.modifiers, result);
    }

    private boolean flag(String name) throws Exception {
        Field field = GameModifiers.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getBoolean(this.modifiers);
    }

}
