/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.notes

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.getReference
import app.morphe.patches.readera.shared.DRAWER_QUOTES_ITEM
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.showDrawerItem
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.replaceMethod
import app.morphe.util.asSequence
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import org.w3c.dom.Element

private const val NOTES_ACTIVITY = "app.hxreborn.extension.readera.NotesActivity"
private const val NOTES_ACTIVITY_TYPE = "Lapp/hxreborn/extension/readera/NotesActivity;"
private const val CURSOR_TYPE = "Landroid/database/Cursor;"
private const val QUOTE_TYPE = 3
private const val POSITION_BRIDGE_REGISTERS = 6

private val ENGLISH_TEXT_BY_NOTES_STRING = mapOf(
    "hx_notes_title" to "Quotes, notes ...",
    "hx_notes_search" to "Search quotes and notes",
    "hx_notes_back" to "Back",
    "hx_notes_all" to "All",
    "hx_notes_in_books" to "In books",
    "hx_notes_in_collections" to "In collections",
    "hx_notes_in_authors" to "In authors",
    "hx_notes_empty_all" to "Quotes, notes, bookmarks and reviews from all books and documents will be here",
    "hx_notes_empty_books" to "Quotes, notes, bookmarks and reviews, grouped by books, will be here",
    "hx_notes_empty_collections" to "Quotes, notes, bookmarks and reviews, grouped by collections, will be here",
    "hx_notes_empty_authors" to "Quotes, notes, bookmarks and reviews, grouped by authors, will be here",
    "hx_notes_quotes" to "Quotes",
    "hx_notes_bookmarks" to "Bookmarks",
    "hx_notes_reviews" to "Reviews",
    "hx_notes_bookmark" to "Bookmark",
    "hx_notes_review" to "Review",
    "hx_notes_page" to "page %d",
)

private class NotePosition(val model: String, val position: String)

private val notesStringsPatch = resourcePatch {
    execute {
        val sourceNames = document("res/values/strings.xml").use { document ->
            val namesByText = document.getElementsByTagName("string").asSequence()
                .map { it as Element }
                .groupBy({ it.textContent }, { it.getAttribute("name") })
            ENGLISH_TEXT_BY_NOTES_STRING.mapValues { (name, text) ->
                namesByText[text]?.first() ?: throw PatchException("No string \"$text\" in res/values/strings.xml for $name")
            }
        }
        decode("res/values*/strings.xml")
            .forEach { path ->
                document(path).use { document ->
                    val strings = document.getElementsByTagName("string").asSequence()
                        .map { it as Element }
                        .associateBy { it.getAttribute("name") }
                    sourceNames.forEach { (name, sourceName) ->
                        strings[sourceName]?.let { source ->
                            val copy = source.cloneNode(true) as Element
                            copy.setAttribute("name", name)
                            document.documentElement.appendChild(copy)
                        }
                    }
                }
            }
    }
}

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

    dependsOn(disableSignatureChecksPatch, notesActivityManifestPatch, notesStringsPatch)

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

        mutableClassDefBy(NOTES_ACTIVITY_TYPE).replaceMethod(
            "postNotePosition",
            POSITION_BRIDGE_REGISTERS,
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
