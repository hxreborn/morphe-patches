/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.scheduling

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.instanceOf
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal object SnoozeOptionsFingerprint : Fingerprint(
    strings = listOf("Unsupported week start given "),
    filters = listOf(
        instanceOf("Luniffi/mail_uniffi/SnoozeTime\$Custom;"),
        opcode(Opcode.SGET_OBJECT),
        methodCall("Ljava/util/List;->contains(Ljava/lang/Object;)Z"),
        opcode(Opcode.SGET_OBJECT),
    ),
)

internal object ScheduleSendCustomOptionFingerprint : Fingerprint(
    definingClass = "Luniffi/mail_uniffi/DraftScheduleSendOptions;",
    name = "isCustomOptionAvailable",
    returnType = "Z",
)
