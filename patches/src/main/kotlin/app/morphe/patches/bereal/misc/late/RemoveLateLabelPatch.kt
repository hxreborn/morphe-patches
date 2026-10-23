/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.late

import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.instructionMatch
import app.morphe.patches.bereal.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.setExtensionIsPatchIncluded
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS = "${BEREAL_EXTENSION_PACKAGE}latelabel/LateLabel;"

private class LateRead(val opcode: Opcode, val definingClass: String, val name: String, val filter: String)

private fun Match.lateFlagFieldName() =
    instructionMatch(Opcode.IGET_BOOLEAN)
        .getInstruction<ReferenceInstruction>().getReference<FieldReference>()!!.name

private fun BytecodePatchContext.lateReads(): List<LateRead> {
    val corePost = CorePostToStringFingerprint.matchSingle()
    val lateBy = corePost.classDef.fields.single { it.type == "J" && it.accessFlags and AccessFlags.STATIC.value == 0 }
    return listOf(
        LateRead(Opcode.IGET_WIDE, corePost.classDef.type, lateBy.name, "lateBy(J)J"),
        LateRead(Opcode.IGET_BOOLEAN, corePost.classDef.type, corePost.lateFlagFieldName(), "isLate(Z)Z"),
    ) + listOf(MomentPostToStringFingerprint, ChatPostToStringFingerprint).map {
        val match = it.matchSingle()
        LateRead(Opcode.IGET_BOOLEAN, match.classDef.type, match.lateFlagFieldName(), "isLate(Z)Z")
    }
}

private fun BytecodePatchContext.filterLateReads(methods: List<MutableMethod>, reads: List<LateRead>): Int {
    val targets = methods.flatMap { method ->
        reads.mapNotNull { read ->
            lateReadFingerprint(read.definingClass, read.name, read.opcode).matchOrNull(method)
                ?.let { Triple(it.method, it.instructionMatches.single(), read) }
        }
    }
    targets.sortedByDescending { (_, match, _) -> match.index }.forEach { (method, match, read) ->
        val register = match.getInstruction<OneRegisterInstruction>().registerA
        val wide = read.opcode == Opcode.IGET_WIDE
        val last = if (wide) register + 1 else register
        method.addInstructions(
            match.index + 1,
            """
                invoke-static/range { v$register .. v$last }, $EXTENSION_CLASS->${read.filter}
                move-result${if (wide) "-wide" else ""} v$register
            """,
        )
    }
    return targets.size
}

@Suppress("unused")
val removeLateLabelPatch = bytecodePatch(
    name = "Remove late label",
    description = "Removes the late label from friends' BeReals. Each one shows how long ago your friend posted it.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(settingsPatch)

    execute {
        setExtensionIsPatchIncluded(EXTENSION_CLASS)
        val reads = lateReads()

        val formatter = LateDurationFormatterFingerprint.matchSingle()
        val labelMethods = mutableClassDefBy(formatter.classDef.type).methods.toList() +
            LateDurationLiteralFingerprint.matchAll(1..5).map { it.method } +
            LateTagLiteralFingerprint.matchAll(1..10).map { it.method } +
            PostedLateLiteralFingerprint.matchAll(1..3).map { it.method }
        val patched = filterLateReads(
            labelMethods.distinctBy { "${it.definingClass}->${it.name}${it.parameterTypes}${it.returnType}" },
            reads,
        )
        if (patched == 0) throw PatchException("No lateBy or isLate read found in the late label methods")

        val placeholder = PostPlaceholderToStringFingerprint.matchSingle().classDef
        val getter = placeholderLateSecondsGetterFingerprint(placeholder.type).matchSingle()
        val read = getter.instructionMatches.first()
        val register = read.getInstruction<OneRegisterInstruction>().registerA
        getter.method.addInstructions(
            read.index + 1,
            """
                invoke-static/range { v$register .. v$register }, $EXTENSION_CLASS->lateSeconds(Ljava/lang/Integer;)Ljava/lang/Integer;
                move-result-object v$register
            """,
        )
    }
}
