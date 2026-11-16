/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.quranify.misc.integrity

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import org.w3c.dom.Element

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/quranify/LocalApiServer;"
private const val NETWORK_SECURITY_CONFIG_ATTRIBUTE = "android:networkSecurityConfig"
private const val NETWORK_SECURITY_CONFIG_NAME = "hx_quranify_network_security"
private const val LOCAL_CLEARTEXT_CONFIG =
    """<network-security-config><domain-config cleartextTrafficPermitted="true">""" +
        """<domain>127.0.0.1</domain></domain-config></network-security-config>"""

private val allowLocalCleartextPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element
            if (application.hasAttribute(NETWORK_SECURITY_CONFIG_ATTRIBUTE)) {
                throw PatchException("Quranify already declares a network security configuration")
            }
            application.setAttribute(NETWORK_SECURITY_CONFIG_ATTRIBUTE, "@xml/$NETWORK_SECURITY_CONFIG_NAME")
        }
        get("res/xml/$NETWORK_SECURITY_CONFIG_NAME.xml").writeText(LOCAL_CLEARTEXT_CONFIG)
    }
}

val bypassIntegrityCheckPatch = bytecodePatch {
    compatibleWith(AppCompatibilities.QURANIFY)
    extendWith("extensions/extension.mpe")
    dependsOn(allowLocalCleartextPatch)

    execute {
        ApiEndpointsFingerprint.matchSingle().apply {
            val (_, _, decodedBaseUrl) = instructionMatches
            val register = (decodedBaseUrl.instruction as OneRegisterInstruction).registerA
            method.addInstructions(
                decodedBaseUrl.index + 1,
                """
                    invoke-static {}, $EXTENSION_CLASS->baseUrl()Ljava/lang/String;
                    move-result-object v$register
                """,
            )
        }
        FetchSessionTokenFingerprint.matchSingle().method.addInstructions(
            0,
            """
                const-string v0, "morphe"
                return-object v0
            """,
        )
    }
}
