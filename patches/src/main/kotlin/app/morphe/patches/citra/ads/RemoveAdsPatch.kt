package app.morphe.patches.citra.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.citraNoteExtensionPatch
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/citra/AzaharNote;"

@Suppress("unused")
val removeAdsPatch = patch(
    name = "Remove ads",
    description = "Removes ads during onboarding, browsing, and game launch. Shows a one-time note about Azahar on first launch.",
) {
    compatibleWith(AppCompatibilities.CITRA)
    dependsOn(removePairipProtectionPatch, citraNoteExtensionPatch)

    execute {
        MainActivityResumeFingerprint.method.apply {
            addInstructions(
                instructions.lastIndex,
                "invoke-static { p0 }, $EXTENSION_CLASS->showOnce(Landroid/app/Activity;)V",
            )
        }
        AdsInitializationFingerprint.method.returnEarly()
        AdConsentFingerprint.method.addInstructions(
            0,
            "const/4 v0, 0x0\n" +
                "sput-boolean v0, Lcom/theemulator/citra/ConsentHelper;->consentGiven:Z\n" +
                "invoke-interface { p1, v0 }, Lcom/theemulator/citra/ConsentHelper\$OnConsentResolved;->onResolved(Z)V\n" +
                "return-void",
        )
        AdNetworkFingerprint.method.returnEarly("none")
        AdFeaturesFingerprint.method.addInstructions(
            0,
            "new-instance p0, $FEATURES_CLASS\n" +
                "invoke-direct { p0 }, $FEATURES_CLASS-><init>()V\n" +
                "return-object p0",
        )
    }
}
