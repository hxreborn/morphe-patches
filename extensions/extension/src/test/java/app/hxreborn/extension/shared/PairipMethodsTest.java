/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.shared;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Test;

public final class PairipMethodsTest {

    private static final String TARGET = Target.class.getName();

    private static Method find(String methodName, String parameterTypes) {
        return PairipMethods.find(TARGET, methodName, parameterTypes);
    }

    @Test
    public void findsMethodWithoutParameters() throws ReflectiveOperationException {
        // when
        final Method method = find("noArguments", "");

        // then
        assertEquals("noArguments", method.getName());
        assertEquals(0, method.getParameterTypes().length);
        assertEquals("none", method.invoke(new Target()));
    }

    @Test
    public void mapsPrimitiveNamesToPrimitiveTypes() {
        // when
        final Method method = find("primitives", "boolean,byte,char,double,float,int,long,short");

        // then
        assertArrayEquals(new Class<?>[] { boolean.class, byte.class, char.class, double.class, float.class, int.class,
                long.class, short.class }, method.getParameterTypes());
    }

    @Test
    public void resolvesNonPrimitiveTypesByBinaryName() throws ReflectiveOperationException {
        // when
        final Method method = find("mixed", "java.lang.String,int,[B");

        // then
        assertArrayEquals(new Class<?>[] { String.class, int.class, byte[].class }, method.getParameterTypes());
        assertEquals("a-7-2", method.invoke(new Target(), "a", 7, new byte[] { 1, 2 }));
    }

    @Test
    public void reachesPrivateMethods() throws ReflectiveOperationException {
        // when
        final Method method = find("hidden", "");

        // then
        assertTrue(method.isAccessible());
        assertEquals("private", method.invoke(new Target()));
    }

    @Test
    public void reachesStaticMethods() throws ReflectiveOperationException {
        assertEquals("static-5", find("shared", "int").invoke(null, 5));
    }

    @Test
    public void distinguishesOverloadsByParameterTypes() throws ReflectiveOperationException {
        assertEquals("int", find("overloaded", "int").invoke(new Target(), 1));
        assertEquals("long", find("overloaded", "long").invoke(new Target(), 1L));
        assertEquals("string", find("overloaded", "java.lang.String").invoke(new Target(), "x"));
    }

    @Test
    public void returnsTheDeclaredMethodOfTheNamedClass() {
        assertSame(Target.class, find("noArguments", "").getDeclaringClass());
    }

    @Test
    public void failsNamingTheMissingMethod() {
        try {
            find("absent", "");
            throw new AssertionError("expected a failure");
        } catch (IllegalStateException ex) {
            assertEquals(TARGET + ".absent is missing", ex.getMessage());
            assertTrue(ex.getCause() instanceof NoSuchMethodException);
        }
    }

    @Test
    public void failsWhenParametersDoNotMatchTheMethod() {
        try {
            find("primitives", "int");
            throw new AssertionError("expected a failure");
        } catch (IllegalStateException ex) {
            assertEquals(TARGET + ".primitives is missing", ex.getMessage());
        }
    }

    @Test
    public void failsNamingTheMissingClass() {
        try {
            PairipMethods.find("app.hxreborn.extension.shared.NoSuchClass", "run", "");
            throw new AssertionError("expected a failure");
        } catch (IllegalStateException ex) {
            assertEquals("app.hxreborn.extension.shared.NoSuchClass.run is missing", ex.getMessage());
            assertTrue(ex.getCause() instanceof ClassNotFoundException);
        }
    }

    @Test
    public void failsWhenAParameterTypeCannotBeLoaded() {
        try {
            find("mixed", "java.lang.String,no.such.Type,[B");
            throw new AssertionError("expected a failure");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getCause() instanceof ClassNotFoundException);
        }
    }

    @SuppressWarnings("unused")
    private static final class Target {

        String noArguments() {
            return "none";
        }

        String primitives(boolean flag, byte b, char c, double d, float f, int i, long l, short s) {
            return "primitives";
        }

        String mixed(String text, int number, byte[] bytes) {
            return text + "-" + number + "-" + bytes.length;
        }

        private String hidden() {
            return "private";
        }

        static String shared(int value) {
            return "static-" + value;
        }

        String overloaded(int value) {
            return "int";
        }

        String overloaded(long value) {
            return "long";
        }

        String overloaded(String value) {
            return "string";
        }

    }

}
