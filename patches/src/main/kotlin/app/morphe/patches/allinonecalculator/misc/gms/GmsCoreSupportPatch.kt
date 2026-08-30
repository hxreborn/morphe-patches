/*
 * SPDX-FileCopyrightText: 2026 Morphe
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from MorpheApp/morphe-patches:
 * https://github.com/MorpheApp/morphe-patches/commit/7ae360fd3ee25cf05c530329a179f82b1678c19a
 * Commit 7ae360fd3ee25cf05c530329a179f82b1678c19a (2026-08-10),
 * patches/src/main/kotlin/app/morphe/patches/shared/misc/gms/GmsCoreSupportPatch.kt
 */
package app.morphe.patches.allinonecalculator.misc.gms

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.allinonecalculator.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import org.w3c.dom.Element

private const val GMS_PACKAGE_NAME = "com.google.android.gms"

private const val GMS_CORE_PACKAGE_NAME = "app.revanced.android.gms"

private const val PLAY_SIGNATURE = "1b80a9d28abf3c06ade7a8063cc4afa11edcb199"

private const val REQUEST_PARAMS_CLASS =
    "Lio/flutter/plugins/googlesignin/GetCredentialRequestParams;"

private const val EXTENSION_CLASS =
    "Lapp/hxreborn/extension/allinonecalculator/GmsCoreSignIn;"

private fun ClassDef.allMethods() = directMethods + virtualMethods

private fun Instruction.isGmsPackageString() =
    (opcode == Opcode.CONST_STRING || opcode == Opcode.CONST_STRING_JUMBO) &&
            ((this as? ReferenceInstruction)?.reference as? StringReference)?.string == GMS_PACKAGE_NAME

private val spoofSignatureResourcePatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0)

            val metadata = document.createElement("meta-data") as Element
            metadata.setAttribute("android:name", "$GMS_CORE_PACKAGE_NAME.SPOOFED_PACKAGE_SIGNATURE")
            metadata.setAttribute("android:value", PLAY_SIGNATURE)
            application.appendChild(metadata)
        }
    }
}

@Suppress("unused")
val gmsCoreSupportPatch = bytecodePatch(
    name = "GmsCore support",
    description = "Signs in through GmsCore instead of Google Play Services. " +
        "Requires GmsCore to be installed.",
) {
    compatibleWith(AppCompatibilities.ALL_IN_ONE_CALCULATOR)
    extendWith("extensions/extension.mpe")

    dependsOn(removePairipProtectionPatch, spoofSignatureResourcePatch)

    execute {
        GetCredentialFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-virtual { p0 }, $DELEGATE_CLASS->getActivity()Landroid/app/Activity;
                move-result-object v0
                invoke-virtual { p1 }, $REQUEST_PARAMS_CLASS->getServerClientId()Ljava/lang/String;
                move-result-object v1
                invoke-static { v0, v1, p2 }, $EXTENSION_CLASS->getCredential(Landroid/app/Activity;Ljava/lang/String;Ljava/lang/Object;)V
                return-void
            """,
        )
    }

    finalize {
        val classTypes = buildList {
            classDefForEach { classDef ->
                val hasPackageString = classDef.allMethods().any { method ->
                    method.implementation?.instructions?.any(Instruction::isGmsPackageString) == true
                }
                if (hasPackageString) add(classDef.type)
            }
        }

        classTypes.forEach { classType ->
            val classDef = mutableClassDefBy(classType)

            (classDef.directMethods + classDef.virtualMethods).forEach { method ->
                val indices = method.implementation
                    ?.instructions
                    ?.withIndex()
                    ?.filter { (_, instruction) -> instruction.isGmsPackageString() }
                    ?.map { (index, _) -> index }
                    ?: return@forEach

                indices.asReversed().forEach { index ->
                    method.replaceInstruction(
                        index,
                        BuilderInstruction21c(
                            Opcode.CONST_STRING,
                            method.getInstruction<OneRegisterInstruction>(index).registerA,
                            ImmutableStringReference(GMS_CORE_PACKAGE_NAME),
                        ),
                    )
                }
            }
        }
    }
}
