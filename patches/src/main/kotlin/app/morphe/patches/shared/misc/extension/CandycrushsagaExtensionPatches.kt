/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val candycrushsagaBoostersExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/candycrushsaga-boosters.mpe")
}

internal val candycrushsagaGameModifiersExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/candycrushsaga-game-modifiers.mpe")
}

internal val candycrushsagaServerResponsesExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/candycrushsaga-server-responses.mpe")
}
