/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.narababy.shared

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object DatabaseSnapshotToMapFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Lcom/facebook/react/bridge/WritableMap;",
    parameters = listOf("Lcom/google/firebase/database/DataSnapshot;"),
    strings = listOf("exists", "hasChildren", "childrenCount"),
)
