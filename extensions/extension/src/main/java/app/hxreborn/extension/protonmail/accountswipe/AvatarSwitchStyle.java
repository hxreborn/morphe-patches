/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.protonmail.accountswipe;

public enum AvatarSwitchStyle {

    SLIDE("Slide"), CAROUSEL("Carousel"), FADE("Fade"), ZOOM("Zoom"), FLIP("Flip"), CUBE("Cube"), SPIN("Spin"),
    NONE("None");

    private final String label;

    AvatarSwitchStyle(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }

    static AvatarSwitchStyle fromName(String name) {
        for (AvatarSwitchStyle style : values()) {
            if (style.name().equals(name)) {
                return style;
            }
        }
        return SLIDE;
    }

}
