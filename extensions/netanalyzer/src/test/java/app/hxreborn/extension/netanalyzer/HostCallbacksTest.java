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
package app.hxreborn.extension.netanalyzer;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

public final class HostCallbacksTest {

    @Test
    public void invokesTheActionOnEveryClick() {
        final AtomicInteger calls = new AtomicInteger();
        final HostClick callback = (HostClick) HostCallbacks.create(HostClick.class, calls::incrementAndGet);

        callback.invoke();
        callback.invoke();

        Assert.assertEquals(2, calls.get());
    }

    @Test
    public void returnsNullFromTheClick() {
        final HostClick callback = (HostClick) HostCallbacks.create(HostClick.class, () -> {
        });

        final Object result = callback.invoke();

        Assert.assertNull(result);
    }

    @Test
    public void keepsObjectMethodsFromInvokingTheAction() {
        final AtomicInteger calls = new AtomicInteger();
        final Object callback = HostCallbacks.create(HostClick.class, calls::incrementAndGet);

        Assert.assertEquals("hxNetAnalyzerClick", callback.toString());
        Assert.assertEquals(System.identityHashCode(callback), callback.hashCode());
        Assert.assertTrue(callback.equals(callback));
        Assert.assertFalse(callback.equals(new Object()));
        Assert.assertFalse(callback.equals(null));
        Assert.assertEquals(0, calls.get());
    }

    @Test
    public void invokesTheOriginalCallbackThroughItsInterface() {
        final AtomicInteger calls = new AtomicInteger();
        final HostClick callback = () -> calls.incrementAndGet();

        HostCallbacks.invoke(callback);

        Assert.assertEquals(1, calls.get());
    }

    @Test
    public void implementsEveryCallbackInterface() {
        final Object callback = HostCallbacks.create(new Class<?>[] { HostClick.class, Runnable.class }, () -> {
        });

        Assert.assertTrue(callback instanceof HostClick);
        Assert.assertTrue(callback instanceof Runnable);
    }

}
