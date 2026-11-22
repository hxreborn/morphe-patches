package app.morphe.patches.arduinodroid.misc.premium

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks premium features, removes ads and removes flashing limits.",
) {
    compatibleWith(AppCompatibilities.ARDUINODROID)
    dependsOn(removePairipProtectionPatch)

    execute {
        val premiumStatus = PremiumStatusFingerprint.method
        subscriptionStatusFingerprint(premiumStatus).method.returnEarly(false)
        premiumStatus.returnEarly(true)
        AdsInitializationFingerprint.method.returnEarly()
    }
}
