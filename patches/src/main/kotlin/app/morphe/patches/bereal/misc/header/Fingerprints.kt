package app.morphe.patches.bereal.misc.header

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral

private const val POSTING_LOGO_TO_PAINTER_WINDOW = 1

internal object PostingHeaderLogoFingerprint : Fingerprint(
    returnType = "V",
    isStatic = true,
    strings = listOf("postingscreen_options_button"),
    filters = listOf(
        resourceLiteral(ResourceType.DRAWABLE, "logo"),
        methodCall(
            definingClass = "Landroidx/compose/ui/res/PainterResources_androidKt;",
            parameters = listOf("I", "Landroidx/compose/runtime/Composer;", "I"),
            returnType = "Landroidx/compose/ui/graphics/painter/Painter;",
            location = MatchAfterWithin(POSTING_LOGO_TO_PAINTER_WINDOW),
        ),
    ),
)

internal object TopBarLogoFingerprint : Fingerprint(
    returnType = "V",
    isStatic = true,
    parameters = listOf("L", "Landroidx/compose/ui/Modifier;", "Landroidx/compose/runtime/Composer;", "I", "I"),
    filters = listOf(
        resourceLiteral(ResourceType.DRAWABLE, "logo_bereal_white"),
        methodCall(
            definingClass = "Landroidx/compose/ui/res/PainterResources_androidKt;",
            parameters = listOf("I", "Landroidx/compose/runtime/Composer;", "I"),
            returnType = "Landroidx/compose/ui/graphics/painter/Painter;",
            location = MatchAfterImmediately(),
        ),
    ),
)
