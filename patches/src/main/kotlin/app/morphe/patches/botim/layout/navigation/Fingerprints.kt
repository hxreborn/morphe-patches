/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.botim.layout.navigation

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal const val MAIN_ACTIVITY = "Lim/thebot/main/main_page/MainActivity;"

internal object TabFragmentFactoryFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Landroidx/fragment/app/Fragment;",
    filters = listOf(
        opcode(Opcode.INSTANCE_OF),
        newInstance("Lim/thebot/main/main_page/home/HomeFragment;"),
        opcode(Opcode.INSTANCE_OF),
        newInstance("Lim/thebot/main/main_page/calls/CallsFragment;"),
    ),
    definingClass = MAIN_ACTIVITY,
)

internal object TabMapFingerprint : Fingerprint(
    returnType = "Ljava/util/Map;",
    parameters = listOf(),
    definingClass = MAIN_ACTIVITY,
)

internal object SelectedTabInitializerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    strings = listOf("key_selected_tab"),
    definingClass = MAIN_ACTIVITY,
)
