package app.morphe.patches.flipboard.home

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.checkCast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val FAVORITES = "Lflipboard/io/FavoritesWithVersion;"
internal const val FEED_SECTION = "Lflipboard/model/FeedSection;"
internal const val HOME_CALLBACK = "Lflipboard/io/b;"
internal const val FAVORITES_RESPONSE = "Lflipboard/model/flapresponse/FavoritesResponse;"

internal val homeFeedKey = fieldAccess(type = "Ljava/lang/String;", opcode = Opcode.IGET_OBJECT)
internal val homeFavoritesCallback = fieldAccess(
    definingClass = HOME_CALLBACK, type = HOME_CALLBACK, opcode = Opcode.SGET_OBJECT,
)
internal val restoreHomeRequest = methodCall(
    parameters = listOf("Ljava/lang/Iterable;"), opcode = Opcode.INVOKE_INTERFACE,
    location = MatchAfterImmediately(),
)

private fun homeOptOutFilters(api: MethodReference? = null) = listOf(
    homeFeedKey,
    methodCall(
        parameters = listOf("Ljava/lang/Object;"), returnType = "Ljava/util/List;",
        opcode = Opcode.INVOKE_STATIC, location = MatchAfterImmediately(),
    ),
    opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
    if (api == null) restoreHomeRequest else {
        methodCall(
            definingClass = api.definingClass, name = api.name,
            parameters = api.parameterTypes.map(CharSequence::toString), returnType = api.returnType,
            opcode = Opcode.INVOKE_INTERFACE, location = MatchAfterImmediately(),
        )
    },
    homeFavoritesCallback,
)

internal object RestoreDynamicHomeFeedFingerprint : Fingerprint(
    name = "onClick",
    isStatic = false,
    parameters = listOf("Landroid/view/View;"),
    returnType = "V",
    filters = listOf(
        fieldAccess(
            definingClass = "Lflipboard/toolbox/usage/UsageEvent\$EventDataType;",
            name = "dynamic_home_feed_unhide",
            opcode = Opcode.SGET_OBJECT,
        ),
    ) + homeOptOutFilters(),
)

internal fun hideDynamicHomeFeedFingerprint(api: MethodReference) = Fingerprint(
    isStatic = true,
    filters = homeOptOutFilters(api),
)

internal object HomeAccountPreferencesFingerprint : Fingerprint(
    isStatic = false,
    parameters = emptyList(),
    strings = listOf("Uid is ", ", udid is "),
    filters = listOf(
        methodCall(parameters = emptyList(), returnType = "Landroid/content/Context;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        fieldAccess(name = "uid-prefs", opcode = Opcode.SGET_OBJECT, location = MatchAfterImmediately()),
        methodCall(
            returnType = "Landroid/content/SharedPreferences;", opcode = Opcode.INVOKE_STATIC,
            location = MatchAfterImmediately(),
        ),
    ),
)

internal val homeIteratorHasNext = methodCall(definingClass = "Ljava/util/Iterator;", name = "hasNext")
internal val castHomeFeedSection = checkCast(FEED_SECTION, location = MatchAfterImmediately())
internal val repeatHomeConversion = opcode(Opcode.GOTO, MatchAfterImmediately())

internal object ConvertHomeFavoritesFingerprint : Fingerprint(
    isStatic = false,
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        methodCall(definingClass = FAVORITES, name = "getValue"),
        homeIteratorHasNext,
        methodCall(definingClass = "Ljava/util/Iterator;", name = "next"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        castHomeFeedSection,
        methodCall(parameters = listOf(FEED_SECTION), opcode = Opcode.INVOKE_STATIC, location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        methodCall(
            definingClass = "Ljava/util/ArrayList;",
            name = "add",
            parameters = listOf("Ljava/lang/Object;"),
            location = MatchAfterImmediately(),
        ),
        repeatHomeConversion,
        methodCall(definingClass = FAVORITES, name = "getOptOut"),
    ),
)
