/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal const val THEME_COLOR_OPTION = "Lcom/microsoft/office/outlook/uikit/accessibility/ThemeColorOption;"
internal const val THEME_CATEGORY = "Lcom/microsoft/office/outlook/uikit/accessibility/ThemeColorOption\$ThemeCategory;"
internal const val THEME_STYLE = "Lcom/microsoft/office/outlook/uikit/accessibility/ThemeStyle;"
internal const val COLOR_PALETTE_MANAGER = "Lcom/microsoft/office/outlook/uikit/accessibility/ColorPaletteManager;"
internal const val THEME_PALETTE = "Lcom/microsoft/office/outlook/uicomposekit/theme/ThemePalette;"
internal const val SEARCH_PILL_STOCK_LAYOUT = "tab_item_pill_toolbar"
internal const val SEMANTIC_COLORS = "Lcom/microsoft/office/outlook/uicomposekit/theme/SemanticColors;"
internal const val TAB_DEFAULTS = "Lcom/microsoft/office/outlook/uicomposekit/ui/TabDefaults;"
internal const val TAB_COLORS = "Lcom/microsoft/office/outlook/uicomposekit/ui/TabColors;"
internal const val PILL_SWITCH_DEFAULTS = "Lcom/microsoft/office/outlook/uicomposekit/ui/PillSwitchDefaults;"
internal const val PILL_SWITCH_COLORS = "Lcom/microsoft/office/outlook/uicomposekit/ui/PillSwitchColors;"
private const val CONTEXT = "Landroid/content/Context;"
private const val BASE_ACTIVITY = "Lcom/microsoft/office/outlook/appui/core/BaseActivity;"
private const val APPEARANCE_PANE = "Lcom/microsoft/office/outlook/settingsui/compose/ui/AppearancePaneKt;"
private const val DYNAMIC_THEME_VIEW_MODEL =
    "Lcom/microsoft/office/outlook/settingsui/compose/viewmodels/DynamicThemeViewModel;"
private const val VALUES_METHOD = "\$values"

internal object ThemeStyleInitializerFingerprint : Fingerprint(
    definingClass = THEME_STYLE,
    name = "<clinit>",
    filters = listOf(
        methodCall(definingClass = THEME_STYLE, name = VALUES_METHOD),
        fieldAccess(definingClass = THEME_STYLE, name = "\$VALUES", opcode = Opcode.SPUT_OBJECT),
    ),
)

internal object ThemeColorOptionInitializerFingerprint : Fingerprint(
    definingClass = THEME_COLOR_OPTION,
    name = "<clinit>",
    filters = listOf(
        methodCall(definingClass = THEME_COLOR_OPTION, name = VALUES_METHOD),
        fieldAccess(definingClass = THEME_COLOR_OPTION, name = "\$VALUES", opcode = Opcode.SPUT_OBJECT),
    ),
)

internal object ApplyThemeFingerprint : Fingerprint(
    definingClass = COLOR_PALETTE_MANAGER,
    name = "apply",
    isStatic = true,
    parameters = listOf(CONTEXT, THEME_COLOR_OPTION),
    filters = listOf(
        methodCall(definingClass = THEME_COLOR_OPTION, name = "getStyleToApply"),
        methodCall(definingClass = "Landroid/content/res/Resources\$Theme;", name = "applyStyle"),
    ),
)

internal object ProminentDialogContextFingerprint : Fingerprint(
    definingClass = COLOR_PALETTE_MANAGER,
    name = "getProminentDialogContext",
    isStatic = true,
    parameters = listOf(CONTEXT),
    returnType = CONTEXT,
    filters = listOf(newInstance("Landroid/view/ContextThemeWrapper;")),
)

internal object ThemePaletteFingerprint : Fingerprint(
    definingClass = "Lcom/microsoft/office/outlook/uicomposekit/theme/ThemePaletteKt;",
    name = "toThemePalette",
    isStatic = true,
    returnType = THEME_PALETTE,
    parameters = listOf(THEME_STYLE, CONTEXT, "L", "L"),
    filters = listOf(newInstance("Lkotlin/NoWhenBranchMatchedException;")),
)

internal object ThemeSelectionFingerprint : Fingerprint(
    definingClass = APPEARANCE_PANE,
    isStatic = true,
    parameters = listOf(
        DYNAMIC_THEME_VIEW_MODEL,
        "Lcom/microsoft/office/outlook/settingsui/compose/viewmodels/AppearanceViewModel;",
        THEME_COLOR_OPTION,
        CONTEXT,
    ),
    filters = listOf(
        methodCall(definingClass = DYNAMIC_THEME_VIEW_MODEL, name = "cancelThemeGenerate"),
        methodCall(definingClass = COLOR_PALETTE_MANAGER, name = "apply", parameters = listOf(CONTEXT)),
        methodCall(definingClass = DYNAMIC_THEME_VIEW_MODEL, name = "setPendingRecreate"),
        methodCall(definingClass = "Landroid/app/Activity;", name = "recreate"),
        fieldAccess(opcode = Opcode.SGET_OBJECT, location = InstructionLocation.MatchAfterImmediately()),
        opcode(Opcode.RETURN_OBJECT, InstructionLocation.MatchAfterImmediately()),
    ),
)

internal object ActivityThemeCheckFingerprint : Fingerprint(
    definingClass = BASE_ACTIVITY,
    name = "onMAMPostResume",
    isStatic = false,
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(definingClass = COLOR_PALETTE_MANAGER, name = "getThemeColorOption"),
        fieldAccess(definingClass = BASE_ACTIVITY, name = "themeColor"),
        opcode(Opcode.IF_EQ),
        methodCall(definingClass = "Landroid/app/Activity;", name = "getWindow"),
        fieldAccess(definingClass = BASE_ACTIVITY, name = "recreateTask"),
        methodCall(definingClass = "Landroid/view/View;", name = "post"),
    ),
)

internal object ImmersiveThemeOverlayFingerprint : Fingerprint(
    definingClass = "Lcom/acompli/acompli/fragments/HeadlessThemeFragment;",
    name = "onCreate",
    isStatic = false,
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(
        fieldAccess(definingClass = "Lcom/microsoft/office/outlook/uikit/R\$attr;", name = "immersiveThemeOverlay"),
        methodCall(definingClass = "Landroid/content/res/TypedArray;", name = "getResourceId"),
        opcode(Opcode.MOVE_RESULT, InstructionLocation.MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/content/res/Resources\$Theme;", name = "applyStyle"),
    ),
)

internal object ThemeItemClickFingerprint : Fingerprint(
    definingClass = APPEARANCE_PANE,
    isStatic = true,
    parameters = listOf("Z", THEME_COLOR_OPTION, "Lkotlin/jvm/functions/Function0;", "Landroidx/compose/runtime/"),
    filters = listOf(
        fieldAccess(definingClass = THEME_CATEGORY, name = "PRIDE"),
        fieldAccess(definingClass = THEME_CATEGORY, name = "PHOTOS"),
        methodCall(definingClass = "Lkotlin/jvm/functions/Function0;", name = "invoke"),
    ),
)

internal object FilterButtonColorsFingerprint : Fingerprint(
    definingClass = "Lcom/microsoft/office/outlook/ui/mail/actions/MailFilterElementsKt;",
    name = "getFilterButtonColors",
    isStatic = true,
    parameters = listOf(PILL_SWITCH_COLORS, "Z", "Z", "Z", "L", "I"),
)

internal object SearchPillLayoutFingerprint : Fingerprint(
    definingClass = "Lcom/acompli/acompli/ui/search/SearchListFragmentV2;",
    name = "onCreateView",
    filters = listOf(
        fieldAccess(
            definingClass = "Lcom/microsoft/office/outlook/uikit/R\$layout;",
            name = SEARCH_PILL_STOCK_LAYOUT,
            opcode = Opcode.SGET,
        ),
    ),
)

internal object PrimaryNavigationBarFingerprint : Fingerprint(
    definingClass = SEMANTIC_COLORS,
    name = "getPrimaryNavigationBar-0d7_KjU",
    returnType = "J",
    parameters = listOf(),
)

internal object TabDefaultColorsFingerprint : Fingerprint(
    definingClass = TAB_DEFAULTS,
    name = "defaultColors",
    returnType = TAB_COLORS,
    parameters = listOf("L", "I"),
)

internal object PillSwitchDefaultColorsFingerprint : Fingerprint(
    definingClass = PILL_SWITCH_DEFAULTS,
    name = "default",
    returnType = PILL_SWITCH_COLORS,
    parameters = listOf("L", "I"),
)
