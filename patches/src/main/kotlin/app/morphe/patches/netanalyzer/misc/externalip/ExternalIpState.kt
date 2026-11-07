/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.externalip

import app.morphe.patcher.Match
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.getReference
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTERNAL_IP_CLASS = "Lapp/hxreborn/extension/netanalyzer/ExternalIp;"

internal fun BytecodePatchContext.replaceExternalIpStateStub(stateRead: Match.InstructionMatch) {
    val stateValue = stateRead.instruction.getReference<MethodReference>()!!
    mutableClassDefBy(EXTERNAL_IP_CLASS).replaceStub(
        "stateValue",
        1,
        """
            check-cast p0, ${stateValue.definingClass}
            ${stateRead.instruction.opcode.name} { p0 }, ${descriptor(stateValue)}
            move-result-object p0
            return-object p0
        """,
    )
}
