/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/ModLambda.java
 */
package app.hxreborn.extension.firefox;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

@SuppressWarnings("unused")
public final class MenuLambda implements Function2<Object, Object, Object> {

    private static final int EXTENSIONS_PAGE = 0;

    private static final int LIBRARY_LIST = 1;

    private static final int PATCHES_ROW = 2;

    private final int kind;

    private final Object a;

    private final Object b;

    private final Object c;

    private final Object d;

    private MenuLambda(int kind, Object a, Object b, Object c, Object d) {
        this.kind = kind;
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public static MenuLambda extensionsPage(Object content, Object onBack) {
        return new MenuLambda(EXTENSIONS_PAGE, content, onBack, null, null);
    }

    public static MenuLambda libraryList(Object history, Object bookmarks, Object downloads, Object passwords) {
        return new MenuLambda(LIBRARY_LIST, history, bookmarks, downloads, passwords);
    }

    public static MenuLambda patchesRow() {
        return new MenuLambda(PATCHES_ROW, null, null, null, null);
    }

    public Object invoke(Object composer, Object flags) {
        switch (this.kind) {
            case EXTENSIONS_PAGE:
                MenuCompose.extensionsPage(this.a, this.b, composer);
                break;
            case LIBRARY_LIST:
                MenuCompose.libraryList(this.a, this.b, this.c, this.d, composer);
                break;
            case PATCHES_ROW:
                MenuCompose.patchesRowContent(composer);
                break;
            default:
                break;
        }
        return Unit.INSTANCE;
    }

}
