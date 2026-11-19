package app.morphe.patches.flipboard.home

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.addFreeRegisters
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.AccessFlags.PUBLIC
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val disableAutomaticHomeFeedsPatch = bytecodePatch(
    name = "Disable automatic Home feeds",
    description = "Skips automatically added Home tabs.",
) {
    compatibleWith(AppCompatibilities.FLIPBOARD)

    execute {
        val wasRestored = persistExplicitHomeRestores()
        val converter = ConvertHomeFavoritesFingerprint.matchSingle()
        val matches = converter.instructionMatches
        val hasNext = matches.single { it.filter === homeIteratorHasNext }
        val feedSection = matches.single { it.filter === castHomeFeedSection }
        val repeat = matches.single { it.filter === repeatHomeConversion }
        val section = (feedSection.instruction as OneRegisterInstruction).registerA
        val dynamicFeed = classDefBy(FEED_SECTION).instanceFields.single {
            it.name == "dynamicFeed" && it.type == "Z" && PUBLIC.isSet(it.accessFlags)
        }
        val automatic = converter.method.implementation!!.registerCount
        check(section < 16 && automatic < 16) { "The Home conversion registers require a wider field read" }
        val (method, parameterCopies) = converter.method.addFreeRegisters(1)
        val loop = method.getInstruction<BuilderOffsetInstruction>(repeat.index + parameterCopies).target.location
        check(loop.index == hasNext.index + parameterCopies) { "The Home conversion loop target changed" }
        val conversion = feedSection.index + parameterCopies + 1
        method.addInstructionsWithLabels(
            conversion,
            """
                iget-boolean v$automatic, v$section, $dynamicFeed
                if-eqz v$automatic, :convert_feed
                invoke-static/range {v$section .. v$section}, $wasRestored
                move-result v$automatic
                if-eqz v$automatic, :next_feed
            """,
            ExternalLabel("convert_feed", method.getInstruction(conversion)),
            ExternalLabel("next_feed", method.getInstruction(loop.index)),
        )
    }
}
