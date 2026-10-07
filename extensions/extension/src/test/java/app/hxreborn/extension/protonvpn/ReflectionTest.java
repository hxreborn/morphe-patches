/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonvpn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

public final class ReflectionTest {

    static final class Target {

        private final Object value = new Object();

        public Object getValue() {
            return this.value;
        }

        public int getCount() {
            return 7;
        }

        public boolean isReady() {
            return true;
        }

        public void nothing() {
        }

        public void explode() {
            throw new UnsupportedOperationException("exploded");
        }

        public String echo(String text) {
            return text;
        }

        @SuppressWarnings("unused")
        private String hidden() {
            return "hidden";
        }

    }

    @Test
    public void returnsTheMethodResult() {
        // given
        final Target target = new Target();

        // when
        final Object result = Reflection.call(target, "getValue");

        // then
        assertSame(target.getValue(), result);
    }

    @Test
    public void boxesPrimitiveResults() {
        final Target target = new Target();
        assertEquals(7, Reflection.call(target, "getCount"));
        assertEquals(Boolean.TRUE, Reflection.call(target, "isReady"));
    }

    @Test
    public void voidMethodsReturnNull() {
        assertNull(Reflection.call(new Target(), "nothing"));
    }

    @Test
    public void findsPublicMethodsInheritedFromObject() {
        final Target target = new Target();
        assertEquals(target.hashCode(), Reflection.call(target, "hashCode"));
        assertEquals(target.toString(), Reflection.call(target, "toString"));
    }

    @Test
    public void missingMethodIsReportedAsIllegalState() {
        try {
            Reflection.call(new Target(), "getMissing");
            fail("expected an IllegalStateException");
        } catch (IllegalStateException thrown) {
            assertTrue(thrown.getCause() instanceof NoSuchMethodException);
        }
    }

    @Test
    public void nonPublicOrParameterizedMethodsAreNotFound() {
        for (String name : new String[] { "hidden", "echo" }) {
            try {
                Reflection.call(new Target(), name);
                fail("expected an IllegalStateException for " + name);
            } catch (IllegalStateException thrown) {
                assertTrue(name, thrown.getCause() instanceof NoSuchMethodException);
            }
        }
    }

    @Test
    public void failureInsideTheMethodIsWrappedWithItsOriginalCause() {
        try {
            Reflection.call(new Target(), "explode");
            fail("expected an IllegalStateException");
        } catch (IllegalStateException thrown) {
            assertTrue(thrown.getCause() instanceof InvocationTargetException);
            assertTrue(thrown.getCause().getCause() instanceof UnsupportedOperationException);
            assertEquals("exploded", thrown.getCause().getCause().getMessage());
        }
    }

}
