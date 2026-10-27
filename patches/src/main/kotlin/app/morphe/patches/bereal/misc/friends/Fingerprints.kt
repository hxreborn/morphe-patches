/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.friends

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.resource.ResourceType

private const val LAZY_LIST_SCOPE = "Landroidx/compose/foundation/lazy/LazyListScope;"
private const val MODIFIER = "Landroidx/compose/ui/Modifier;"
private const val COMPOSER = "Landroidx/compose/runtime/Composer;"

internal object FriendRecommendationToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("FriendRecommendationDomainModel(user="),
)

internal fun friendRecommendationMapperFingerprint(recommendationType: String) = Fingerprint(
    isStatic = true,
    returnType = recommendationType,
    parameters = listOf("L"),
)

internal object RelationshipSectionsToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("Sections(users=", ", friendSuggestions="),
)

internal fun relationshipSectionsConstructorFingerprint(sectionsType: String) = Fingerprint(
    definingClass = sectionsType,
    name = "<init>",
)

internal object FeedCarouselToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("FriendRecommendationCarouselViewState(sectionLabelUiState="),
)

internal fun feedCarouselConstructionFingerprint(carouselType: String) = Fingerprint(
    filters = listOf(methodCall(definingClass = carouselType, name = "<init>")),
)

internal object FeedWidgetToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("FriendRecommendationWidget(shouldDisplay="),
)

internal fun feedWidgetConstructorFingerprint(widgetType: String) = Fingerprint(
    definingClass = widgetType,
    name = "<init>",
    parameters = listOf("Z"),
)

internal object ContactsPlaceholderToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("EmptyFriendPlaceholder(title="),
)

internal fun contactsPlaceholderFingerprint(placeholderType: String) = Fingerprint(
    isStatic = true,
    returnType = "V",
    parameters = listOf(placeholderType, MODIFIER, COMPOSER, "I"),
)

internal object SuggestionSectionFingerprint : Fingerprint(
    isStatic = true,
    returnType = "V",
    parameters = listOf(LAZY_LIST_SCOPE, "L", "Z", "Ljava/lang/String;", "L", "L", "L", "L", "L", "Ljava/lang/Integer;", "Z"),
)

internal object ContactsOnBeRealSectionFingerprint : Fingerprint(
    isStatic = true,
    returnType = "V",
    parameters = listOf(LAZY_LIST_SCOPE, "L", "Ljava/lang/String;", "Ljava/util/List;", "L", "L", "L"),
    filters = listOf(resourceLiteral(ResourceType.STRING, "setup_contactsScan_headerContactsUsingBeRealList")),
)

internal object PopularAccountsToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("Visible(items=", ", hideSectionItem="),
)

internal fun popularAccountsConstructionFingerprint(visibleType: String) = Fingerprint(
    filters = listOf(methodCall(definingClass = visibleType, name = "<init>")),
)
