/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.firefox.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

private const val ENUM = "Ljava/lang/Enum;"
internal const val ACCENT_COLOR = "Lapp/hxreborn/extension/firefox/AccentColor;"
private const val COLOR_SCHEME = "Landroidx/compose/material3/ColorScheme;"
private const val WEBCOMPAT_UI = "Lorg/mozilla/fenix/webcompat/ui/"

internal object ThemeValuesFingerprint : Fingerprint(
    name = "<clinit>",
    strings = listOf("Light", "Private"),
    filters = listOf(
        string("Dark"),
        methodCall(definingClass = ENUM, name = "<init>", location = MatchAfterWithin(2)),
        fieldAccess(opcode = Opcode.SPUT_OBJECT, location = MatchAfterImmediately()),
        string("Oled"),
        methodCall(definingClass = ENUM, name = "<init>", location = MatchAfterWithin(2)),
    ),
)

private fun reportBrokenSiteColorFingerprint(className: String, methodName: String, role: String) = Fingerprint(
    definingClass = "$WEBCOMPAT_UI$className;",
    name = methodName,
    filters = listOf(fieldAccess(opcode = Opcode.IGET_WIDE, definingClass = COLOR_SCHEME, name = role)),
)

internal val reportBrokenSitePageColorFingerprints = listOf(
    reportBrokenSiteColorFingerprint("WebCompatReporterKt", "WebCompatReporter", "surfaceBright"),
    reportBrokenSiteColorFingerprint("WebCompatReporterKt", "TempAppBar", "surfaceBright"),
)

internal val reportBrokenSiteCardColorFingerprints = listOf(
    reportBrokenSiteColorFingerprint("BrokenSiteReasonListItemKt", "BrokenSiteReasonListItem", "surface"),
    reportBrokenSiteColorFingerprint("ReadOnlyUrlFieldKt", "ReadOnlyUrlField", "surface"),
)

private fun violetColorsFingerprint(colorsClass: String) = Fingerprint(
    definingClass = colorsClass,
    name = "<clinit>",
    filters = listOf(fieldAccess(opcode = Opcode.SPUT_WIDE, definingClass = colorsClass, name = "Violet50")),
)

internal val violetColorsFingerprints = listOf(
    violetColorsFingerprint("Lmozilla/components/ui/colors/NovaColors;"),
    violetColorsFingerprint("Lmozilla/components/ui/colors/PhotonColors;"),
)

internal object ApplicationOnCreateFingerprint : Fingerprint(
    definingClass = "Lorg/mozilla/fenix/FenixApplication;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf(),
)

internal object ChosenAccentColorFingerprint : Fingerprint(
    definingClass = ACCENT_COLOR,
    name = "chosenColor",
    returnType = "Ljava/lang/String;",
)

internal object ApplyOverrideConfigurationFingerprint : Fingerprint(
    definingClass = "Landroidx/appcompat/view/ContextThemeWrapper;",
    name = "applyOverrideConfiguration",
    returnType = "V",
    parameters = listOf("Landroid/content/res/Configuration;"),
)
