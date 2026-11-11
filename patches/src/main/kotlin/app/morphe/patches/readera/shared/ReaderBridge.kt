/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.shared

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patches.readera.misc.highlightcolors.READ_ACTIVITY_CLASS
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val CHAPTER_READER = "Lapp/hxreborn/extension/readera/ChapterReader;"

internal val chapterReaderPatch = bytecodePatch {
    execute {
        val (_, documentRead, positionRead, copyCall, speechCall) =
            ReadAloudFromCurrentPositionFingerprint.matchAll(1..1).single().instructionMatches
        val document = documentRead.instruction.getReference<FieldReference>()!!
        val visiblePosition = positionRead.instruction.getReference<FieldReference>()!!
        val copy = copyCall.instruction.getReference<MethodReference>()!!
        val readAloud = speechCall.instruction.getReference<MethodReference>()!!
        val positionType = copy.definingClass
        check(copy.parameterTypes == listOf(positionType) && visiblePosition.type == positionType &&
            readAloud.parameterTypes == listOf(positionType, "Z")) {
            "Reading position types disagree: ${descriptor(copy)}, ${descriptor(visiblePosition)}, ${descriptor(readAloud)}"
        }

        mutableClassDefBy(READ_ACTIVITY_CLASS).apply {
            interfaces.add(CHAPTER_READER)
            addMethod("currentReadingPosition", emptyList(), "Ljava/lang/Object;", AccessFlags.PUBLIC.value, 3, """
                iget-object v0, p0, ${descriptor(document)}
                if-eqz v0, :missing
                iget-object v0, v0, ${descriptor(visiblePosition)}
                if-eqz v0, :missing
                new-instance v1, $positionType
                invoke-direct {v1, v0}, ${descriptor(copy)}
                return-object v1
                :missing
                const/4 v0, 0x0
                return-object v0
            """)
            addMethod("copyReadingPosition", listOf("Ljava/lang/Object;"), "Ljava/lang/Object;", AccessFlags.PUBLIC.value, 3, """
                check-cast p1, $positionType
                new-instance v0, $positionType
                invoke-direct {v0, p1}, ${descriptor(copy)}
                return-object v0
            """)
            addMethod("readAloudFrom", listOf("Ljava/lang/Object;"), "V", AccessFlags.PUBLIC.value, 3, """
                check-cast p1, $positionType
                const/4 v0, 0x1
                invoke-virtual {p0, p1, v0}, ${descriptor(readAloud)}
                return-void
            """)
        }
    }
}
