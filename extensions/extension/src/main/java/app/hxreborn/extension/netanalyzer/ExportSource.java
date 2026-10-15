/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import android.content.Context;

interface ExportSource {

    String subject(Context context);

    String text(Context context);

}
