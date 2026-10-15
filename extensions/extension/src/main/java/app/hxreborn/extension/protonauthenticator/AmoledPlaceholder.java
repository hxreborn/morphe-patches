/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonauthenticator;

@SuppressWarnings("unused")
public final class AmoledPlaceholder {

    private static final long PACKED_BLACK = 0xFF000000L << 32;

    private static final long PACKED_TINT = 0x33FFFFFFL << 32;

    private static final long NO_TINT = 0;

    private AmoledPlaceholder() {

    }

    public static long tint(long packedBackground) {
        return (packedBackground == PACKED_BLACK) ? PACKED_TINT : NO_TINT;
    }

}
