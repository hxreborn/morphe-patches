/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Additional terms under GPLv3 section 7:
 * - You must preserve reasonable legal notices and author attributions in this file.
 * - Modified versions must not misrepresent the origin of this file.
 */
package app.morphe.patches.forus.misc.premium

import app.morphe.patcher.Fingerprint

// Per-module access flag, deserialized from the server's module options. Every dashboard
// module card ("Exclusivo para socios Plus") gates on this getter.
internal object AppModuleHasAccessFingerprint : Fingerprint(
    definingClass = "Lcom/vitale/coredata/data/models/AppModule;",
    name = "getHasAccess",
)
