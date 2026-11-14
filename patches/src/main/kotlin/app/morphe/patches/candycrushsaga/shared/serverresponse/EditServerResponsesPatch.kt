/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.shared.serverresponse

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.candycrushsaga.shared.isCandyCrushSodaSaga
import app.morphe.patches.shared.misc.smali.addFieldReader
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

internal const val SERVER_RESPONSES = "Lapp/hxreborn/extension/candycrushsaga/ServerResponses;"

internal val editServerResponsesPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")
    execute {
        val soda = isCandyCrushSodaSaga(packageMetadata.packageName)
        mutableClassDefBy(SERVER_RESPONSES).replaceStub("isSodaSaga", 1, """
            const/4 v0, ${if (soda) "0x1" else "0x0"}
            return v0
        """)
        val responseStreamMatch = ReadDataFingerprint.matchSingle()
        val (openStream) = responseStreamMatch.instructionMatches
        val readResponse = responseStreamMatch.method
        val requestBody = classDefBy(readResponse.definingClass).fields.single { it.type == "[B" }
        val connectionRegister = readResponse.getInstruction<FiveRegisterInstruction>(openStream.index).registerC
        readResponse.replaceInstruction(
            openStream.index,
            "invoke-static { v$connectionRegister, p0 }, " +
                "$SERVER_RESPONSES->open(Ljava/net/HttpURLConnection;Ljava/lang/Object;)Ljava/io/InputStream;",
        )
        val readRequestBody = mutableClassDefBy(requestBody.definingClass).addFieldReader(requestBody)
        mutableClassDefBy(SERVER_RESPONSES).replaceStub("requestBody", 1, """
            check-cast p0, ${requestBody.definingClass}
            invoke-virtual { p0 }, $readRequestBody
            move-result-object p0
            return-object p0
        """)
    }
}
