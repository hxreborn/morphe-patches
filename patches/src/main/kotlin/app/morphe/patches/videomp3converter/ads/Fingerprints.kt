package app.morphe.patches.videomp3converter.ads

import app.morphe.patcher.Fingerprint

internal object AdInitializationFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("I"),
    strings = listOf("ad.block", "ad_priority", "ad.native_size"),
)
