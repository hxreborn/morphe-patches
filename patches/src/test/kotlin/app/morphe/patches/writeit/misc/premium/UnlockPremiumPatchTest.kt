/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.writeit.misc.premium

import app.morphe.patcher.patch.PatchException
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class UnlockPremiumPatchTest {

    private val entryStoreJavascript =
        "saveNote:(entry,note)=>client.saveUserEntryNote.mutate({entryId:entry,note:note})," +
            "saveStatus:(entry,status)=>client.saveUserEntry.mutate({entryId:entry,status:status})," +
            "userEntryFor:item=>entries.byEntryId[item]"

    @Test
    fun replacesRemoteEntryUpdatesWithLocalUpdates() {
        val patched = enableLocalEntryUpdates(entryStoreJavascript, "entries.js")

        assertContains(
            patched,
            "saveNote:(entry,note)=>Promise.resolve(" +
                "entries.loaded[entries.byEntryId[entry].id]={...entries.byEntryId[entry],note:note})",
        )
        assertContains(
            patched,
            "saveStatus:(entry,status)=>Promise.resolve(" +
                "entries.loaded[entries.byEntryId[entry].id]={...entries.byEntryId[entry],status:status})",
        )
    }

    @Test
    fun rejectsDuplicateNoteMutation() {
        assertFailsWith<PatchException> {
            enableLocalEntryUpdates(
                "$entryStoreJavascript,saveNote:(entry,note)=>client.saveUserEntryNote.mutate(" +
                    "{entryId:entry,note:note})",
                "entries.js",
            )
        }
    }

    @Test
    fun rejectsMissingStore() {
        assertFailsWith<PatchException> {
            enableLocalEntryUpdates(entryStoreJavascript.substringBefore(",userEntryFor"), "entries.js")
        }
    }

    @Test
    fun rejectsStoreNamedLikeAHandlerParameter() {
        assertFailsWith<PatchException> {
            enableLocalEntryUpdates(entryStoreJavascript.replace("entries.byEntryId", "note.byEntryId"), "entries.js")
        }
        assertFailsWith<PatchException> {
            enableLocalEntryUpdates(entryStoreJavascript.replace("entries.byEntryId", "entry.byEntryId"), "entries.js")
        }
    }

    @Test
    fun setsLiveUpdateStrategyToNone() {
        val patched = disableLiveUpdates("""{"autoUpdateStrategy":"background"}""", "capacitor.config.json")

        assertContains(patched, """"autoUpdateStrategy":"none"""")
    }

    @Test
    fun rejectsDuplicateLiveUpdateStrategies() {
        assertFailsWith<PatchException> {
            disableLiveUpdates(
                """{"autoUpdateStrategy":"background","autoUpdateStrategy":"background"}""",
                "capacitor.config.json",
            )
        }
    }
}
