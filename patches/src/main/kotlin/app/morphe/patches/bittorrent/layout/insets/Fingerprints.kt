/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bittorrent.layout.insets

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal object ActivityContentFingerprint : Fingerprint(
    name = "onCreate",
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(
        methodCall(definingClass = "Landroidx/viewbinding/ViewBinding;", name = "getRoot"),
        methodCall(name = "setContentView", parameters = listOf("Landroid/view/View;"), returnType = "V"),
    ),
)

internal object PlaylistSheetFingerprint : Fingerprint(
    name = "onCreateView",
    parameters = listOf("Landroid/view/LayoutInflater;", "Landroid/view/ViewGroup;", "Landroid/os/Bundle;"),
    filters = listOf(
        fieldAccess(name = "layout_playlist_bottom_sheet", opcode = Opcode.SGET),
        methodCall(definingClass = "Landroid/view/LayoutInflater;", name = "inflate"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
)
