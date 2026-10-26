/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * patches/src/main/kotlin/app/ftl/patches/FireFox/Fingerprints.kt
 */
package app.morphe.patches.firefox.misc.ui

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.instanceOf
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

private const val ACCESS_POINT = "Lorg/mozilla/fenix/components/menu/MenuAccessPoint;"

private val SURFACE_BRIGHT = fieldAccess(
    definingClass = COLOR_SCHEME,
    name = "surfaceBright",
    opcode = Opcode.IGET_WIDE,
)

private fun homeAccessPoint() = fieldAccess(
    definingClass = ACCESS_POINT,
    name = "Home",
    opcode = Opcode.SGET_OBJECT,
)

internal object BottomSheetHandleFingerprint : Fingerprint(
    definingClass = BOTTOM_SHEET_HANDLE_KT,
    custom = { method, _ -> method.name.startsWith("BottomSheetHandle") },
)

internal object ExtensionsMenuItemFingerprint : Fingerprint(
    definingClass = EXTENSIONS_MENU_ITEM_KT,
    name = "ExtensionsMenuItem",
    filters = listOf(methodCall(definingClass = MENU_ITEM_KT, name = "MenuItem")),
)

internal object WebExtensionMenuItemsFingerprint : Fingerprint(
    definingClass = EXTENSIONS_MENU_ITEM_KT,
    name = "WebExtensionMenuItems",
    filters = listOf(constAny(0x40000000)),
)

internal object IPProtectionMenuItemFingerprint : Fingerprint(
    definingClass = IP_PROTECTION_KT,
    name = "IPProtectionMenuItem",
    filters = listOf(SURFACE_BRIGHT, constAny(0x42500000)),
)

internal object IPProtectionBadgeFingerprint : Fingerprint(
    name = "invoke",
    returnType = OBJECT,
    parameters = listOf(OBJECT, OBJECT),
    filters = listOf(
        methodCall(definingClass = MENU_ITEM_KT, name = "Badge"),
        constAny(0x41000000),
    ),
)

internal object MainMenuAddonsFingerprint : Fingerprint(
    definingClass = MAIN_MENU_KT,
    name = "Addons",
    filters = listOf(
        constAny(0x40000000),
        homeAccessPoint(),
        fieldAccess(
            definingClass = "Lorg/mozilla/fenix/R\$string;",
            name = "browser_menu_manage_extensions",
            opcode = Opcode.SGET,
        ),
        methodCall(definingClass = MAIN_MENU_KT, name = "MoreExtensionsMenuItem"),
    ),
)

internal object MainMenuLibraryGroupFingerprint : Fingerprint(
    definingClass = MAIN_MENU_KT,
    name = "LibraryMenuGroup",
)

internal object MainMenuFingerprint : Fingerprint(
    definingClass = MAIN_MENU_KT,
    name = "MainMenu",
    filters = listOf(
        constAny(0x41000000),
        fieldAccess(
            definingClass = "Landroidx/compose/ui/Modifier\$Companion;",
            name = "\$\$INSTANCE",
            opcode = Opcode.SGET_OBJECT,
        ),
        methodCall(
            definingClass = "Landroidx/compose/foundation/layout/PaddingKt;",
            name = "padding-qDBjuR0",
            location = MatchAfterImmediately(),
        ),
        methodCall(definingClass = MENU_SCAFFOLD_KT, name = "MenuFrame"),
    ),
)

internal object MoreExtensionsMenuItemFingerprint : Fingerprint(
    definingClass = MAIN_MENU_KT,
    name = "MoreExtensionsMenuItem",
    filters = listOf(SURFACE_BRIGHT),
)

internal object MainMenuNavigationLambdaFingerprint : Fingerprint(
    name = "invoke",
    returnType = OBJECT,
    parameters = listOf(OBJECT, OBJECT),
    filters = listOf(
        homeAccessPoint(),
        opcode(Opcode.IF_EQ),
        methodCall(definingClass = MENU_NAVIGATION_KT, name = "MenuNavigation"),
        methodCall(name = "HorizontalDivider-9IZ8Weo"),
    ),
)

internal object MainMenuDividerLambdaFingerprint : Fingerprint(
    name = "invoke",
    returnType = OBJECT,
    parameters = listOf(OBJECT, OBJECT),
    filters = listOf(
        homeAccessPoint(),
        opcode(Opcode.IF_EQ),
        methodCall(name = "HorizontalDivider-9IZ8Weo"),
        methodCall(definingClass = MENU_NAVIGATION_KT, name = "MenuNavigation"),
    ),
)

internal object MainMenuColumnLambdaFingerprint : Fingerprint(
    name = "invoke",
    returnType = OBJECT,
    parameters = listOf(OBJECT, OBJECT, OBJECT),
    filters = listOf(
        methodCall(definingClass = COMPOSER, name = "shouldExecute"),
        opcode(Opcode.MOVE_RESULT),
        opcode(Opcode.IF_EQZ),
        methodCall(definingClass = MENU_GROUP_KT, name = "MenuGroup"),
        methodCall(definingClass = MENU_GROUP_KT, name = "MenuGroup"),
        methodCall(definingClass = MENU_GROUP_KT, name = "MenuGroup"),
        fieldAccess(definingClass = ACCESS_POINT, name = "Browser", opcode = Opcode.SGET_OBJECT),
        methodCall(definingClass = MAIN_MENU_KT, name = "LibraryMenuGroup"),
        methodCall(definingClass = MENU_GROUP_KT, name = "MenuGroup"),
        methodCall(definingClass = MAIN_MENU_KT, name = "QuitMenuGroup"),
    ),
)

internal object MenuGroupFingerprint : Fingerprint(
    definingClass = MENU_GROUP_KT,
    name = "MenuGroup",
    filters = listOf(constAny(0x40000000)),
)

internal object BadgeFingerprint : Fingerprint(
    definingClass = MENU_ITEM_KT,
    name = "Badge",
    filters = listOf(constAny(0x41800000), constAny(0x41000000)),
)

internal object MenuBadgeItemFingerprint : Fingerprint(
    definingClass = MENU_ITEM_KT,
    name = "MenuBadgeItem",
    filters = listOf(SURFACE_BRIGHT, constAny(0x41000000), constAny(0x41800000)),
)

internal object MenuItemFingerprint : Fingerprint(
    definingClass = MENU_ITEM_KT,
    name = "MenuItem",
    filters = listOf(
        SURFACE_BRIGHT,
        constAny(0x42600000),
        constAny(0x42500000),
        callPrefix(LIST_ITEM_KT, "IconListItem"),
    ),
)

internal object MenuTextItemFingerprint : Fingerprint(
    definingClass = MENU_ITEM_KT,
    name = "MenuTextItem",
    filters = listOf(
        constAny(0x42600000),
        constAny(0x42500000),
        SURFACE_BRIGHT,
    ),
)

internal object WebExtensionMenuItemFingerprint : Fingerprint(
    definingClass = MENU_ITEM_KT,
    custom = { method, _ -> method.name.startsWith("WebExtensionMenuItem") },
    filters = listOf(
        SURFACE_BRIGHT,
        methodCall(name = "<init>", parameters = listOf("Ljava/lang/String;", FUNCTION0)),
        methodCall(name = "rememberComposableLambda"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        constAny(0x1fe57c),
        callPrefix(LIST_ITEM_KT, "IconListItem"),
    ),
)

internal object MenuItemIconLambdaFingerprint : Fingerprint(
    name = "invoke",
    returnType = OBJECT,
    parameters = listOf(OBJECT, OBJECT),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Lkotlin/jvm/functions/Function0;"),
        opcode(Opcode.IF_EQZ),
        methodCall(definingClass = "Landroidx/compose/foundation/layout/SizeKt;", name = "size-3ABfNKs"),
        methodCall(definingClass = "Lmozilla/components/compose/base/button/IconButtonKt;", name = "IconButton"),
    ),
)

internal object MenuNavItemFingerprint : Fingerprint(
    definingClass = MENU_NAVIGATION_KT,
    name = "MenuNavItem",
    filters = listOf(
        methodCall(definingClass = "Landroidx/compose/material3/IconKt;", name = "Icon-ww6aTOc"),
        fieldAccess(
            definingClass = "Lmozilla/components/compose/base/theme/AcornTypographyKt;",
            name = "defaultTypography",
            opcode = Opcode.SGET_OBJECT,
        ),
        methodCall(definingClass = "Landroidx/compose/material3/TextKt;", name = "Text-Nvy7gAk"),
    ),
)

internal object MenuFrameFingerprint : Fingerprint(
    definingClass = MENU_SCAFFOLD_KT,
    name = "MenuFrame",
    filters = listOf(constAny(0x41400000)),
)

internal object AccountMenuItemFingerprint : Fingerprint(
    definingClass = ACCOUNT_KT,
    name = "MozillaAccountMenuItem",
    filters = listOf(SURFACE_BRIGHT, constAny(0x42600000)),
)

internal object AccountSubtitleLambdaFingerprint : Fingerprint(
    name = "invoke",
    returnType = OBJECT,
    parameters = listOf(OBJECT, OBJECT),
    filters = listOf(
        callPrefix("Landroidx/compose/material3/TextKt;", "Text-"),
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Ljava/lang/String;"),
        instanceOf("Lmozilla/components/service/fxa/manager/AccountState\$AuthenticationProblem;"),
    ),
)

private const val DIALOG_FRAGMENT = "${MENU_PACKAGE}MenuDialogFragment;"
private const val MENU_FRAGMENT = "${MENU_PACKAGE}MenuFragment;"
private const val DIALOG = "Landroid/app/Dialog;"
private const val BUNDLE = "Landroid/os/Bundle;"
private const val DIALOG_INTERFACE = "Landroid/content/DialogInterface;"
private const val INSETS_COMPAT = "Landroidx/core/view/WindowInsetsCompat;"

internal object DialogFragmentCreateDialogFingerprint : Fingerprint(
    definingClass = DIALOG_FRAGMENT,
    name = "onCreateDialog",
    returnType = DIALOG,
    parameters = listOf(BUNDLE),
)

internal object MenuFragmentCreateDialogFingerprint : Fingerprint(
    definingClass = MENU_FRAGMENT,
    name = "onCreateDialog",
    returnType = DIALOG,
    parameters = listOf(BUNDLE),
)

internal object DialogFragmentViewCreatedFingerprint : Fingerprint(
    definingClass = DIALOG_FRAGMENT,
    name = "onViewCreated",
    returnType = "V",
    parameters = listOf("Landroid/view/View;", BUNDLE),
    filters = listOf(methodCall(definingClass = OBJECT, name = "getClass")),
)

internal object DialogFragmentWidthFingerprint : Fingerprint(
    definingClass = DIALOG_FRAGMENT,
    name = "calculateMenuSheetWidth",
    returnType = "I",
    parameters = listOf(),
)

internal object MenuFragmentWidthFingerprint : Fingerprint(
    definingClass = MENU_FRAGMENT,
    name = "calculateMenuSheetWidth\$1",
    returnType = "I",
    parameters = listOf(),
)

internal object DialogFragmentShowFingerprint : Fingerprint(
    name = "onShow",
    returnType = "V",
    parameters = listOf(DIALOG_INTERFACE),
    custom = { _, classDef -> classDef.fields.any { it.type == DIALOG_FRAGMENT } },
)

internal object MenuFragmentShowFingerprint : Fingerprint(
    name = "onShow",
    returnType = "V",
    parameters = listOf(DIALOG_INTERFACE),
    custom = { _, classDef -> classDef.fields.any { it.type == MENU_FRAGMENT } },
)

internal fun insetsListenerFingerprint(listenerType: String) = Fingerprint(
    definingClass = listenerType,
    name = "onApplyWindowInsets",
    returnType = INSETS_COMPAT,
    parameters = listOf("Landroid/view/View;", INSETS_COMPAT),
)

private const val TOOLBAR_MIDDLEWARE = "Lorg/mozilla/fenix/components/toolbar/BrowserToolbarMiddleware;"
private const val LIST_ITEM_KT = "Lorg/mozilla/fenix/compose/list/ListItemKt;"

internal object MenuNavigationFingerprint : Fingerprint(
    definingClass = MENU_NAVIGATION_KT,
    name = "MenuNavigation",
    filters = listOf(
        constAny(0x41400000),
        constAny(0x40800000),
        callPrefix("Landroidx/compose/foundation/layout/PaddingKt;", "padding-"),
    ),
)

internal object ToolbarEndActionsFingerprint : Fingerprint(
    definingClass = TOOLBAR_MIDDLEWARE,
    name = "buildEndBrowserActions",
    filters = listOf(
        methodCall(definingClass = "Lkotlin/collections/ArraysKt___ArraysKt;", name = "filterNotNull"),
        opcode(Opcode.NEW_INSTANCE),
    ),
)

internal object ToolbarMiddlewareFingerprint : Fingerprint(
    definingClass = TOOLBAR_MIDDLEWARE,
    name = "invoke",
    returnType = OBJECT,
    parameters = listOf(OBJECT, OBJECT, OBJECT),
    filters = listOf(
        methodCall(definingClass = OBJECT, name = "getClass"),
        methodCall(definingClass = OBJECT, name = "getClass"),
        methodCall(definingClass = OBJECT, name = "getClass"),
        instanceOf("Lorg/mozilla/fenix/components/toolbar/StartPageActions\$ProxyActivationAnimationFinished;"),
        instanceOf("Lorg/mozilla/fenix/components/toolbar/DisplayActions\$MenuClicked;"),
    ),
)

internal object WebExtensionMenuItemsPinFingerprint : Fingerprint(
    definingClass = EXTENSIONS_MENU_ITEM_KT,
    name = "WebExtensionMenuItems",
    filters = listOf(
        fieldAccess(definingClass = ACCESS_POINT, name = "External", opcode = Opcode.SGET_OBJECT),
    ),
)

internal object IconListItemFingerprint : Fingerprint(
    definingClass = LIST_ITEM_KT,
    custom = { method, _ -> method.name == "IconListItem" || method.name.startsWith("IconListItem-") },
)

internal object BadgeHideFingerprint : Fingerprint(
    definingClass = MENU_ITEM_KT,
    name = "Badge",
    returnType = "V",
    custom = { method, _ ->
        method.parameterTypes.firstOrNull()?.toString() == "Ljava/lang/String;" &&
            method.parameterTypes.size >= 5
    },
)

internal object FabContentLambdaFingerprint : Fingerprint(
    definingClass = TOOLBAR_KT,
    name = "FloatingToolbarFAB",
    filters = listOf(
        methodCall(
            name = "<init>",
            parameters = listOf("I", "Ljava/lang/String;", "Lkotlin/jvm/internal/Ref\$ObjectRef;"),
        ),
    ),
)

internal object TabLayoutScrollHelperFingerprint : Fingerprint(
    definingClass = TAB_LAYOUT_KT,
    returnType = "V",
    parameters = listOf(SCROLLABLE_STATE, "I", "F", "Z", "Z", COMPOSER, "I"),
    filters = listOf(
        methodCall(
            definingClass = SCROLL_HELPER_LAMBDA,
            name = "<init>",
            parameters = listOf("I", "Z", "Z", SCROLLABLE_STATE, "I", CONTINUATION),
            opcode = Opcode.INVOKE_DIRECT_RANGE,
        ),
    ),
)
