/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.publicvideos.misc.thumbnails

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.addStaticMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.parameterRegisterCount
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.immutable.ImmutableField

private const val STRING_TYPE = "Ljava/lang/String;"
private const val DEFER_LOAD_METHOD = "hxDeferLoad"
private const val LOAD_DEFERRED_METHOD = "hxLoadDeferred"
private const val DEFER_LOAD_REGISTERS = 9
private const val LOAD_DEFERRED_REGISTERS = 8
private const val DEFERRED_URL_FIELD = "hxDeferredUrl"
private const val DEFERRED_SUBTITLE_FIELD = "hxDeferredSubtitle"
private const val REPLAYING_FIELD = "hxReplayingDeferredLoad"
private val DEFERRED_FLAG_FIELDS = listOf(
    "hxDeferredForceReload",
    "hxDeferredLoop",
    "hxDeferredAutoplay",
    "hxDeferredRetryWithoutSubtitle",
)

@Suppress("unused")
val loadVideosOnTapPatch = bytecodePatch(
    name = "Load videos on tap",
    description = "Loads feed videos only after a tap when Autoplay is set to Off in the app's settings. " +
        "YouTube videos are not affected.",
) {
    compatibleWith(AppCompatibilities.PUBLIC)

    execute {
        val load = PlayerLoadFingerprint.matchSingle()
        val loadImplementation = checkNotNull(load.method.implementation) { "${descriptor(load.method)} has no body" }
        if (loadImplementation.registerCount - load.method.parameterRegisterCount() < 1) {
            throw PatchException("${descriptor(load.method)} has no local register for the injected result")
        }
        val togglePlayback = PlayerTogglePlaybackFingerprint.matchSingle().method
        val autoplayAllowed = AutoplayAllowedFingerprint.matchSingle().method

        val player = mutableClassDefBy(load.classDef)
        val playerType = player.type
        listOf(DEFERRED_URL_FIELD to STRING_TYPE, DEFERRED_SUBTITLE_FIELD to STRING_TYPE, REPLAYING_FIELD to "Z")
            .plus(DEFERRED_FLAG_FIELDS.map { it to "Z" })
            .forEach { (name, type) ->
                player.fields.add(ImmutableField(playerType, name, type, AccessFlags.PUBLIC.value, null, null, null).toMutable())
            }
        val urlField = "$playerType->$DEFERRED_URL_FIELD:$STRING_TYPE"
        val subtitleField = "$playerType->$DEFERRED_SUBTITLE_FIELD:$STRING_TYPE"
        val replayingField = "$playerType->$REPLAYING_FIELD:Z"
        val (forceReloadField, loopField, autoplayField, retryWithoutSubtitleField) = DEFERRED_FLAG_FIELDS.map { "$playerType->$it:Z" }

        val deferLoad = player.addStaticMethod(
            DEFER_LOAD_METHOD,
            listOf(playerType, STRING_TYPE, STRING_TYPE, "Z", "Z", "Z", "Z"),
            "Z",
            DEFER_LOAD_REGISTERS,
            """
                iget-boolean v0, p0, $replayingField
                if-nez v0, :load
                if-eqz p1, :load
                invoke-static {}, ${descriptor(autoplayAllowed)}
                move-result v0
                if-nez v0, :load
                const-string v0, "youtube.com"
                invoke-virtual {p1, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                move-result v0
                if-nez v0, :load
                iput-object p1, p0, $urlField
                iput-object p2, p0, $subtitleField
                iput-boolean p3, p0, $forceReloadField
                iput-boolean p4, p0, $loopField
                iput-boolean p5, p0, $autoplayField
                iput-boolean p6, p0, $retryWithoutSubtitleField
                const/4 v0, 0x1
                return v0
                :load
                const/4 v0, 0x0
                iput-object v0, p0, $urlField
                return v0
            """,
        )

        val loadDeferred = player.addStaticMethod(
            LOAD_DEFERRED_METHOD,
            listOf(playerType),
            "V",
            LOAD_DEFERRED_REGISTERS,
            """
                iget-object v1, p0, $urlField
                if-eqz v1, :done
                iget-object v2, p0, $subtitleField
                iget-boolean v3, p0, $forceReloadField
                iget-boolean v4, p0, $loopField
                iget-boolean v5, p0, $autoplayField
                iget-boolean v6, p0, $retryWithoutSubtitleField
                const/4 v0, 0x0
                iput-object v0, p0, $urlField
                iput-object v0, p0, $subtitleField
                const/4 v0, 0x1
                iput-boolean v0, p0, $replayingField
                move-object v0, p0
                invoke-virtual/range {v0 .. v6}, ${descriptor(load.method)}
                const/4 v0, 0x0
                iput-boolean v0, p0, $replayingField
                :done
                return-void
            """,
        )

        load.method.addInstructionsWithLabels(
            0,
            """
                invoke-static/range {p0 .. p6}, $deferLoad
                move-result v0
                if-eqz v0, :load
                const/4 v0, 0x0
                return v0
            """,
            ExternalLabel("load", load.method.getInstruction(0)),
        )

        togglePlayback.addInstructions(0, "invoke-static {p0}, $loadDeferred")
    }
}
