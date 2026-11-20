/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.bytecodePatch

internal val readeraSettingsExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/readera-settings.mpe")
}

internal val readeraChapterReaderExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/readera-chapter-reader.mpe")
}

internal val readeraChapterSpeechExtensionPatch = bytecodePatch {
    dependsOn(readeraChapterReaderExtensionPatch, readeraSettingsExtensionPatch)
    extendWith("extensions/hxreborn/readera-chapter-speech.mpe")
}

internal val readeraHighlightColorPickerExtensionPatch = bytecodePatch {
    dependsOn(sharedDimensionsExtensionPatch)
    extendWith("extensions/hxreborn/readera-highlight-color-picker.mpe")
}

internal val readeraLibraryViewModesExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/readera-library-view-modes.mpe")
}

internal val readeraMaterialRadioButtonExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/readera-material-radio-button.mpe")
}

internal val readeraMaterial3ControlsExtensionPatch = bytecodePatch {
    dependsOn(readeraMaterialRadioButtonExtensionPatch)
    extendWith("extensions/hxreborn/readera-material-3-controls.mpe")
}

internal val readeraQuotesAndNotesSectionExtensionPatch = bytecodePatch {
    dependsOn(sharedDimensionsExtensionPatch)
    extendWith("extensions/hxreborn/readera-quotes-and-notes-section.mpe")
}

internal val readeraSelectionMenuExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/readera-selection-menu.mpe")
}

internal val readeraReadAloudFromSelectionExtensionPatch = bytecodePatch {
    dependsOn(readeraChapterReaderExtensionPatch, readeraSelectionMenuExtensionPatch)
    extendWith("extensions/hxreborn/readera-read-aloud-from-selection.mpe")
}

internal val readeraSearchSelectionInBookExtensionPatch = bytecodePatch {
    dependsOn(readeraSelectionMenuExtensionPatch)
    extendWith("extensions/hxreborn/readera-search-selection-in-book.mpe")
}

internal val readeraCopyWithSourceExtensionPatch = bytecodePatch {
    dependsOn(readeraSelectionMenuExtensionPatch)
    extendWith("extensions/hxreborn/readera-copy-with-source.mpe")
}

internal val readeraReadAloudNotificationControlsExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/readera-read-aloud-notification-controls.mpe")
}

internal val readeraSpeechPitchSliderExtensionPatch = bytecodePatch {
    dependsOn(readeraSettingsExtensionPatch)
    extendWith("extensions/hxreborn/readera-speech-pitch-slider.mpe")
}

internal val readeraSpeechRateSliderExtensionPatch = bytecodePatch {
    extendWith("extensions/hxreborn/readera-speech-rate-slider.mpe")
}
