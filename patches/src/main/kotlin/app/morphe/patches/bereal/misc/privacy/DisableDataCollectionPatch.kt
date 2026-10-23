/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.disableCrashlyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableFirebaseSessionsPatch
import app.morphe.patches.shared.misc.analytics.putApplicationMetaData
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.util.asSequence
import app.morphe.util.setExtensionIsPatchIncluded
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import org.w3c.dom.Element

private val removedPermissions = setOf(
    "com.google.android.gms.permission.AD_ID",
    "android.permission.ACCESS_ADSERVICES_AD_ID",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
    "android.permission.ACCESS_ADSERVICES_TOPICS",
    "com.amazon.privacypass.ATTEST",
    "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
)

private val removePermissionsAndCollectionFlagsPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.documentElement
            val declared = manifest.childNodes.asSequence().filterIsInstance<Element>()
                .filter { it.tagName == "uses-permission" && it.getAttribute("android:name") in removedPermissions }
                .toList()
            val missing = removedPermissions - declared.map { it.getAttribute("android:name") }.toSet()
            if (missing.isNotEmpty()) throw PatchException("Permissions not declared: $missing")
            declared.forEach { manifest.removeChild(it) }

            document.putApplicationMetaData("delivery_metrics_exported_to_big_query_enabled", "false")
        }
    }
}

private val dataCollectionExtensionPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")
}

@Suppress("unused")
val disableDataCollectionPatch = bytecodePatch(
    name = "Disable data collection",
    description = "Turns off ad tracking, Firebase crash reports and sessions, Amplitude and Datadog analytics, Adjust " +
        "attribution and the Shake bug reporter.",
) {
    compatibleWith(AppCompatibilities.BEREAL)

    dependsOn(
        removePairipProtectionPatch,
        removePermissionsAndCollectionFlagsPatch,
        disableCrashlyticsCollectionPatch,
        disableFirebaseSessionsPatch,
        dataCollectionExtensionPatch,
    )

    execute {
        setExtensionIsPatchIncluded("${BEREAL_EXTENSION_PACKAGE}DataCollection;")

        val start = ShakeStartCallFingerprint.matchSingle()
        start.method.removeInstruction(start.instructionMatches.single().index)

        val upload = AmplitudeUploadFingerprint.matchSingle()
        val factory = amplitudeResponseFactoryFingerprint(upload.method.returnType).matchSingle().method
        upload.method.addInstructions(
            0,
            """
                const/16 v0, 0xc8
                const/4 v1, 0x0
                invoke-static { v0, v1 }, ${factory.definingClass}->${factory.name}(ILjava/lang/String;)${factory.returnType}
                move-result-object v0
                return-object v0
            """,
        )

        val consentType = TrackingConsentEnumFingerprint.matchSingle().classDef.type
        trackingConsentSetterFingerprint(consentType).matchSingle().method.apply {
            val registers = getFreeRegisterProvider(0, 2)
            val name = registers.getFreeRegister4Bit()
            val type = registers.getFreeRegister4Bit()
            addInstructions(
                0,
                """
                    const-string v$name, "NOT_GRANTED"
                    const-class v$type, $consentType
                    invoke-static { v$type, v$name }, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;
                    move-result-object p1
                    check-cast p1, $consentType
                """,
            )
        }

        val datadogUpload = DatadogUploadRequestFingerprint.matchSingle()
        val successType = datadogUpload.instructionMatches.first()
            .getInstruction<ReferenceInstruction>().getReference<TypeReference>()!!.type
        val successConstructor = datadogUpload.instructionMatches.last()
            .getInstruction<ReferenceInstruction>().getReference<MethodReference>()!!
        datadogUpload.method.addInstructions(
            0,
            """
                new-instance v0, $successType
                const/4 v1, 0x0
                const/16 v2, 0xca
                const/4 v3, 0x0
                const/4 v4, 0x4
                invoke-direct { v0, v1, v2, v3, v4 }, $successConstructor
                return-object v0
            """,
        )

        AdjustInitFingerprint.matchSingle().method.returnEarly()
    }
}
