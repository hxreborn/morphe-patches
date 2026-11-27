/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.util.returnEarly
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patcher.util.numberOfParameterRegisters
import app.morphe.patches.shared.misc.smali.replaceMethod
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.util.StringUtils.escapeString

internal fun returnTrueFrom(extensionClass: String, methodName: String) =
    mutableClassDefBy(extensionClass).methods.single { it.name == methodName }.returnEarly(true)

internal fun fillPatchDetails(
    extensionClass: String,
    patch: Patch,
    nameMethod: String,
    descriptionMethod: String?,
) {
    val methods = mutableClassDefBy(extensionClass).methods
    methods.single { it.name == nameMethod }.returnEarly(escapeString(checkNotNull(patch.name)))
    if (descriptionMethod != null) {
        methods.single { it.name == descriptionMethod }.returnEarly(escapeString(checkNotNull(patch.description)))
    }
}

internal fun fillPatchMetadata(
    extensionClass: String,
    methodName: String,
    patch: Patch,
    withDescription: Boolean = true,
) = fillPatchDetails(
    extensionClass,
    patch,
    methodName + "Name",
    (methodName + "Description").takeIf { withDescription },
)

internal fun fillAppliedPatch(
    extensionClass: String,
    methodName: String,
    patch: Patch,
    withDescription: Boolean = true,
) {
    returnTrueFrom(extensionClass, methodName)
    fillPatchMetadata(extensionClass, methodName, patch, withDescription)
}

internal fun addAppliedPatchMetadata(
    extensionClass: String,
    key: String,
    patch: Patch,
    withDescription: Boolean = true,
) {
    val appliedPatches = mutableClassDefBy(extensionClass)
    val lookupMethod = "metadata"
    val lookup = appliedPatches.methods.single { it.name == lookupMethod }
    val keyIsRead = appliedPatches.methods.any { reader ->
        reader !== lookup && reader.implementation?.instructions?.any { instruction ->
            ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string == key
        } == true
    }
    if (!keyIsRead) throw PatchException("$extensionClass never reads applied patch key $key")
    val parameterRegisters = lookup.numberOfParameterRegisters
    val method = if (lookup.implementation!!.registerCount == parameterRegisters) {
        appliedPatches.replaceMethod(lookupMethod, parameterRegisters + 1, "const/4 v0, 0x0\nreturn-object v0")
        appliedPatches.methods.single { it.name == lookupMethod }
    } else {
        lookup
    }
    val name = escapeString(checkNotNull(patch.name))
    val description = if (withDescription) {
        "const-string/jumbo v0, \"${escapeString(checkNotNull(patch.description))}\""
    } else {
        "const/4 v0, 0x0"
    }
    method.addInstructionsWithLabels(
        0,
        """
            const-string/jumbo v0, "${escapeString(key)}"
            invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
            move-result v0
            if-eqz v0, :next
            if-nez p1, :description
            const-string/jumbo v0, "$name"
            return-object v0
            :description
            $description
            return-object v0
        """.trimIndent(),
        ExternalLabel("next", method.getInstruction(0)),
    )
}
