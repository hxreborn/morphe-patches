/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
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
        final Click callback = (Click) HostCallbacks.create(Click.class, calls::incrementAndGet);

        callback.invoke();
        callback.invoke();

        Assert.assertEquals(2, calls.get());
    }

    @Test
    public void returnsNullFromTheClick() {
        final Click callback = (Click) HostCallbacks.create(Click.class, () -> {
        });

        final Object result = callback.invoke();

        Assert.assertNull(result);
    }

    @Test
    public void keepsObjectMethodsFromInvokingTheAction() {
        final AtomicInteger calls = new AtomicInteger();
        final Object callback = HostCallbacks.create(Click.class, calls::incrementAndGet);

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
        final Click callback = () -> calls.incrementAndGet();

        HostCallbacks.invoke(callback);

        Assert.assertEquals(1, calls.get());
    }

    @Test
    public void implementsEveryCallbackInterface() {
        final Object callback = HostCallbacks.create(new Class<?>[] { Click.class, Runnable.class }, () -> {
        });

        Assert.assertTrue(callback instanceof Click);
        Assert.assertTrue(callback instanceof Runnable);
    }

    public interface Click {

        Object invoke();

    }

}
