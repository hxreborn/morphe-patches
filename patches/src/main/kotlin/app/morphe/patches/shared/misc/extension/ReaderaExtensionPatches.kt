/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.patch.patch


internal val readeraSettingsExtensionPatch = patch {
    extendWith("extensions/hxreborn/readera-settings.mpe")
}

internal val readeraChapterReaderExtensionPatch = patch {
    extendWith("extensions/hxreborn/readera-chapter-reader.mpe")
}

internal val readeraChapterSpeechExtensionPatch = patch {
    dependsOn(readeraChapterReaderExtensionPatch, readeraSettingsExtensionPatch)
    extendWith("extensions/hxreborn/readera-chapter-speech.mpe")
}

internal val readeraHighlightColorPickerExtensionPatch = patch {
    dependsOn(sharedDimensionsExtensionPatch)
    extendWith("extensions/hxreborn/readera-highlight-color-picker.mpe")
}

internal val readeraLibraryViewModesExtensionPatch = patch {
    extendWith("extensions/hxreborn/readera-library-view-modes.mpe")
}

internal val readeraMaterialRadioButtonExtensionPatch = patch {
    extendWith("extensions/hxreborn/readera-material-radio-button.mpe")
}

internal val readeraMaterial3ControlsExtensionPatch = patch {
    dependsOn(readeraMaterialRadioButtonExtensionPatch)
    extendWith("extensions/hxreborn/readera-material-3-controls.mpe")
}

internal val readeraQuotesAndNotesSectionExtensionPatch = patch {
    dependsOn(sharedDimensionsExtensionPatch)
    extendWith("extensions/hxreborn/readera-quotes-and-notes-section.mpe")
}

internal val readeraSelectionMenuExtensionPatch = patch {
    extendWith("extensions/hxreborn/readera-selection-menu.mpe")
}

internal val readeraReadAloudFromSelectionExtensionPatch = patch {
    dependsOn(readeraChapterReaderExtensionPatch, readeraSelectionMenuExtensionPatch)
    extendWith("extensions/hxreborn/readera-read-aloud-from-selection.mpe")
}

internal val readeraSearchSelectionInBookExtensionPatch = patch {
    dependsOn(readeraSelectionMenuExtensionPatch)
    extendWith("extensions/hxreborn/readera-search-selection-in-book.mpe")
}

internal val readeraCopyWithSourceExtensionPatch = patch {
    dependsOn(readeraSelectionMenuExtensionPatch)
    extendWith("extensions/hxreborn/readera-copy-with-source.mpe")
}

internal val readeraReadAloudNotificationControlsExtensionPatch = patch {
    extendWith("extensions/hxreborn/readera-read-aloud-notification-controls.mpe")
}

internal val readeraSpeechPitchSliderExtensionPatch = patch {
    dependsOn(readeraSettingsExtensionPatch)
    extendWith("extensions/hxreborn/readera-speech-pitch-slider.mpe")
}

internal val readeraSpeechRateSliderExtensionPatch = patch {
    extendWith("extensions/hxreborn/readera-speech-rate-slider.mpe")
}
