/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.shared;

import android.view.Window;
import android.view.WindowManager;

@SuppressWarnings("unused")
public final class WindowFlags {

    private WindowFlags() {

    }

    public static void addFlags(Window window, int flags) {
        window.addFlags(flags & ~WindowManager.LayoutParams.FLAG_SECURE);
    }

    public static void setFlags(Window window, int flags, int mask) {
        window.setFlags(flags & ~WindowManager.LayoutParams.FLAG_SECURE, mask);
    }

}
