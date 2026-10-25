/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.uptcl;

@SuppressWarnings("unused")
public final class AppVersionCheck {

    private static final String UP_TO_DATE_CODE = "200";

    private static final String[] UPDATE_PROMPT_CODES = { "007", "008" };

    private AppVersionCheck() {

    }

    public static String replaceUpdateCode(String responseCode) {
        for (String updatePromptCode : UPDATE_PROMPT_CODES) {
            if (updatePromptCode.equals(responseCode)) {
                return UP_TO_DATE_CODE;
            }
        }
        return responseCode;
    }

}
