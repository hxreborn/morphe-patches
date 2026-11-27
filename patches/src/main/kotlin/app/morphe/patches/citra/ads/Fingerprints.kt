package app.morphe.patches.citra.ads

import app.morphe.patcher.Fingerprint

private const val REMOTE_CONFIG_CLASS = "Lcom/theemulator/citra/data/RemoteConfig;"
internal const val FEATURES_CLASS = "Lcom/theemulator/citra/data/RemoteConfig\$Features;"

internal object AdNetworkFingerprint : Fingerprint(
    definingClass = REMOTE_CONFIG_CLASS,
    name = "getAdNetwork",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
)

internal object AdFeaturesFingerprint : Fingerprint(
    definingClass = REMOTE_CONFIG_CLASS,
    name = "getFeatures",
    returnType = FEATURES_CLASS,
    parameters = emptyList(),
)

internal object AdsInitializationFingerprint : Fingerprint(
    definingClass = "Lcom/theemulator/citra/CitraApplication;",
    name = "initAdsSdk",
    returnType = "V",
    parameters = emptyList(),
)

internal object AdConsentFingerprint : Fingerprint(
    definingClass = "Lcom/theemulator/citra/ConsentHelper;",
    name = "requestConsent",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "Lcom/theemulator/citra/ConsentHelper\$OnConsentResolved;"),
    isStatic = true,
)

internal object MainActivityResumeFingerprint : Fingerprint(
    definingClass = "Lcom/theemulator/citra/ui/main/MainActivity;",
    name = "onResume",
    returnType = "V",
    parameters = emptyList(),
    isStatic = false,
)
