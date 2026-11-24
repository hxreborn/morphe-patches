/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

internal val readeraSettingsExtensionPatch = extensionPayload("readera-settings", sharedPatchesMenuExtensionPatch)

internal val readeraChapterReaderExtensionPatch = extensionPayload("readera-chapter-reader")

internal val readeraChapterSpeechExtensionPatch =
    extensionPayload("readera-chapter-speech", readeraChapterReaderExtensionPatch, readeraSettingsExtensionPatch)

internal val readeraHighlightColorPickerExtensionPatch =
    extensionPayload("readera-highlight-color-picker", sharedDimensionsExtensionPatch)

internal val readeraLibraryViewModesExtensionPatch = extensionPayload("readera-library-view-modes")

internal val readeraMaterialRadioButtonExtensionPatch = extensionPayload("readera-material-radio-button")

internal val readeraMaterial3ControlsExtensionPatch =
    extensionPayload("readera-material-3-controls", readeraMaterialRadioButtonExtensionPatch)

internal val readeraQuotesAndNotesSectionExtensionPatch =
    extensionPayload("readera-quotes-and-notes-section", sharedDimensionsExtensionPatch, sharedHostedComponentsExtensionPatch)

internal val readeraSelectionMenuExtensionPatch = extensionPayload("readera-selection-menu")

internal val readeraReadAloudFromSelectionExtensionPatch = extensionPayload(
    "readera-read-aloud-from-selection",
    readeraChapterReaderExtensionPatch,
    readeraSelectionMenuExtensionPatch,
)

internal val readeraSearchSelectionInBookExtensionPatch =
    extensionPayload("readera-search-selection-in-book", readeraSelectionMenuExtensionPatch)

internal val readeraCopyWithSourceExtensionPatch =
    extensionPayload("readera-copy-with-source", readeraSelectionMenuExtensionPatch)

internal val readeraReadAloudNotificationControlsExtensionPatch =
    extensionPayload("readera-read-aloud-notification-controls")

internal val readeraSpeechPitchSliderExtensionPatch =
    extensionPayload("readera-speech-pitch-slider", readeraSettingsExtensionPatch)

internal val readeraSpeechRateSliderExtensionPatch = extensionPayload("readera-speech-rate-slider")
