package app.morphe.patches.bereal.misc.header

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.morphe.patcher.resource.PublicXmlManager
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.markFeaturePatched
import app.morphe.patches.bereal.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR = "${BEREAL_EXTENSION_PACKAGE}header/BeFakeHeader;"
private const val POSTING_DRAWABLE = "hx_befake_header"
private const val TOP_BAR_DRAWABLE = "hx_befake_top_bar"

val customBeFakeHeaderPatch: Patch = patch(
    name = "Custom BeFake header",
    description = "Replaces the BeReal wordmark in the top bar and posting header with BeFake.",
    default = false,
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(settingsPatch)

    execute {
        val (postingId, topBarId) = PublicXmlManager(resourceFile("res/values/public.xml")).use { publicIds ->
            publicIds.createPublicId("drawable", POSTING_DRAWABLE)
            publicIds.createPublicId("drawable", TOP_BAR_DRAWABLE)
            val ids = publicIds.getDefinedIds()
            ids.getValue("drawable" to POSTING_DRAWABLE) to ids.getValue("drawable" to TOP_BAR_DRAWABLE)
        }
        copyResources("bereal/header", ResourceGroup("drawable", "$POSTING_DRAWABLE.xml", "$TOP_BAR_DRAWABLE.xml"))

        markFeaturePatched(EXTENSION_CLASS_DESCRIPTOR, customBeFakeHeaderPatch)
        val extension = mutableClassDefBy(EXTENSION_CLASS_DESCRIPTOR)
        extension.methods.single { it.name == "postingLogoId" }.returnEarly(postingId)
        extension.methods.single { it.name == "topBarLogoId" }.returnEarly(topBarId)

        PostingHeaderLogoFingerprint.insertLogoSelector("selectPostingLogo")
        TopBarLogoFingerprint.insertLogoSelector("selectTopBarLogo")
    }
}

private fun Fingerprint.insertLogoSelector(selectorMethod: String) {
    val match = match()
    val (drawableLiteral) = match.instructionMatches
    val register = match.method.getInstruction<OneRegisterInstruction>(drawableLiteral.index).registerA
    match.method.addInstructions(
        drawableLiteral.index + 1,
        """
            invoke-static/range {v$register .. v$register}, $EXTENSION_CLASS_DESCRIPTOR->$selectorMethod(I)I
            move-result v$register
        """,
    )
}
