/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.movieboxtv.misc.seek

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.addStaticMethod
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val PREFERENCES = "hx_movieboxtv_patches"
private const val SKIP_LENGTH_SECONDS_KEY = "skip_length_seconds"
private const val SKIP_OPTION_PREFIX = "hx_skip_length:"

private val skipLengths = listOf(
    "Skip 10s" to 10,
    "Skip 30s" to 30,
    "Skip 1m" to 60,
    "Skip 5m" to 300,
    "Skip 10m" to 600,
)

private const val CONTEXT = "Landroid/content/Context;"
private const val PREFERENCES_TYPE = "Landroid/content/SharedPreferences;"
private const val EDITOR = "Landroid/content/SharedPreferences\$Editor;"
private const val FRAGMENT = "Landroidx/fragment/app/Fragment;"

private val openPreferences = """
    const-string v1, "$PREFERENCES"
    const/4 v2, 0x0
    invoke-virtual { v0, v1, v2 }, $CONTEXT->getSharedPreferences(${STRING}I)$PREFERENCES_TYPE
    move-result-object v0
""".trimIndent()

private val readSkipSeconds = """
    $openPreferences
    const-string v1, "$SKIP_LENGTH_SECONDS_KEY"
    const/16 v2, $STOCK_SKIP_LENGTH_SECONDS
    invoke-interface { v0, v1, v2 }, $PREFERENCES_TYPE->getInt(${STRING}I)I
    move-result v0
""".trimIndent()

private fun skipChip(option: String, index: Int, text: String, seconds: Int) = """
    const-string v3, "$text"
    const-string v4, "$SKIP_OPTION_PREFIX$seconds"
    const/4 v5, 0x0
    const v6, $seconds
    if-ne v1, v6, :unselected_$index
    const/4 v5, 0x1
    :unselected_$index
    const/4 v6, -0x1
    const/4 v7, 0x0
    const/4 v8, 0x0
    new-instance v2, $option
    invoke-direct/range { v2 .. v8 }, $option-><init>(${OPTION_CONSTRUCTOR_PARAMETERS.joinToString("")})V
    invoke-virtual { v9, v2 }, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
""".trimIndent()

@Suppress("unused")
val customSkipLengthPatch = patch(
    name = "Custom skip length",
    description = "Adds skip lengths from 10 seconds to 10 minutes to the player's speed settings.",
) {
    compatibleWith(AppCompatibilities.MOVIEBOX_TV)

    execute {
        val (playerViewWrite) = PlayerViewFieldFingerprint.instructionMatches
        val skipLengthMillis = PlayerViewFieldFingerprint.classDef.addStaticMethod(
            "skipLengthMillis",
            listOf(PLAYER_CONTROLLER),
            "J",
            5,
            """
                iget-object v0, p0, ${descriptor(playerViewWrite.fieldReference)}
                invoke-virtual { v0 }, Landroid/view/View;->getContext()$CONTEXT
                move-result-object v0
                $readSkipSeconds
                int-to-long v0, v0
                const-wide/16 v2, $MILLIS_PER_SECOND
                mul-long/2addr v0, v2
                return-wide v0
            """,
        )
        listOf(DpadSkipFingerprint, MediaKeySkipFingerprint).forEach { fingerprint ->
            val (stockSkip) = fingerprint.instructionMatches
            val register = stockSkip.getInstruction<OneRegisterInstruction>().registerA
            fingerprint.method.replaceInstruction(stockSkip.index, "invoke-static/range { p0 .. p0 }, $skipLengthMillis")
            fingerprint.method.addInstruction(stockSkip.index + 1, "move-result-wide v$register")
        }

        val selected = SettingSelectedFingerprint.method
        val option = selected.parameterTypes.last().toString()
        val (labelWrite, valueWrite) = optionFieldsFingerprint(option).instructionMatches
        val label = descriptor(labelWrite.fieldReference)
        val value = descriptor(valueWrite.fieldReference)
        val fragment = SettingSelectedFingerprint.classDef
        val refreshSpeedOptions = refreshSpeedOptionsFingerprint(SpeedOptionsFingerprint.method.name).method

        val withSkipLengths = fragment.addStaticMethod(
            "withSkipLengths",
            listOf(FRAGMENT, LIST),
            LIST,
            12,
            """
                new-instance v9, Ljava/util/ArrayList;
                invoke-direct { v9, p1 }, Ljava/util/ArrayList;-><init>(Ljava/util/Collection;)V
                invoke-virtual { p0 }, $FRAGMENT->getContext()$CONTEXT
                move-result-object v0
                if-eqz v0, :done
                $readSkipSeconds
                move v1, v0
                ${skipLengths.withIndex().joinToString("\n") { (index, length) -> skipChip(option, index, length.first, length.second) }}
                :done
                return-object v9
            """,
        )
        val (_, speedOptionsReturn) = SpeedOptionsFingerprint.instructionMatches
        val list = speedOptionsReturn.getInstruction<OneRegisterInstruction>().registerA
        val fragmentRegister = if (list == 0) 1 else 0
        SpeedOptionsFingerprint.method.replaceInstruction(speedOptionsReturn.index, "move-object/from16 v$fragmentRegister, p0")
        SpeedOptionsFingerprint.method.addInstructions(
            speedOptionsReturn.index + 1,
            """
                invoke-static { v$fragmentRegister, v$list }, $withSkipLengths
                move-result-object v$list
                return-object v$list
            """,
        )

        val selectSkipLength = fragment.addStaticMethod(
            "selectSkipLength",
            listOf(FRAGMENT, option),
            "Z",
            9,
            """
                iget-object v3, p1, $value
                const-string v4, "$SKIP_OPTION_PREFIX"
                invoke-virtual { v3, v4 }, $STRING->startsWith($STRING)Z
                move-result v5
                if-eqz v5, :not_skip_length
                invoke-virtual { v4 }, $STRING->length()I
                move-result v5
                invoke-virtual { v3, v5 }, $STRING->substring(I)$STRING
                move-result-object v3
                invoke-static { v3 }, Ljava/lang/Integer;->parseInt($STRING)I
                move-result v3
                invoke-virtual { p0 }, $FRAGMENT->requireContext()$CONTEXT
                move-result-object v0
                move-object v6, v0
                $openPreferences
                invoke-interface { v0 }, $PREFERENCES_TYPE->edit()$EDITOR
                move-result-object v0
                const-string v1, "$SKIP_LENGTH_SECONDS_KEY"
                invoke-interface { v0, v1, v3 }, $EDITOR->putInt(${STRING}I)$EDITOR
                move-result-object v0
                invoke-interface { v0 }, $EDITOR->apply()V
                iget-object v1, p1, $label
                const/4 v2, 0x0
                invoke-static { v6, v1, v2 }, Landroid/widget/Toast;->makeText(${CONTEXT}Ljava/lang/CharSequence;I)Landroid/widget/Toast;
                move-result-object v1
                invoke-virtual { v1 }, Landroid/widget/Toast;->show()V
                const/4 v0, 0x1
                return v0
                :not_skip_length
                const/4 v0, 0x0
                return v0
            """,
        )
        selected.addInstructionsWithLabels(
            0,
            """
                move-object/from16 v0, p0
                move-object/from16 v1, p2
                invoke-static { v0, v1 }, $selectSkipLength
                move-result v1
                if-eqz v1, :stock
                invoke-virtual { v0 }, ${descriptor(refreshSpeedOptions)}
                return-void
            """,
            ExternalLabel("stock", selected.getInstruction(0)),
        )
    }
}
