/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.gms

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.packageMetadata
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.teraboxGmsCoreSupportExtensionPatch
import app.morphe.patches.shared.misc.gms.gmsCoreSpoofedSignaturePatch
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.patches.terabox.misc.login.webLoginPagePatch
import app.morphe.patches.terabox.misc.settings.markPatchApplied
import app.morphe.patches.terabox.misc.settings.patchesSettingsPatch

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/terabox/GmsCoreSignIn;"
private const val PATCH_SETTINGS_CLASS = "Lapp/hxreborn/extension/terabox/PatchSettings;"

@Suppress("unused")
val gmsCoreSupportPatch: Patch = patch(
    name = "GmsCore support",
    description = "Signs in through GmsCore instead of Google Play Services. Requires GmsCore to be installed.",
) {
    compatibleWith(AppCompatibilities.TERABOX)
    dependsOn(teraboxGmsCoreSupportExtensionPatch, gmsCoreSpoofedSignaturePatch, patchesSettingsPatch, webLoginPagePatch)

    execute {
        markPatchApplied("gmsCoreSupport", gmsCoreSupportPatch)
        AutomaticGoogleLoginFingerprint.method.returnEarly()
        listOf(GoogleLoginFingerprint, CredentialLoginFingerprint).forEach { fingerprint ->
            val match = fingerprint.match()
            val clientId = match.classDef.fields.single { it.type == "Ljava/lang/String;" }
            match.method.apply {
                val value = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
                addInstructions(
                    0,
                    """
                        iget-object v$value, p0, ${clientId.definingClass}->${clientId.name}:${clientId.type}
                        invoke-static {p1, v$value, p2}, $EXTENSION_CLASS->signIn(Landroid/app/Activity;Ljava/lang/String;Ljava/lang/Object;)V
                        return-void
                    """,
                )
            }
        }
        val listener = classDefBy(LISTENER_CLASS)
        fun listenerMethod(vararg parameters: String) = listener.methods.single {
            it.returnType == "V" && it.parameterTypes.map(CharSequence::toString) == parameters.toList()
        }
        val success = listenerMethod("Ljava/lang/String;", "Ljava/lang/String;")
        val failure = listenerMethod("Ljava/lang/String;", "I")
        val extension = mutableClassDefBy(EXTENSION_CLASS)
        extension.replaceStub(
            "deliverSuccess", 3,
            """
                check-cast p0, ${listener.type}
                invoke-interface {p0, p1, p2}, ${listener.type}->${success.name}(Ljava/lang/String;Ljava/lang/String;)V
                return-void
            """,
        )
        extension.replaceStub(
            "deliverFailure", 3,
            """
                check-cast p0, ${listener.type}
                invoke-interface {p0, p1, p2}, ${listener.type}->${failure.name}(Ljava/lang/String;I)V
                return-void
            """,
        )
    }

    finalize {
        val installedPackageName = document("AndroidManifest.xml").use { it.documentElement.getAttribute("package") }
        if (installedPackageName != packageMetadata.packageName) return@finalize
        val settings = mutableClassDefBy(PATCH_SETTINGS_CLASS)
        settings.replaceStub(
            "hasGmsCoreSignIn", 1,
            """
                const/4 v0, 0x1
                return v0
            """,
        )
        settings.replaceStub(
            "startGmsCoreSignIn", 3,
            """
                invoke-static {p0, p1, p2}, $EXTENSION_CLASS->signIn(Landroid/app/Activity;Ljava/lang/String;Ljava/lang/Object;)V
                return-void
            """,
        )
    }
}
