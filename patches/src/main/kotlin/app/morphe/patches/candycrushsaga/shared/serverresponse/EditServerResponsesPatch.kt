/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.shared.serverresponse

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.smali.addFieldReader
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

internal const val SERVER_RESPONSES = "Lapp/hxreborn/extension/candycrushsaga/ServerResponses;"

internal val editServerResponsesPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")
    execute {
        val match = ReadDataFingerprint.matchAll(1..1).single()
        val (openStream) = match.instructionMatches
        val method = match.method
        val body = classDefBy(method.definingClass).fields.single { it.type == "[B" }
        val connection = method.getInstruction<FiveRegisterInstruction>(openStream.index).registerC
        method.replaceInstruction(
            openStream.index,
            "invoke-static { v$connection, p0 }, " +
                "$SERVER_RESPONSES->open(Ljava/net/HttpURLConnection;Ljava/lang/Object;)Ljava/io/InputStream;",
        )
        val readBody = mutableClassDefBy(body.definingClass).addFieldReader(body)
        mutableClassDefBy(SERVER_RESPONSES).replaceStub("requestBody", 1, """
            check-cast p0, ${body.definingClass}
            invoke-virtual { p0 }, $readBody
            move-result-object p0
            return-object p0
        """)
    }
}
