/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.login

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.terabox.misc.fix.signature.spoofSignaturePatch
import app.morphe.util.findElementByAttributeValueOrThrow
import app.morphe.util.findInstructionIndicesReversed
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/terabox/GoogleLoginChromeClient;"

private const val LOADING_INDICATOR_SIZE = "64.0dp"

private val loginLoadingIndicatorPatch = resourcePatch {
    execute {
        document("res/layout/activity_account_webview.xml").use { document ->
            document.getElementsByTagName("com.dubox.drive.ui.widget.DegradedPerformanceLottieView")
                .findElementByAttributeValueOrThrow("android:id", "@id/image_loading")
                .apply {
                    setAttribute("android:layout_width", LOADING_INDICATOR_SIZE)
                    setAttribute("android:layout_height", LOADING_INDICATOR_SIZE)
                    removeAttribute("android:paddingTop")
                    removeAttribute("android:paddingBottom")
                }
        }
    }
}

@Suppress("unused")
val fixGoogleLoginPatch = bytecodePatch(
    name = "Fix Google login",
    description = "Restores signing in with a Google account.",
) {
    compatibleWith(AppCompatibilities.TERABOX)
    dependsOn(loginLoadingIndicatorPatch, spoofSignaturePatch)
    extendWith("extensions/extension.mpe")

    execute {
        NativeAccountPageFingerprint.matchSingle().method.returnEarly(false)

        OfflinePackageManifestFingerprint.matchSingle().method.returnEarly(null)

        WebViewSetupFingerprint.matchSingle().method.apply {
            val registers = getFreeRegisterProvider(0, 2)
            val enabled = registers.getFreeRegister4Bit()
            val receiver = registers.getFreeRegister4Bit()

            addInstructions(
                0,
                """
                    const/4 v$enabled, 0x1
                    invoke-virtual { p1 }, Landroid/webkit/WebView;->getSettings()Landroid/webkit/WebSettings;
                    move-result-object v$receiver
                    invoke-virtual { v$receiver, v$enabled }, Landroid/webkit/WebSettings;->setSupportMultipleWindows(Z)V
                    invoke-virtual { v$receiver, v$enabled }, Landroid/webkit/WebSettings;->setJavaScriptCanOpenWindowsAutomatically(Z)V
                    invoke-static { }, Landroid/webkit/CookieManager;->getInstance()Landroid/webkit/CookieManager;
                    move-result-object v$receiver
                    invoke-virtual { v$receiver, p1, v$enabled }, Landroid/webkit/CookieManager;->setAcceptThirdPartyCookies(Landroid/webkit/WebView;Z)V
                """,
            )
        }

        WebViewUserAgentFingerprint.matchSingle().method.apply {
            findInstructionIndicesReversed {
                opcode == Opcode.INVOKE_VIRTUAL && getReference<MethodReference>()?.name == "setUserAgentString"
            }.forEach { index ->
                val instruction = getInstruction<FiveRegisterInstruction>(index)

                replaceInstruction(
                    index,
                    "invoke-static { v${instruction.registerC}, v${instruction.registerD} }, " +
                        "$EXTENSION_CLASS->setBrowserUserAgent(" +
                        "Landroid/webkit/WebSettings;Ljava/lang/String;)V",
                )
            }
        }

        WebChromeClientFingerprint.matchSingle().classDef.setSuperClass(EXTENSION_CLASS)
    }
}
