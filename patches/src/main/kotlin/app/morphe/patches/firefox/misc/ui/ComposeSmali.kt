/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * patches/src/main/kotlin/app/ftl/patches/FireFox/ComposeSmali.kt
 */
package app.morphe.patches.firefox.misc.ui

private const val MENU_ITEM = "$MENU_ITEM_KT->MenuItem(" +
    "Ljava/lang/String;Landroidx/compose/ui/graphics/painter/Painter;Landroidx/compose/ui/Modifier;" +
    "Landroidx/compose/ui/Modifier;ZLjava/lang/String;ILjava/lang/String;" +
    "${MENU_PACKAGE}compose/MenuItemState;${MENU_PACKAGE}compose/MenuItemState;" +
    "Lkotlin/jvm/functions/Function0;ZLandroidx/compose/ui/graphics/painter/Painter;Ljava/lang/String;" +
    "Landroidx/compose/ui/semantics/CollectionItemInfo;Lkotlin/jvm/functions/Function0;" +
    "Lkotlin/jvm/functions/Function2;Landroidx/compose/runtime/Composer;III)V"

internal class ComposeMethod(
    val name: String,
    val registers: Int,
    val smali: String,
)

internal val COMPOSE_METHODS = listOf(
    ComposeMethod(
        name = "divider",
        registers = 5,
        smali = $$$"""
            check-cast p0, Landroidx/compose/runtime/Composer;
            sget-object v0, Landroidx/compose/ui/Modifier$Companion;->$$INSTANCE:Landroidx/compose/ui/Modifier$Companion;
            const/high16 v1, 0x3f800000
            invoke-static {v0, v1}, Landroidx/compose/foundation/layout/SizeKt;->fillMaxWidth(Landroidx/compose/ui/Modifier;F)Landroidx/compose/ui/Modifier;
            move-result-object v0
            invoke-static {v0, v1}, Landroidx/compose/foundation/layout/SizeKt;->height-3ABfNKs(Landroidx/compose/ui/Modifier;F)Landroidx/compose/ui/Modifier;
            move-result-object v0
            sget-object v1, Landroidx/compose/material3/ColorSchemeKt;->LocalColorScheme:Landroidx/compose/runtime/StaticProvidableCompositionLocal;
            invoke-interface {p0, v1}, Landroidx/compose/runtime/Composer;->consume(Landroidx/compose/runtime/ProvidableCompositionLocal;)Ljava/lang/Object;
            move-result-object v1
            check-cast v1, Landroidx/compose/material3/ColorScheme;
            iget-wide v1, v1, Landroidx/compose/material3/ColorScheme;->outlineVariant:J
            sget-object v3, Landroidx/compose/ui/graphics/RectangleShapeKt;->RectangleShape:Landroidx/compose/ui/graphics/RectangleShapeKt$RectangleShape$1;
            invoke-static {v0, v1, v2, v3}, Landroidx/compose/foundation/BackgroundKt;->background-bw27NRU(Landroidx/compose/ui/Modifier;JLandroidx/compose/ui/graphics/Shape;)Landroidx/compose/ui/Modifier;
            move-result-object v0
            invoke-static {p0, v0}, Landroidx/compose/foundation/layout/SpacerKt;->Spacer(Landroidx/compose/runtime/Composer;Landroidx/compose/ui/Modifier;)V
            return-void
""",
    ),
    ComposeMethod(
        name = "managerRow",
        registers = 27,
        smali = $$$"""
            move-object/from16 v17, p0
            check-cast v17, Landroidx/compose/runtime/Composer;
            move-object/from16 v10, p1
            check-cast v10, Lkotlin/jvm/functions/Function0;
            sget v1, Lmozilla/components/ui/icons/R$drawable;->mozac_ic_extension_24:I
            const/4 v5, 0x0
            move-object/from16 v4, v17
            invoke-static {v1, v4, v5}, Landroidx/compose/ui/res/PainterResources_androidKt;->painterResource(ILandroidx/compose/runtime/Composer;I)Landroidx/compose/ui/graphics/painter/Painter;
            move-result-object v22
            sget v0, Lorg/mozilla/fenix/R$string;->browser_menu_manage_extensions:I
            invoke-static {v0, v4}, Landroidx/compose/ui/res/StringResources_androidKt;->stringResource(ILandroidx/compose/runtime/Composer;)Ljava/lang/String;
            move-result-object v21
            move-object/from16 v0, v21
            move-object/from16 v1, v22
            const/4 v2, 0x0
            const/4 v3, 0x0
            const/4 v4, 0x0
            const/4 v5, 0x0
            const/4 v6, 0x0
            const/4 v7, 0x0
            const/4 v8, 0x0
            const/4 v9, 0x0
            const/4 v11, 0x0
            const/4 v12, 0x0
            const/4 v13, 0x0
            const/4 v14, 0x0
            const/4 v15, 0x0
            const/16 v16, 0x0
            const/16 v18, 0x40
            const/16 v19, 0x0
            const v20, 0x3f7fc
            invoke-static/range {v0 .. v20}, $$$MENU_ITEM
            move-object/from16 v0, v17
            invoke-static {v0}, Lapp/hxreborn/extension/firefox/MenuCompose;->divider(Ljava/lang/Object;)V
            return-void
""",
    ),
    ComposeMethod(
        name = "extensionsPage",
        registers = 27,
        smali = $$$"""
            move-object/from16 v17, p2
            check-cast v17, Landroidx/compose/runtime/Composer;
            move-object/from16 v4, v17
            move-object/from16 v1, p0
            check-cast v1, Landroidx/compose/runtime/internal/ComposableLambdaImpl;
            const/4 v2, 0x1
            const/4 v3, 0x6
            invoke-static {v2, v1, v4, v3}, Lorg/mozilla/fenix/components/menu/compose/MenuItemKt;->ExpandableMenuItemAnimation(ZLandroidx/compose/runtime/internal/ComposableLambdaImpl;Landroidx/compose/runtime/Composer;I)V
            invoke-static {v4}, Lapp/hxreborn/extension/firefox/MenuCompose;->divider(Ljava/lang/Object;)V
            sget v1, Lorg/mozilla/fenix/R$string;->browser_menu_extensions:I
            invoke-static {v1, v4}, Landroidx/compose/ui/res/StringResources_androidKt;->stringResource(ILandroidx/compose/runtime/Composer;)Ljava/lang/String;
            move-result-object v21
            sget v1, Lmozilla/components/ui/icons/R$drawable;->mozac_ic_back_24:I
            const/4 v5, 0x0
            invoke-static {v1, v4, v5}, Landroidx/compose/ui/res/PainterResources_androidKt;->painterResource(ILandroidx/compose/runtime/Composer;I)Landroidx/compose/ui/graphics/painter/Painter;
            move-result-object v22
            move-object/from16 v10, p1
            check-cast v10, Lkotlin/jvm/functions/Function0;
            move-object/from16 v0, v21
            move-object/from16 v1, v22
            const/4 v2, 0x0
            const/4 v3, 0x0
            const/4 v4, 0x0
            const/4 v5, 0x0
            const/4 v6, 0x0
            const/4 v7, 0x0
            const/4 v8, 0x0
            const/4 v9, 0x0
            const/4 v11, 0x0
            const/4 v12, 0x0
            const/4 v13, 0x0
            const/4 v14, 0x0
            const/4 v15, 0x0
            const/16 v16, 0x0
            const/16 v18, 0x40
            const/16 v19, 0x0
            const v20, 0x3f7fc
            invoke-static/range {v0 .. v20}, $$$MENU_ITEM
            return-void
""",
    ),
    ComposeMethod(
        name = "libraryItem",
        registers = 25,
        smali = $$$"""
            move-object/from16 v17, p0
            check-cast v17, Landroidx/compose/runtime/Composer;
            move/from16 v1, p1
            move/from16 v2, p2
            move-object/from16 v10, p3
            check-cast v10, Lkotlin/jvm/functions/Function0;
            move-object/from16 v0, v17
            invoke-static {v1, v0}, Landroidx/compose/ui/res/StringResources_androidKt;->stringResource(ILandroidx/compose/runtime/Composer;)Ljava/lang/String;
            move-result-object v1
            const/4 v3, 0x0
            invoke-static {v2, v0, v3}, Landroidx/compose/ui/res/PainterResources_androidKt;->painterResource(ILandroidx/compose/runtime/Composer;I)Landroidx/compose/ui/graphics/painter/Painter;
            move-result-object v2
            move-object v0, v1
            move-object v1, v2
            const/4 v2, 0x0
            const/4 v3, 0x0
            const/4 v4, 0x0
            const/4 v5, 0x0
            const/4 v6, 0x0
            const/4 v7, 0x0
            sget-object v8, Lorg/mozilla/fenix/components/menu/compose/MenuItemState;->ENABLED:Lorg/mozilla/fenix/components/menu/compose/MenuItemState;
            const/4 v9, 0x0
            const/4 v11, 0x0
            const/4 v12, 0x0
            const/4 v13, 0x0
            const/4 v14, 0x0
            const/4 v15, 0x0
            const/16 v16, 0x0
            const v18, 0x30000040
            const/16 v19, 0x0
            const v20, 0x3f5fc
            invoke-static/range {v0 .. v20}, $$$MENU_ITEM
            return-void
""",
    ),
    ComposeMethod(
        name = "libraryList",
        registers = 12,
        smali = $$$"""
            check-cast p4, Landroidx/compose/runtime/Composer;
            move-object v0, p4
            move-object v1, p0
            move-object v2, p1
            move-object v3, p2
            move-object v4, p3
            sget v5, Lorg/mozilla/fenix/R$string;->library_history:I
            sget v6, Lmozilla/components/ui/icons/R$drawable;->mozac_ic_history_24:I
            invoke-static {v0, v5, v6, v1}, Lapp/hxreborn/extension/firefox/MenuCompose;->libraryItem(Ljava/lang/Object;IILjava/lang/Object;)V
            sget v5, Lorg/mozilla/fenix/R$string;->library_bookmarks:I
            sget v6, Lmozilla/components/ui/icons/R$drawable;->mozac_ic_bookmark_tray_fill_24:I
            invoke-static {v0, v5, v6, v2}, Lapp/hxreborn/extension/firefox/MenuCompose;->libraryItem(Ljava/lang/Object;IILjava/lang/Object;)V
            sget v5, Lorg/mozilla/fenix/R$string;->library_downloads:I
            sget v6, Lmozilla/components/ui/icons/R$drawable;->mozac_ic_download_24:I
            invoke-static {v0, v5, v6, v3}, Lapp/hxreborn/extension/firefox/MenuCompose;->libraryItem(Ljava/lang/Object;IILjava/lang/Object;)V
            if-eqz v4, :hx_nopw
            sget v5, Lorg/mozilla/fenix/R$string;->browser_menu_passwords:I
            sget v6, Lmozilla/components/ui/icons/R$drawable;->mozac_ic_login_24:I
            invoke-static {v0, v5, v6, v4}, Lapp/hxreborn/extension/firefox/MenuCompose;->libraryItem(Ljava/lang/Object;IILjava/lang/Object;)V
            :hx_nopw
            return-void
""",
    ),
    ComposeMethod(
        name = "libraryGroup",
        registers = 9,
        smali = $$$"""
            check-cast p4, Landroidx/compose/runtime/Composer;
            invoke-static {p1, p0, p2, p3}, Lapp/hxreborn/extension/firefox/MenuLambda;->libraryList(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Lapp/hxreborn/extension/firefox/MenuLambda;
            move-result-object v0
            const v1, 0x41d486a7
            invoke-static {v1, v0, p4}, Landroidx/compose/runtime/internal/ComposableLambdaKt;->rememberComposableLambda(ILkotlin/Function;Landroidx/compose/runtime/Composer;)Landroidx/compose/runtime/internal/ComposableLambdaImpl;
            move-result-object v0
            const/4 v1, 0x6
            invoke-static {v0, p4, v1}, Lorg/mozilla/fenix/components/menu/compose/MenuGroupKt;->MenuGroup(Landroidx/compose/runtime/internal/ComposableLambdaImpl;Landroidx/compose/runtime/Composer;I)V
            return-void
""",
    ),
    ComposeMethod(
        name = "patchesRow",
        registers = 6,
        smali = $$$"""
            check-cast p0, Landroidx/compose/runtime/Composer;
            invoke-static {}, Lapp/hxreborn/extension/firefox/PatchSettings;->oldMenu()Z
            move-result v0
            if-eqz v0, :hx_plain
            invoke-static {p0}, Lapp/hxreborn/extension/firefox/MenuCompose;->divider(Ljava/lang/Object;)V
            :hx_plain
            invoke-static {}, Lapp/hxreborn/extension/firefox/MenuLambda;->patchesRow()Lapp/hxreborn/extension/firefox/MenuLambda;
            move-result-object v0
            const v1, 0x7a11b0a1
            invoke-static {v1, v0, p0}, Landroidx/compose/runtime/internal/ComposableLambdaKt;->rememberComposableLambda(ILkotlin/Function;Landroidx/compose/runtime/Composer;)Landroidx/compose/runtime/internal/ComposableLambdaImpl;
            move-result-object v0
            const/4 v1, 0x6
            invoke-static {v0, p0, v1}, Lorg/mozilla/fenix/components/menu/compose/MenuGroupKt;->MenuGroup(Landroidx/compose/runtime/internal/ComposableLambdaImpl;Landroidx/compose/runtime/Composer;I)V
            invoke-static {}, Lapp/hxreborn/extension/firefox/OldMenu;->showBottomDivider()Z
            move-result v0
            if-eqz v0, :hx_end
            invoke-static {p0}, Lapp/hxreborn/extension/firefox/MenuCompose;->divider(Ljava/lang/Object;)V
            :hx_end
            return-void
""",
    ),
    ComposeMethod(
        name = "patchesRowContent",
        registers = 27,
        smali = $$$"""
            move-object/from16 v17, p0
            check-cast v17, Landroidx/compose/runtime/Composer;
            sget-object v0, Landroidx/compose/ui/platform/AndroidCompositionLocals_androidKt;->LocalContext:Landroidx/compose/runtime/StaticProvidableCompositionLocal;
            move-object/from16 v1, v17
            invoke-interface {v1, v0}, Landroidx/compose/runtime/Composer;->consume(Landroidx/compose/runtime/ProvidableCompositionLocal;)Ljava/lang/Object;
            move-result-object v0
            check-cast v0, Landroid/content/Context;
            invoke-static {v0}, Lapp/hxreborn/extension/firefox/PatchesMenuClick;->open(Landroid/content/Context;)Lapp/hxreborn/extension/firefox/PatchesMenuClick;
            move-result-object v10
            invoke-static {v0}, Lapp/hxreborn/extension/firefox/OldMenu;->patchesIcon(Landroid/content/Context;)I
            move-result v1
            const/4 v5, 0x0
            move-object/from16 v4, v17
            invoke-static {v1, v4, v5}, Landroidx/compose/ui/res/PainterResources_androidKt;->painterResource(ILandroidx/compose/runtime/Composer;I)Landroidx/compose/ui/graphics/painter/Painter;
            move-result-object v22
            const-string v21, "hxreborn patches"
            move-object/from16 v0, v21
            move-object/from16 v1, v22
            const/4 v2, 0x0
            const/4 v3, 0x0
            const/4 v4, 0x0
            const/4 v5, 0x0
            const/4 v6, 0x0
            const/4 v7, 0x0
            const/4 v8, 0x0
            const/4 v9, 0x0
            const/4 v11, 0x0
            const/4 v12, 0x0
            const/4 v13, 0x0
            const/4 v14, 0x0
            const/4 v15, 0x0
            const/16 v16, 0x0
            const/16 v18, 0x40
            const/16 v19, 0x0
            const v20, 0x3f7fc
            invoke-static/range {v0 .. v20}, $$$MENU_ITEM
            return-void
""",
    ),
    ComposeMethod(
        name = "libraryGroupFlagged",
        registers = 7,
        smali = $$$"""
            if-nez p0, :hx_keep
            const/4 p5, 0x0
            :hx_keep
            invoke-static {p2, p3, p4, p5, p6}, Lapp/hxreborn/extension/firefox/MenuCompose;->libraryGroup(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
            return-void
        """,
    ),
)
