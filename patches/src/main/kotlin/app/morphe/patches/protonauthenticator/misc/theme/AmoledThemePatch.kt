/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonauthenticator.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.protonauthenticator.misc.settings.patchesSettingsPatch
import app.morphe.patches.protonauthenticator.shared.themeColorReadInstructions
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.generated.descriptor
import app.morphe.patches.shared.misc.proton.AMOLED_BACKGROUND_OVERLAY_STYLE
import app.morphe.patches.shared.misc.proton.AMOLED_THEME_CLASS
import app.morphe.patches.shared.misc.proton.COMPOSER_TYPE
import app.morphe.patches.shared.misc.proton.amoledBackgroundOverlayPatch
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.patches.shared.misc.proton.transformCoreDarkBackgrounds
import app.morphe.util.findElementByAttributeValueOrThrow
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val THEME_PALETTE_COUNT = 2
private const val EMPTY_STATE_COLOR_FILTER_METHOD = "placeholderColorFilter"
private const val COLOR_FILTER_PARAMETER = 6
private const val COMPOSER_PARAMETER = 7
private const val AMOLED_PLACEHOLDER_CLASS = "Lapp/hxreborn/extension/protonauthenticator/AmoledPlaceholder;"
private const val COMPOSE_COLOR_FILTER_TYPE = "Landroidx/compose/ui/graphics/ColorFilter;"
private const val COLOR_FILTER_COMPANION_TYPE = "Landroidx/compose/ui/graphics/ColorFilter\$Companion;"
private const val SRC_IN_BLEND_MODE = 5
private const val COLOR_FILTER_REGISTER_COUNT = 5

private val amoledWindowBackgroundPatch = resourcePatch {
    finalize {
        document("res/values/styles.xml").use { document ->
            document.getElementsByTagName("style")
                .findElementByAttributeValueOrThrow("name", AMOLED_BACKGROUND_OVERLAY_STYLE)
                .appendChild(
                    document.createElement("item").apply {
                        setAttribute("name", "android:windowBackground")
                        textContent = "@android:color/black"
                    },
                )
        }
    }
}

private fun BytecodePatchContext.tintEmptyStatePlaceholder() {
    val image = EmptyStateImageFingerprint.matchSingle()
    val imageIndex = image.instructionMatches.last().index
    val imageCall = image.method.getInstruction<RegisterRangeInstruction>(imageIndex)

    val colorFilterMethod = ImmutableMethod(
        image.classDef.type,
        EMPTY_STATE_COLOR_FILTER_METHOD,
        listOf(ImmutableMethodParameter(COMPOSER_TYPE, null, null)),
        COMPOSE_COLOR_FILTER_TYPE,
        AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
        null,
        null,
        MutableMethodImplementation(COLOR_FILTER_REGISTER_COUNT),
    ).toMutable().apply {
        addInstructionsWithLabels(
            0,
            """
                ${themeColorReadInstructions(BACKGROUND_GRADIENT_TOP_FIELD)}
                invoke-static { v0, v1 }, $AMOLED_PLACEHOLDER_CLASS->tint(J)J
                move-result-wide v0
                const-wide/16 v2, 0x0
                cmp-long v2, v0, v2
                if-nez v2, :tint
                const/4 v0, 0x0
                return-object v0
                :tint
                sget-object v2, $COMPOSE_COLOR_FILTER_TYPE->Companion:$COLOR_FILTER_COMPANION_TYPE
                const/4 v3, $SRC_IN_BLEND_MODE
                invoke-virtual { v2, v0, v1, v3 }, $COLOR_FILTER_COMPANION_TYPE->tint-xETnrds(JI)$COMPOSE_COLOR_FILTER_TYPE
                move-result-object v0
                return-object v0
            """,
        )
    }
    image.classDef.methods.add(colorFilterMethod)

    val composer = imageCall.startRegister + COMPOSER_PARAMETER
    val defaultsMask = imageCall.startRegister + imageCall.registerCount - 1
    image.method.addInstructions(
        imageIndex,
        """
            invoke-static/range { v$composer .. v$composer }, ${descriptor(colorFilterMethod)}
            move-result-object v${imageCall.startRegister + COLOR_FILTER_PARAMETER}
            and-int/lit16 v$defaultsMask, v$defaultsMask, ${(1 shl COLOR_FILTER_PARAMETER).inv()}
        """,
    )
}

@Suppress("unused")
val amoledThemePatch = bytecodePatch(
    name = "AMOLED dark theme",
    description = "Replaces the dark theme background with pure black.",
) {
    compatibleWith(AppCompatibilities.PROTON_AUTHENTICATOR)
    dependsOn(patchesSettingsPatch, amoledBackgroundOverlayPatch, amoledWindowBackgroundPatch)
    extendWith("extensions/extension.mpe")

    execute {
        markFeaturePatched(AMOLED_THEME_CLASS)
        transformCoreDarkBackgrounds()
        tintEmptyStatePlaceholder()

        ThemeColorsInitializerFingerprint.matchAll(THEME_PALETTE_COUNT..THEME_PALETTE_COUNT).forEach { match ->
            match.instructionMatches.reversed().forEach { write ->
                val register = write.getInstruction<OneRegisterInstruction>().registerA
                match.method.addInstructions(
                    write.index,
                    """
                        invoke-static/range { v$register .. v${register + 1} }, $AMOLED_THEME_CLASS->transformPackedBackground(J)J
                        move-result-wide v$register
                    """,
                )
            }
        }
    }
}
