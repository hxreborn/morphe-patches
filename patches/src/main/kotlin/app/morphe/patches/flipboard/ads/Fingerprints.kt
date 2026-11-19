package app.morphe.patches.flipboard.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchFirst
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal const val FEED_ITEM = "Lflipboard/model/FeedItem;"
internal const val SERVER_ITEM = "Lflipboard/model/ItemFromServer;"
internal const val ITEM_LIST = "Ljava/util/List;"
private const val AMAZON_AD_REGISTRATION = "Lcom/amazon/device/ads/AdRegistration;"
private const val AMAZON_AD_REQUEST = "Lcom/amazon/device/ads/DTBAdRequest;"
private const val AMAZON_AD_CALLBACK = "Lcom/amazon/device/ads/DTBAdCallback;"

internal object FeedAdPreferenceFingerprint : Fingerprint(
    isStatic = false,
    returnType = "V",
    filters = listOf(
        string("pref_key_enable_ads"),
        methodCall(
            definingClass = "Landroid/content/SharedPreferences;",
            name = "getBoolean",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        fieldAccess(type = "Z", opcode = Opcode.IPUT_BOOLEAN, location = MatchAfterImmediately()),
        fieldAccess(type = "Z", opcode = Opcode.IGET_BOOLEAN),
    ),
)

internal object LoadSectionAdsFingerprint : Fingerprint(
    isStatic = false,
    parameters = emptyList(),
    returnType = "V",
    strings = listOf(
        "pref_key_ad_prep_override_ngl",
        "Requesting an Ad from Flint",
        "Requesting an Ad from DFP",
    ),
)

internal object CheckDetailBannerPacingFingerprint : Fingerprint(
    definingClass = "Lflipboard/activities/DetailActivity;",
    isStatic = false,
    returnType = "V",
    filters = listOf(
        methodCall(definingClass = "Lflipboard/model/ConfigSetting;", name = "getDetailAdPacing", returnType = "I"),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        opcode(Opcode.IF_GTZ, MatchAfterImmediately()),
        string("[shouldLoadDetailBannerAd] disabled, DetailAdPacing: "),
    ),
)

internal val registerAmazonAds = methodCall(
    definingClass = AMAZON_AD_REGISTRATION,
    name = "getInstance",
    parameters = listOf("Ljava/lang/String;", "Landroid/content/Context;"),
    returnType = AMAZON_AD_REGISTRATION,
)
internal val setAmazonMraidVersions = methodCall(
    definingClass = AMAZON_AD_REGISTRATION,
    name = "setMRAIDSupportedVersions",
    parameters = listOf("[Ljava/lang/String;"),
    returnType = "V",
)
internal val setAmazonMraidPolicy = methodCall(
    definingClass = AMAZON_AD_REGISTRATION,
    name = "setMRAIDPolicy",
    parameters = listOf("Lcom/amazon/device/ads/MRAIDPolicy;"),
    returnType = "V",
    location = MatchAfterImmediately(),
)

internal object RegisterAmazonAdsFingerprint : Fingerprint(
    name = "<init>",
    isStatic = false,
    returnType = "V",
    filters = listOf(
        registerAmazonAds,
        string("1.0", location = MatchAfterImmediately()),
        setAmazonMraidVersions,
        fieldAccess(
            definingClass = "Lcom/amazon/device/ads/MRAIDPolicy;",
            name = "CUSTOM",
            opcode = Opcode.SGET_OBJECT,
            location = MatchAfterImmediately(),
        ),
        setAmazonMraidPolicy,
        opcode(Opcode.RETURN_VOID, MatchAfterImmediately()),
    ),
)

internal object LoadAmazonLayoutBidFingerprint : Fingerprint(
    isStatic = false,
    returnType = "V",
    filters = listOf(
        string("[Amazon TAM Request] Content URL: "),
        methodCall(name = "<init>", parameters = listOf("Ljava/lang/Object;", "B"), opcode = Opcode.INVOKE_DIRECT),
        methodCall(
            definingClass = AMAZON_AD_REQUEST,
            name = "loadAd",
            parameters = listOf(AMAZON_AD_CALLBACK),
            opcode = Opcode.INVOKE_VIRTUAL,
            location = MatchAfterImmediately(),
        ),
    ),
)

internal object InitializeGoogleAdsFingerprint : Fingerprint(
    definingClass = "Lcom/flipboard/startup/AppStartupInitializer;",
    name = "create",
    parameters = listOf("Landroid/content/Context;"),
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        string("pref_key_gam_test_device_id"),
        methodCall(
            definingClass = "Lcom/google/android/gms/ads/MobileAds;",
            name = "initialize",
            parameters = listOf(
                "Landroid/content/Context;",
                "Lcom/google/android/gms/ads/initialization/OnInitializationCompleteListener;",
            ),
            returnType = "V",
        ),
    ),
)

internal object CompleteAmazonLayoutBidFailureFingerprint : Fingerprint(
    name = "onFailure",
    isStatic = false,
    parameters = listOf("Lcom/amazon/device/ads/AdError;"),
    returnType = "V",
    filters = listOf(
        string("Amazon TAM request failed, error: "),
        opcode(Opcode.SGET_OBJECT),
        methodCall(
            parameters = listOf("Ljava/lang/Object;"),
            returnType = "V",
            opcode = Opcode.INVOKE_VIRTUAL,
            location = MatchAfterImmediately(),
        ),
        methodCall(
            parameters = emptyList(),
            returnType = "V",
            opcode = Opcode.INVOKE_VIRTUAL,
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.RETURN_VOID, MatchAfterImmediately()),
    ),
)

internal object ReplaceSectionItemsFingerprint : Fingerprint(
    isStatic = false,
    parameters = listOf(ITEM_LIST),
    returnType = "V",
    filters = listOf(
        methodCall(definingClass = FEED_ITEM, name = "isSectionCover"),
        methodCall(definingClass = FEED_ITEM, name = "getPrimaryItem"),
        methodCall(definingClass = SERVER_ITEM, name = "getPinned"),
    ),
)

internal object BindRoundupAdapterFingerprint : Fingerprint(
    isStatic = false,
    returnType = "V",
    filters = listOf(
        methodCall(definingClass = SERVER_ITEM, name = "getSimilarItems", returnType = ITEM_LIST),
        methodCall(
            name = "<init>",
            parameters = listOf("Landroid/content/Context;", "L", ITEM_LIST),
            opcode = Opcode.INVOKE_DIRECT,
        ),
        methodCall(definingClass = "Landroidx/recyclerview/widget/RecyclerView;", name = "setAdapter"),
    ),
)

internal fun roundupAdapterCountFingerprint(adapter: String) = Fingerprint(
    definingClass = adapter,
    isStatic = false,
    parameters = emptyList(),
    returnType = "I",
    filters = listOf(
        fieldAccess(adapter, type = ITEM_LIST, opcode = Opcode.IGET_OBJECT, location = MatchFirst()),
        methodCall(definingClass = ITEM_LIST, name = "size", returnType = "I", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        opcode(Opcode.ADD_INT_LIT8, MatchAfterImmediately()),
        opcode(Opcode.RETURN, MatchAfterImmediately()),
    ),
)

internal val nestedFeedItemFingerprints = mapOf(
    "getItems" to "items",
    "getInlineItems" to "inlineItems",
    "getSimilarItems" to "similarItems",
).map { (getter, field) ->
    Fingerprint(
        definingClass = SERVER_ITEM,
        name = getter,
        isStatic = false,
        parameters = emptyList(),
        returnType = ITEM_LIST,
        filters = listOf(
            fieldAccess(SERVER_ITEM, field, ITEM_LIST, Opcode.IGET_OBJECT, location = MatchFirst()),
            opcode(Opcode.RETURN_OBJECT, MatchAfterImmediately()),
        ),
    )
}
