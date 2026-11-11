/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.selection

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.getReference
import app.morphe.patches.readera.shared.ClipboardCopyFingerprint
import app.morphe.patches.readera.shared.SELECTION_COPY_TOAST_CALL_MATCH
import app.morphe.patches.readera.shared.SELECTION_COPY_TOAST_MESSAGE_MATCH
import app.morphe.patches.readera.shared.addSelectionAction
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.patchesSettingsPatch
import app.morphe.patches.readera.shared.registerReadEraPatch
import app.morphe.patches.readera.shared.selectionCopyFingerprint
import app.morphe.patches.readera.shared.selectionToolbarPatch
import app.morphe.patches.readera.shared.stringResourceId
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.addStaticMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val SELECTION_SOURCE = "Lapp/hxreborn/extension/readera/SelectionSource;"
private const val QUOTE_SHARE_SUBJECT = "Quote from the book %1\$s"

private var quoteShareSubject = -1L
private val quoteShareSubjectResourcePatch = resourcePatch {
    execute {
        quoteShareSubject = stringResourceId(QUOTE_SHARE_SUBJECT)
    }
}

@Suppress("unused")
val copyWithSourcePatch = bytecodePatch(
    name = "Copy with source",
    description = "Copies selected text with the book title and author. Found under More in the text selection menu.",
) {
    compatibleWith(AppCompatibilities.READERA)
    availability(requireArm64)
    dependsOn(patchesSettingsPatch, disableSignatureChecksPatch, selectionToolbarPatch, quoteShareSubjectResourcePatch)
    extendWith("extensions/extension.mpe")

    execute {
        registerReadEraPatch("Copy with source", "Copies selected text with the book title and author.")
        val clipboard = ClipboardCopyFingerprint.matchAll(1..1).single().originalMethod
        val copyMatches = selectionCopyFingerprint(clipboard).matchAll(1..1).single().instructionMatches
        val toastMessage = copyMatches[SELECTION_COPY_TOAST_MESSAGE_MATCH]
        val toastCall = copyMatches[SELECTION_COPY_TOAST_CALL_MATCH]
        val toast = toastCall.instruction.getReference<MethodReference>()!!
        val copiedMessage = toastMessage.getInstruction<WideLiteralInstruction>().wideLiteral
        check(toastMessage.getInstruction<OneRegisterInstruction>().registerA == toastCall.getInstruction<FiveRegisterInstruction>().registerD) {
            "Copy toast does not show the message constant: ${descriptor(toast)}"
        }

        val share = quoteShareFingerprint(quoteShareSubject).matchAll(1..1).single()
        val (documentCall, _, sourceCall) = share.instructionMatches
        val document = documentCall.instruction.getReference<MethodReference>()!!
        val source = sourceCall.instruction.getReference<MethodReference>()!!
        check(source.parameterTypes == listOf(document.returnType) && source.definingClass == share.originalMethod.definingClass) {
            "Quote source types disagree: ${descriptor(document)}, ${descriptor(source)}"
        }
        val documentSource = mutableClassDefBy(source.definingClass).addStaticMethod(
            "documentSource",
            listOf(document.definingClass),
            "Ljava/lang/String;",
            1,
            """
                invoke-interface {p0}, ${descriptor(document)}
                move-result-object p0
                if-eqz p0, :missing
                invoke-static {p0}, ${descriptor(source)}
                move-result-object p0
                return-object p0
                :missing
                const-string p0, ""
                return-object p0
            """,
        )

        mutableClassDefBy(SELECTION_SOURCE).apply {
            replaceStub("documentSource", 1, """
                check-cast p0, ${document.definingClass}
                invoke-static {p0}, $documentSource
                move-result-object p0
                return-object p0
            """)
            replaceStub("copyText", 4, """
                invoke-static {p0, p1, p2}, ${descriptor(clipboard)}
                const v0, $copiedMessage
                invoke-static {p0, v0}, ${descriptor(toast)}
                return-void
            """)
        }
        addSelectionAction(SELECTION_SOURCE)
    }
}
