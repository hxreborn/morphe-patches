/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.notes

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.readera.shared.DRAWER_QUOTES_ITEM
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.showDrawerItem
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import org.w3c.dom.Element

private const val NOTES_ACTIVITY = "app.hxreborn.extension.readera.NotesActivity"
private const val NOTES_ACTIVITY_TYPE = "Lapp/hxreborn/extension/readera/NotesActivity;"
private const val CURSOR_TYPE = "Landroid/database/Cursor;"
private const val QUOTE_TYPE = 3
private const val POSITION_BRIDGE_REGISTERS = 6

private class NotePosition(val model: String, val position: String)

private val notesActivityManifestPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element
            val activity = document.createElement("activity")
            activity.setAttribute("android:name", NOTES_ACTIVITY)
            activity.setAttribute("android:exported", "false")
            application.appendChild(activity)
        }
    }
}

@Suppress("unused")
val quotesAndNotesPatch = bytecodePatch(
    name = "Quotes and notes section",
    description = "Adds a section to the main menu with the quotes, bookmarks and reviews from all books.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(disableSignatureChecksPatch, notesActivityManifestPatch)

    extendWith("extensions/extension.mpe")

    execute {
        showDrawerItem(DRAWER_QUOTES_ITEM)

        DrawerNotesClickFingerprint.let {
            val openIndex = it.instructionMatches[0].index
            val activityRegister = it.method.getInstruction<FiveRegisterInstruction>(openIndex).registerC
            it.method.replaceInstruction(
                openIndex,
                "invoke-static { v$activityRegister }, $NOTES_ACTIVITY_TYPE->open(Landroid/app/Activity;)V",
            )
        }

        val quote = notePosition(QuoteJumpFingerprint)
        val bookmark = notePosition(BookmarkJumpFingerprint)
        val postPosition = QuoteJumpFingerprint.instructionMatches[1].instruction.getReference<MethodReference>()!!

        val notesActivity = mutableClassDefBy(NOTES_ACTIVITY_TYPE)
        val stub = notesActivity.methods.single { it.name == "postNotePosition" }
        notesActivity.methods.remove(stub)
        notesActivity.methods.add(
            ImmutableMethod(
                stub.definingClass,
                stub.name,
                stub.parameters,
                stub.returnType,
                stub.accessFlags,
                null,
                null,
                MutableMethodImplementation(POSITION_BRIDGE_REGISTERS),
            ).toMutable().apply {
                addInstructionsWithLabels(
                    0,
                    """
                        const/4 v0, $QUOTE_TYPE
                        if-ne p1, v0, :bookmark
                        new-instance v0, ${quote.model}
                        invoke-direct { v0, p0 }, ${quote.model}-><init>($CURSOR_TYPE)V
                        new-instance v1, ${quote.position}
                        invoke-direct { v1, v0 }, ${quote.position}-><init>(${quote.model})V
                        goto :post
                        :bookmark
                        new-instance v0, ${bookmark.model}
                        invoke-direct { v0, p0 }, ${bookmark.model}-><init>($CURSOR_TYPE)V
                        new-instance v1, ${bookmark.position}
                        invoke-direct { v1, v0 }, ${bookmark.position}-><init>(${bookmark.model})V
                        :post
                        invoke-static { v1, p2, p3 }, $postPosition
                        return-void
                    """,
                )
            },
        )
    }
}

private fun BytecodePatchContext.notePosition(jump: Fingerprint): NotePosition {
    val position = jump.instructionMatches[0].instruction.getReference<TypeReference>()!!.type
    val model = classDefBy(position).methods.single { method ->
        method.name == "<init>" && method.parameterTypes.size == 1 &&
            classDefByOrNull(method.parameterTypes[0].toString())?.methods?.any { modelMethod ->
                modelMethod.name == "<init>" && modelMethod.parameterTypes.map { it.toString() } == listOf(CURSOR_TYPE)
            } == true
    }.parameterTypes[0].toString()
    return NotePosition(model, position)
}
