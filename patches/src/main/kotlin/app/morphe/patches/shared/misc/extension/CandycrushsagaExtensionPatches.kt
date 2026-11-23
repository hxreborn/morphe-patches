/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val candycrushsagaBoostersExtensionPatch = patch {
    extendWith("extensions/hxreborn/candycrushsaga-boosters.mpe")
}

internal val candycrushsagaGameModifiersExtensionPatch = patch {
    extendWith("extensions/hxreborn/candycrushsaga-game-modifiers.mpe")
}

internal val candycrushsagaServerResponsesExtensionPatch = patch {
    extendWith("extensions/hxreborn/candycrushsaga-server-responses.mpe")
}
