/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.hypic.misc.fix.signature

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal object ApkFileSignatureReportFingerprint : Fingerprint(
    name = "invokeSuspend",
    strings = listOf("getAppSignature2="),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/bytedance/ies/painter/sdk/jni/PainterInterface;",
            name = "nativeGetAppSignature2",
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
    ),
)

internal object SecuritySdkInitFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("MSConfig init error!"),
    filters = listOf(
        newInstance("Lcom/bytedance/mobsec/metasec/ov/a${'$'}a;"),
    ),
)
