package app.morphe.patches.arduinodroid.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object PremiumManagerFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(
            definingClass = "Lname/antonsmirnov/android/arduinodroid2/R\$string;",
            name = "failedtoGetFeatureDetails",
        ),
    ),
)

internal object PremiumStatusFingerprint : Fingerprint(
    classFingerprint = PremiumManagerFingerprint,
    returnType = "Z",
    filters = listOf(methodCall(definingClass = "Ljava/util/List;", name = "iterator")),
)

internal object AdsInitializationFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(
            definingClass = "Lname/antonsmirnov/android/arduinodroid2/R\$string;",
            name = "ADMOB_APP_ID",
        ),
    ),
)

internal fun subscriptionStatusFingerprint(premiumStatus: MethodReference): Fingerprint = Fingerprint(
    classFingerprint = PremiumManagerFingerprint,
    filters = listOf(methodCall(premiumStatus)),
)
