/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.media

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

private const val DISAPPEARING_MESSAGE_FLAG = 27

internal object MediaMenuStateFingerprint : Fingerprint(
    name = "toString",
    filters = listOf(string("MediaDetailsMenuState(setLockScreenVisible=")),
)

internal fun mediaMenuStateBuilderFingerprint(menuStateType: String) = Fingerprint(
    returnType = menuStateType,
    parameters = listOf("L", "Lcom/viber/voip/messages/conversation/ConversationItemLoaderEntity;", "Z"),
    filters = listOf(
        literal(DISAPPEARING_MESSAGE_FLAG),
        methodCall(name = "a", parameters = listOf("I"), returnType = "Z"),
        methodCall(name = "a", parameters = listOf("I"), returnType = "Z"),
        methodCall(name = "isNotShareablePublicAccount"),
    ),
)
