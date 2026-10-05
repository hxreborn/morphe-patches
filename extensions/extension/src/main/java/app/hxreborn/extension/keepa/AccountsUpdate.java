/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.keepa;

import org.json.JSONException;

interface AccountsUpdate {

    void apply(Accounts accounts) throws JSONException;

}
