/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.moviebox.misc.buzzbox

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ijiami.editIjiamiPayload
import app.morphe.patches.shared.misc.ijiami.ijiamiPatch
import app.morphe.patches.shared.misc.requireArm64

private const val BOTTOM_TAB_TYPE = "Lcom/transsion/home/enum/BottomTabType;"
private const val COMMUNITY_BOTTOM_TAB_VALUE = "BT_COMMUNITY"
private const val COMMUNITY_BOTTOM_TAB_ENUM_NAME = "ROOM"

private const val HOME_TAB_TYPE = "Lcom/transsion/home/enum/HomeTabType;"
private const val COMMUNITY_HOME_TAB_VALUE = "CommunityTab"
private const val COMMUNITY_HOME_TAB_ENUM_NAME = "SUB_ROOM"

@Suppress("unused")
val hideBuzzBoxPatch = patch(
    name = "Hide BuzzBox",
    description = "Removes the BuzzBox tabs from the bottom navigation bar and the home screen.",
) {
    compatibleWith(AppCompatibilities.MOVIEBOX)
    dependsOn(ijiamiPatch)
    availability(requireArm64)

    execute {
        editIjiamiPayload {
            methodWithString(BOTTOM_TAB_TYPE, COMMUNITY_BOTTOM_TAB_VALUE)
                .replaceString(COMMUNITY_BOTTOM_TAB_VALUE, COMMUNITY_BOTTOM_TAB_ENUM_NAME)
            methodWithString(HOME_TAB_TYPE, COMMUNITY_HOME_TAB_VALUE)
                .replaceString(COMMUNITY_HOME_TAB_VALUE, COMMUNITY_HOME_TAB_ENUM_NAME)
        }
    }
}
