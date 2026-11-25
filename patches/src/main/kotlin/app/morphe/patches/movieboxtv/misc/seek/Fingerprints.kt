/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.movieboxtv.misc.seek

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal const val FULL_SCREEN_FRAGMENT = "Lcom/transsion/tvui/fragment/FullScreenFragment;"
internal const val PLAYER_CONTROLLER = "Lcom/transsion/tvplayer/TvPlayerController;"
internal const val STRING = "Ljava/lang/String;"
internal const val LIST = "Ljava/util/List;"
internal const val STOCK_SKIP_LENGTH_SECONDS = 10
internal const val MILLIS_PER_SECOND = 1000
internal val OPTION_CONSTRUCTOR_PARAMETERS = listOf(STRING, STRING, "Z", "I", "Z", "Z")
private const val SETTING_TYPE = "Lcom/transsion/tvplayer/TvPlayerSettingsPanel\$SettingType;"
private const val PLAYER_VIEW = "Lcom/transsion/tvplayer/TvPlayerView;"
private const val STOCK_SKIP_LENGTH_MILLIS = STOCK_SKIP_LENGTH_SECONDS.toLong() * MILLIS_PER_SECOND
private const val NORMAL_SPEED_LABEL = "tv_player_speed_1x"
private const val SPEED_TAB = "SPEED"

internal object PlayerViewFieldFingerprint : Fingerprint(
    definingClass = PLAYER_CONTROLLER,
    name = "<init>",
    filters = listOf(fieldAccess(definingClass = PLAYER_CONTROLLER, type = PLAYER_VIEW, opcode = Opcode.IPUT_OBJECT)),
)

internal object DpadSkipFingerprint : Fingerprint(
    definingClass = PLAYER_CONTROLLER,
    isStatic = false,
    returnType = "Z",
    parameters = listOf("I"),
    filters = listOf(literal(STOCK_SKIP_LENGTH_MILLIS)),
)

internal object MediaKeySkipFingerprint : Fingerprint(
    definingClass = PLAYER_CONTROLLER,
    isStatic = false,
    returnType = "Z",
    parameters = listOf("I", "Landroid/view/KeyEvent;"),
    filters = listOf(literal(STOCK_SKIP_LENGTH_MILLIS)),
)

internal object SpeedOptionsFingerprint : Fingerprint(
    definingClass = FULL_SCREEN_FRAGMENT,
    isStatic = false,
    returnType = LIST,
    parameters = listOf(),
    filters = listOf(
        fieldAccess(name = NORMAL_SPEED_LABEL, type = "I"),
        opcode(Opcode.RETURN_OBJECT),
    ),
)

internal object SettingSelectedFingerprint : Fingerprint(
    definingClass = FULL_SCREEN_FRAGMENT,
    isStatic = false,
    returnType = "V",
    parameters = listOf(SETTING_TYPE, "L"),
)

internal fun refreshSpeedOptionsFingerprint(speedOptions: String) = Fingerprint(
    definingClass = FULL_SCREEN_FRAGMENT,
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(definingClass = SETTING_TYPE, name = SPEED_TAB),
        methodCall(definingClass = FULL_SCREEN_FRAGMENT, name = speedOptions, parameters = listOf(), returnType = LIST),
    ),
)

internal fun optionFieldsFingerprint(option: String) = Fingerprint(
    definingClass = option,
    name = "<init>",
    parameters = OPTION_CONSTRUCTOR_PARAMETERS,
    filters = listOf(
        fieldAccess(definingClass = option, type = STRING, opcode = Opcode.IPUT_OBJECT),
        fieldAccess(definingClass = option, type = STRING, opcode = Opcode.IPUT_OBJECT),
    ),
)
