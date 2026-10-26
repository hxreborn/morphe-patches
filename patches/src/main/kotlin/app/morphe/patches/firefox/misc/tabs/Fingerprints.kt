/*
 * Copyright (C) 2026 heval99
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from heval99/Heval-Morphe-Patches:
 * https://github.com/heval99/Heval-Morphe-Patches/commit/28097b43dbc8de99bb516a44f84708ae6cbb0f64
 * Commit 28097b43dbc8de99bb516a44f84708ae6cbb0f64 (2026-10-25),
 * patches/src/main/kotlin/app/template/patches/firefox/misc/tabs/KeepBackgroundTabsLoadedPatch.kt
 */
package app.morphe.patches.firefox.misc.tabs

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.Opcode

internal const val INSTANCE_INFO = "Lorg/mozilla/gecko/process/ServiceAllocator\$InstanceInfo;"
internal const val PRIORITY_LEVEL = "Lorg/mozilla/gecko/process/ServiceAllocator\$PriorityLevel;"

internal object SetPriorityLevelFingerprint : Fingerprint(
    definingClass = INSTANCE_INFO,
    name = "setPriorityLevel",
    returnType = "Z",
    parameters = listOf(PRIORITY_LEVEL, "I"),
    filters = listOf(fieldAccess(opcode = Opcode.IPUT_OBJECT, definingClass = INSTANCE_INFO, name = "mCurrentPriority")),
)
