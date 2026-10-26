/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * patches/src/main/kotlin/app/ftl/patches/FireFox/Constants.kt
 */
package app.morphe.patches.firefox.misc.ui


internal const val PATCH_SETTINGS = "Lapp/hxreborn/extension/firefox/PatchSettings;"
internal const val OLD_MENU = "Lapp/hxreborn/extension/firefox/OldMenu;"
internal const val MENU_COMPOSE = "Lapp/hxreborn/extension/firefox/MenuCompose;"
internal const val MENU_LAMBDA = "Lapp/hxreborn/extension/firefox/MenuLambda;"

internal const val MENU_PACKAGE = "Lorg/mozilla/fenix/components/menu/"
internal const val COMPOSE_PACKAGE = "Lorg/mozilla/fenix/components/menu/compose/"

internal const val MAIN_MENU_KT = "${COMPOSE_PACKAGE}MainMenuKt;"
internal const val MENU_ITEM_KT = "${COMPOSE_PACKAGE}MenuItemKt;"
internal const val MENU_GROUP_KT = "${COMPOSE_PACKAGE}MenuGroupKt;"
internal const val MENU_NAVIGATION_KT = "${COMPOSE_PACKAGE}MenuNavigationKt;"
internal const val MENU_SCAFFOLD_KT = "${COMPOSE_PACKAGE}MenuScaffoldKt;"
internal const val EXTENSIONS_MENU_ITEM_KT = "${COMPOSE_PACKAGE}ExtensionsMenuItemKt;"
internal const val IP_PROTECTION_KT = "${COMPOSE_PACKAGE}IPProtectionMenuItemKt;"
internal const val ACCOUNT_KT = "${COMPOSE_PACKAGE}header/MozillaAccountMenuItemKt;"
internal const val BOTTOM_SHEET_HANDLE_KT = "Lmozilla/components/compose/base/BottomSheetHandleKt;"

internal const val COLOR_SCHEME = "Landroidx/compose/material3/ColorScheme;"

internal const val OBJECT = "Ljava/lang/Object;"
internal const val FUNCTION0 = "Lkotlin/jvm/functions/Function0;"
internal const val FUNCTION1 = "Lkotlin/jvm/functions/Function1;"
internal const val FUNCTION2 = "Lkotlin/jvm/functions/Function2;"
internal const val CONTINUATION = "Lkotlin/coroutines/Continuation;"
internal const val COMPOSER = "Landroidx/compose/runtime/Composer;"
internal const val SCROLLABLE_STATE = "Landroidx/compose/foundation/gestures/ScrollableState;"
internal const val TAB_TOOLBAR = "Lapp/hxreborn/extension/firefox/TabToolbar;"
internal const val TAB_LAYOUT_KT = "Lorg/mozilla/fenix/tabstray/ui/tabpage/TabLayoutKt;"
internal const val SCROLL_HELPER_LAMBDA = "Lorg/mozilla/fenix/tabstray/ui/tabpage/TabLayoutKt\$TabLayoutScrollHelper"
internal const val TOOLBAR_KT = "Lorg/mozilla/fenix/tabstray/ui/fab/TabManagerFloatingToolbarKt;"
internal const val TABS_TRAY_STATE = "Lorg/mozilla/fenix/tabstray/redux/state/TabsTrayState;"
