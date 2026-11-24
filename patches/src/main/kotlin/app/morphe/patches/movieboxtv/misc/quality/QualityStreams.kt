/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.movieboxtv.misc.quality

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.classDefByOrNull
import app.morphe.patcher.patch.packageMetadata
import app.morphe.patcher.patch.resourceFile
import app.morphe.patcher.util.getReference
import app.morphe.patches.shared.misc.signature.stockSigningCertificate
import app.morphe.patches.shared.misc.smali.addStaticMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val PLAY_INFO_REWRITER = "Lhx/rewrite/TvPlayInfo;"
private const val CERTIFICATE_ASSET = "assets/hx.cer"
private const val STRING = "Ljava/lang/String;"
private const val BYTE_ARRAY = "[B"
private const val COUNTRY_INTERCEPTOR_RETURNS = 4

internal fun installQualityStreams() {
    resourceFile(CERTIFICATE_ASSET, copy = false).writeBytes(packageMetadata.stockSigningCertificate().encoded)

    val response = ResponseToStringFingerprint.originalClassDef
    val request = RequestToStringFingerprint.originalClassDef
    val countryHeader = CountryHeaderFingerprint.classDef
    val intercept = countryHeader.methods.single { it.name == "intercept" }
    if (intercept.returnType != response.type) {
        throw PatchException("Country header interceptor returns ${intercept.returnType}, expected ${response.type}")
    }
    val returns = intercept.implementation!!.instructions.withIndex()
        .filter { it.value.opcode == Opcode.RETURN_OBJECT }
    if (returns.size != COUNTRY_INTERCEPTOR_RETURNS) {
        throw PatchException("Country header interceptor has ${returns.size} returns, expected $COUNTRY_INTERCEPTOR_RETURNS")
    }
    val rewrite = countryHeader.addStaticMethod(
        "rewritePlayInfo",
        listOf(response.type),
        response.type,
        5,
        playbackResponseRewrite(response, request),
    )
    returns.reversed().forEach { (index, instruction) ->
        val register = (instruction as OneRegisterInstruction).registerA
        intercept.replaceInstruction(index, "invoke-static/range { v$register .. v$register }, $rewrite")
        intercept.addInstructions(
            index + 1,
            """
                move-result-object v$register
                return-object v$register
            """,
        )
    }
}

private fun playbackResponseRewrite(response: ClassDef, request: ClassDef): String {
    val responseBody = response.methods.map { it.returnType }.distinct().mapNotNull(::classDefByOrNull).single { classDef ->
        "Ljava/io/Closeable;" in classDef.interfaces && classDef.methods.any {
            it.name == "bytes" && it.parameterTypes.isEmpty() && it.returnType == BYTE_ARRAY
        }
    }
    val readRequest = response.noArgumentMethodReturning(request.type)
    val readBody = response.noArgumentMethodReturning(responseBody.type)
    val url = response.methods.single { it.name == "toString" }.implementation!!.instructions
        .mapNotNull { it.getReference<MethodReference>() }
        .single { it.definingClass == request.type && it.parameterTypes.isEmpty() }
    val contentType = responseBody.methods.single { it.name == "contentType" && it.parameterTypes.isEmpty() }
    val readBytes = responseBody.methods.single { it.name == "bytes" && it.parameterTypes.isEmpty() }
    val createBody = responseBody.methods.single {
        AccessFlags.STATIC.isSet(it.accessFlags) && it.returnType == responseBody.type &&
            it.parameterTypes == listOf(BYTE_ARRAY, contentType.returnType)
    }
    val newBuilder = response.methods.single {
        it.parameterTypes.isEmpty() && it.returnType.startsWith(response.type.removeSuffix(";") + "$")
    }
    val builder = classDefBy(newBuilder.returnType)
    val setBody = builder.methods.single {
        it.parameterTypes == listOf(responseBody.type) && it.returnType == builder.type
    }
    val headers = response.instanceFields.single { field ->
        classDefByOrNull(field.type)?.interfaces?.contains("Ljava/lang/Iterable;") == true
    }
    val headersField = builder.instanceFields.single {
        it.type.startsWith(headers.type.removeSuffix(";") + "$")
    }
    if (!AccessFlags.PUBLIC.isSet(headersField.accessFlags)) {
        throw PatchException("Response headers builder field is not public: $headersField")
    }
    val headersBuilder = classDefBy(headersField.type)
    val removeHeader = headersBuilder.methods.single { method ->
        method.parameterTypes == listOf(STRING) && method.returnType == headersBuilder.type &&
            method.implementation!!.instructions.any { it.getReference<StringReference>()?.string == "name" }
    }
    val build = builder.noArgumentMethodReturning(response.type)
    return """
        invoke-virtual { p0 }, $readRequest
        move-result-object v0
        invoke-virtual { v0 }, $url
        move-result-object v0
        invoke-virtual { v0 }, Ljava/lang/Object;->toString()Ljava/lang/String;
        move-result-object v0
        invoke-static { v0 }, $PLAY_INFO_REWRITER->isPlayInfo(Ljava/lang/String;)Z
        move-result v1
        if-eqz v1, :unchanged
        invoke-virtual { p0 }, $readBody
        move-result-object v1
        if-eqz v1, :unchanged
        invoke-virtual { v1 }, $contentType
        move-result-object v2
        invoke-virtual { v1 }, $readBytes
        move-result-object v1
        invoke-static { v0, v1 }, $PLAY_INFO_REWRITER->rewrite(Ljava/lang/String;[B)[B
        move-result-object v0
        invoke-static { v0, v2 }, $createBody
        move-result-object v0
        invoke-virtual { p0 }, $newBuilder
        move-result-object v1
        invoke-virtual { v1, v0 }, $setBody
        move-result-object v0
        const-string v1, "Content-Length"
        iget-object v2, v0, $headersField
        invoke-virtual { v2, v1 }, $removeHeader
        invoke-virtual { v0 }, $build
        move-result-object v0
        return-object v0
        :unchanged
        return-object p0
    """.trimIndent()
}

private fun ClassDef.noArgumentMethodReturning(type: String): Method = methods.single {
    !AccessFlags.STATIC.isSet(it.accessFlags) && it.parameterTypes.isEmpty() && it.returnType == type
}
