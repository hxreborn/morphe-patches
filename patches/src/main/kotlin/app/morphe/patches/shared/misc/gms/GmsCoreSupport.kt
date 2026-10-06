/*
 * Copyright (C) 2026 Morphe
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from MorpheApp/morphe-patches:
 * https://github.com/MorpheApp/morphe-patches/commit/7ae360fd3ee25cf05c530329a179f82b1678c19a
 * Commit 7ae360fd3ee25cf05c530329a179f82b1678c19a (2026-08-10),
 * patches/src/main/kotlin/app/morphe/patches/shared/misc/gms/GmsCoreSupportPatch.kt
 */
package app.morphe.patches.shared.misc.gms

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.string
import app.morphe.patches.shared.misc.signature.replaceString
import app.morphe.patches.shared.misc.signature.stockSigningCertificate
import app.morphe.util.matchAllMethodIndicesForEach
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import org.w3c.dom.Element
import java.security.MessageDigest

internal const val GMS_CORE_SIGN_IN_CLASS = "Lapp/hxreborn/extension/shared/GmsCoreSignIn;"

private const val GMS_PACKAGE_NAME = "com.google.android.gms"

private const val GMS_CORE_PACKAGE_NAME = "app.revanced.android.gms"

internal val gmsCoreSpoofedSignaturePatch = resourcePatch {
    execute {
        val certificateSha1 = MessageDigest.getInstance("SHA-1")
            .digest(packageMetadata.stockSigningCertificate().encoded)
            .joinToString("") { "%02x".format(it) }

        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0)

            val metadata = document.createElement("meta-data") as Element
            metadata.setAttribute("android:name", "$GMS_CORE_PACKAGE_NAME.SPOOFED_PACKAGE_SIGNATURE")
            metadata.setAttribute("android:value", certificateSha1)
            application.appendChild(metadata)
        }
    }
}

internal fun BytecodePatchContext.bindGmsCoreSignInTypes() {
    val resultClass = ObfuscatedResultToStringFingerprint.matchAllOrNull()
        ?.map { it.classDef.type }
        ?.distinct()
        ?.let { types -> types.singleOrNull() ?: throw PatchException("Found several Result classes: $types") }
        ?: KOTLIN_RESULT_CLASS

    val placeholderTypes = mapOf(
        "<success-class>" to GetCredentialSuccessToStringFingerprint.matchSingle().classDef.type,
        "<failure-class>" to GetCredentialFailureToStringFingerprint.matchSingle().classDef.type,
        "<result-class>" to resultClass,
    ).mapValues { (_, type) -> type.toJavaClassName() }

    val signInClass = mutableClassDefBy(GMS_CORE_SIGN_IN_CLASS)
    placeholderTypes.forEach { (placeholder, className) ->
        signInClass.methods
            .single { method -> method.implementation?.instructions?.any { it.isString(placeholder) } == true }
            .replaceString(placeholder, className)
    }
}

internal fun BytecodePatchContext.redirectGmsPackageToGmsCore() {
    string(GMS_PACKAGE_NAME).matchAllMethodIndicesForEach(requireMatches = false) { index ->
        val register = getInstruction<OneRegisterInstruction>(index).registerA
        replaceInstruction(index, "const-string v$register, \"$GMS_CORE_PACKAGE_NAME\"")
    }
}

private fun Instruction.isString(value: String) =
    (opcode == Opcode.CONST_STRING || opcode == Opcode.CONST_STRING_JUMBO) &&
        ((this as? ReferenceInstruction)?.reference as? StringReference)?.string == value

private fun String.toJavaClassName() = removePrefix("L").removeSuffix(";").replace('/', '.')
